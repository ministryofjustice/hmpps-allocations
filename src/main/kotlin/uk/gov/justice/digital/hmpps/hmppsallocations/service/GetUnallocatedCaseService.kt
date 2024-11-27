package uk.gov.justice.digital.hmpps.hmppsallocations.service

import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.reactor.awaitSingle
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Service
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import uk.gov.justice.digital.hmpps.hmppsallocations.client.AssessRisksNeedsApiClient
import uk.gov.justice.digital.hmpps.hmppsallocations.client.DeliusCaseDetails
import uk.gov.justice.digital.hmpps.hmppsallocations.client.WorkforceAllocationsToDeliusApiClient
import uk.gov.justice.digital.hmpps.hmppsallocations.client.dto.CrnStaffRestrictions
import uk.gov.justice.digital.hmpps.hmppsallocations.client.dto.DeliusCaseView
import uk.gov.justice.digital.hmpps.hmppsallocations.domain.Assessment
import uk.gov.justice.digital.hmpps.hmppsallocations.domain.CaseCountByTeam
import uk.gov.justice.digital.hmpps.hmppsallocations.domain.CaseOverview
import uk.gov.justice.digital.hmpps.hmppsallocations.domain.UnallocatedCase
import uk.gov.justice.digital.hmpps.hmppsallocations.domain.UnallocatedCaseConfirmInstructions
import uk.gov.justice.digital.hmpps.hmppsallocations.domain.UnallocatedCaseConvictions
import uk.gov.justice.digital.hmpps.hmppsallocations.domain.UnallocatedCaseDetails
import uk.gov.justice.digital.hmpps.hmppsallocations.domain.UnallocatedCaseRisks
import uk.gov.justice.digital.hmpps.hmppsallocations.jpa.entity.UnallocatedCaseEntity
import uk.gov.justice.digital.hmpps.hmppsallocations.jpa.repository.UnallocatedCasesRepository
import uk.gov.justice.digital.hmpps.hmppsallocations.service.exception.NotAllowedForLAOException
import java.time.LocalDateTime

@Suppress("TooManyFunctions")
@Service
class GetUnallocatedCaseService(
  private val unallocatedCasesRepository: UnallocatedCasesRepository,
  private val outOfAreaTransferService: OutOfAreaTransferService,
  @Qualifier("assessRisksNeedsApiClientUserEnhanced")
  private val assessRisksNeedsApiClient: AssessRisksNeedsApiClient,
  @Qualifier("workforceAllocationsToDeliusApiClientUserEnhanced")
  private val workforceAllocationsToDeliusApiClient: WorkforceAllocationsToDeliusApiClient,
  @Qualifier("laoService")
  private val laoService: LaoService,
) {

  suspend fun getCase(crn: String, convictionNumber: Long): UnallocatedCaseDetails? {
    log.info("lao check")
    if (laoService.getCrnRestrictions(crn).apopUserExcluded) {
      throw NotAllowedForLAOException("A user of APoP is excluded from viewing this case", crn)
    }
    return findUnallocatedCaseByConvictionNumber(crn, convictionNumber)?.let { unallocatedCaseEntity ->
      log.info("gettingLatestAssessment")
      val assessment = assessRisksNeedsApiClient.getLatestCompleteAssessment(crn)
      log.info("gettingCaseView")
      val getDeliusCaseViewApiCall = workforceAllocationsToDeliusApiClient.getDeliusCaseView(crn, convictionNumber)
      log.info("gettingCaseView api call")
      val getDeliusCaseDetailsApiCall = workforceAllocationsToDeliusApiClient
        .getDeliusCaseDetails(
          crn,
          convictionNumber,
        )
      log.info("apis in parallell call")
      callDeliusCaseViewAndCaseDetailApisInParallel(
        crn,
        convictionNumber,
        unallocatedCaseEntity,
        assessment,
        getDeliusCaseViewApiCall,
        getDeliusCaseDetailsApiCall,
      )
    }
    log.info("out of parallee")
  }

  @Suppress("LongParameterList")
  private suspend fun callDeliusCaseViewAndCaseDetailApisInParallel(
    crn: String,
    convictionNumber: Long,
    unallocatedCaseEntity: UnallocatedCaseEntity,
    assessment: Assessment?,
    getDeliusCaseViewCall: Mono<DeliusCaseView>,
    getDeliusCaseDetailsCall: Mono<DeliusCaseDetails>,
  ): UnallocatedCaseDetails? =
    Mono.zip(getDeliusCaseViewCall, getDeliusCaseDetailsCall)
      .awaitSingle()
      .let {
        log.info("in one")
        val caseView = it.t1
        val caseDetails = it.t2
        val caseIsOutOfAreaTransfer = if (caseDetails.cases.firstOrNull() != null) {
          outOfAreaTransferService.isCaseCurrentlyManagedOutsideOfCurrentTeamsRegion(
            currentTeamCode = unallocatedCaseEntity.teamCode,
            unallocatedCasesFromDelius = caseDetails.cases.first(),
          )
        } else {
          false
        }
        getRisksAndGenerateUnallocatedCaseDetails(
          crn,
          convictionNumber,
          unallocatedCaseEntity,
          caseView,
          assessment,
          caseIsOutOfAreaTransfer,
        )
      }

  @Suppress("LongParameterList")
  private suspend fun getRisksAndGenerateUnallocatedCaseDetails(
    crn: String,
    convictionNumber: Long,
    unallocatedCaseEntity: UnallocatedCaseEntity,
    caseView: DeliusCaseView,
    assessment: Assessment?,
    outOfAreaTransfer: Boolean,
  ): UnallocatedCaseDetails? {
    log.info("gettingCaseRisks")
    val unallocatedCaseRisks = getCaseRisks(crn, convictionNumber)
    log.info("Got case risk")
    return UnallocatedCaseDetails.from(
      unallocatedCaseEntity,
      caseView,
      assessment,
      unallocatedCaseRisks,
      outOfAreaTransfer,
    ).takeUnless { restrictedOrExcluded(crn) }
  }

  suspend fun getCaseOverview(crn: String, convictionNumber: Long): CaseOverview? {
    return findUnallocatedCaseByConvictionNumber(crn, convictionNumber)?.let {
      CaseOverview.from(it)
    }.takeUnless { restrictedOrExcluded(crn) }
  }

  @SuppressWarnings("LongMethod")
  suspend fun getAllByTeam(teamCode: String): List<UnallocatedCase> {
    log.info("Getting all unallocated cases for team $teamCode")
    log.info("Security Context ${SecurityContextHolder.getContext().authentication}")
    val unallocatedCasesFromRepo = unallocatedCasesRepository.findByTeamCode(teamCode)
    if (unallocatedCasesFromRepo.isEmpty()) {
      return emptyList()
    } else {
      val unallocatedCasesUserAccess = workforceAllocationsToDeliusApiClient.getUserAccess(
        crns = unallocatedCasesFromRepo.map { it.crn },
      ).access

      val unallocatedCases = unallocatedCasesFromRepo.filter { uc ->
        val caseAccess = unallocatedCasesUserAccess.firstOrNull { uc.crn == it.crn }
        caseAccess?.userRestricted == false
      }

      if (unallocatedCases.isEmpty()) {
        return emptyList()
      } else {
        val unallocatedCasesFromDelius = workforceAllocationsToDeliusApiClient
          .getDeliusCaseDetailsCases(cases = unallocatedCases)
          .filter { deliusUnallocatedCase ->
            unallocatedCases
              .any { deliusUnallocatedCase.crn == it.crn && deliusUnallocatedCase.event.number.toInt() == it.convictionNumber }
          }
          .toList()

        if (unallocatedCasesFromDelius.isEmpty()) {
          return emptyList()
        } else {
          val crnsThatAreCurrentlyManagedOutsideOfThisTeamsRegion = outOfAreaTransferService
            .getCasesThatAreCurrentlyManagedOutsideOfCurrentTeamsRegion(
              teamCode,
              unallocatedCasesFromDelius,
            ).map { it.crn }

          return unallocatedCasesFromDelius
            .map { deliusCaseDetail ->
              val unallocatedCase =
                unallocatedCases.first { it.crn == deliusCaseDetail.crn && it.convictionNumber == deliusCaseDetail.event.number.toInt() }
              val excluded = unallocatedCasesUserAccess.any { it.crn == unallocatedCase.crn && it.userExcluded }
              UnallocatedCase.from(
                unallocatedCase,
                deliusCaseDetail,
                outOfAreaTransfer = crnsThatAreCurrentlyManagedOutsideOfThisTeamsRegion.contains(unallocatedCase.crn),
                excluded,
                excluded && laoService.getCrnRestrictions(unallocatedCase.crn).apopUserExcluded,
              )
            }
        }
      }
    }
  }

  suspend fun getCaseConvictions(crn: String, excludeConvictionNumber: Long): UnallocatedCaseConvictions? {
    return findUnallocatedCaseByConvictionNumber(crn, excludeConvictionNumber)?.let {
      val probationRecord = workforceAllocationsToDeliusApiClient.getProbationRecord(crn, excludeConvictionNumber)
      return UnallocatedCaseConvictions.from(it, probationRecord).takeUnless { restrictedOrExcluded(crn) }
    }
  }

  suspend fun getCaseRisks(crn: String, convictionNumber: Long): UnallocatedCaseRisks? {
    val harry = findUnallocatedCaseByConvictionNumber(crn, convictionNumber)?.let { unallocatedCaseEntity ->
      log.info("getting risk $unallocatedCaseEntity")
      val fred = workforceAllocationsToDeliusApiClient.getDeliusRisk(crn)
      log.info("got delius risk $fred")
      val george = assessRisksNeedsApiClient.getRiskPredictors(crn)
      log.info("got risk predictors")
      val charlie = assessRisksNeedsApiClient.getRosh(crn)
      log.info("got rosh ")
      return UnallocatedCaseRisks.from(
        fred,
        unallocatedCaseEntity,
        charlie,
        george
          .filter { it.rsrScoreLevel != null && it.rsrPercentageScore != null }
          .toList().maxByOrNull { it.completedDate ?: LocalDateTime.MIN },
      ).takeUnless { restrictedOrExcluded(crn) }
    }
    log.info("got findUnallocatedCaseByConvictionNumber")
    return harry
  }

  fun getCaseCountByTeam(teamCodes: List<String>): Flux<CaseCountByTeam> =
    Flux.fromIterable(unallocatedCasesRepository.getCaseCountByTeam(teamCodes))
      .map { CaseCountByTeam(it.getTeamCode(), it.getCaseCount()) }

  private fun findUnallocatedCaseByConvictionNumber(
    crn: String,
    convictionNumber: Long,
  ) = unallocatedCasesRepository.findCaseByCrnAndConvictionNumber(crn, convictionNumber.toInt())

  suspend fun getCaseConfirmInstructions(crn: String, convictionNumber: Long, staffCode: String): UnallocatedCaseConfirmInstructions? {
    return findUnallocatedCaseByConvictionNumber(crn, convictionNumber)?.let { unallocatedCaseEntity ->
      val personOnProbationStaffDetailsResponse = workforceAllocationsToDeliusApiClient.personOnProbationStaffDetails(crn, staffCode)
      return UnallocatedCaseConfirmInstructions.from(
        unallocatedCaseEntity,
        personOnProbationStaffDetailsResponse,
      ).takeUnless { restrictedOrExcluded(crn) }
    }
  }

  suspend fun restrictedOrExcluded(crn: String): Boolean {
    log.info("check restricted or excluded")
    return workforceAllocationsToDeliusApiClient.getUserAccess(crn)?.run { userRestricted } ?: true
  }

  suspend fun getCrnStaffRestrictions(crn: String, staffCodes: List<String>): CrnStaffRestrictions? {
    return laoService.getCrnRestrictionsForUsers(crn, staffCodes)
  }

  suspend fun isCrnRestricted(crn: String): Boolean? {
    return laoService.isCrnRestricted(crn)
  }

  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }
}
