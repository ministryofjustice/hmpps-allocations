package uk.gov.justice.digital.hmpps.hmppsallocations.service

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Service
import reactor.core.publisher.Flux
import uk.gov.justice.digital.hmpps.hmppsallocations.client.AssessRisksNeedsApiClient
import uk.gov.justice.digital.hmpps.hmppsallocations.client.WorkforceAllocationsToDeliusApiClient
import uk.gov.justice.digital.hmpps.hmppsallocations.domain.CaseCountByTeam
import uk.gov.justice.digital.hmpps.hmppsallocations.domain.CaseOverview
import uk.gov.justice.digital.hmpps.hmppsallocations.domain.UnallocatedCase
import uk.gov.justice.digital.hmpps.hmppsallocations.domain.UnallocatedCaseConfirmInstructions
import uk.gov.justice.digital.hmpps.hmppsallocations.domain.UnallocatedCaseConvictions
import uk.gov.justice.digital.hmpps.hmppsallocations.domain.UnallocatedCaseDecisionEvidencing
import uk.gov.justice.digital.hmpps.hmppsallocations.domain.UnallocatedCaseDetails
import uk.gov.justice.digital.hmpps.hmppsallocations.domain.UnallocatedCaseRisks
import uk.gov.justice.digital.hmpps.hmppsallocations.jpa.repository.UnallocatedCasesRepository
import java.time.LocalDateTime

@Service
class GetUnallocatedCaseService(
  private val unallocatedCasesRepository: UnallocatedCasesRepository,
  private val outOfAreaTransferService: OutOfAreaTransferService,
  @Qualifier("assessRisksNeedsApiClientUserEnhanced") private val assessRisksNeedsApiClient: AssessRisksNeedsApiClient,
  @Qualifier("workforceAllocationsToDeliusApiClientUserEnhanced") private val workforceAllocationsToDeliusApiClient: WorkforceAllocationsToDeliusApiClient,
) {

  suspend fun getCase(crn: String, convictionNumber: Long): UnallocatedCaseDetails? {
    return findUnallocatedCaseByConvictionNumber(crn, convictionNumber)?.let {
      val assessment = assessRisksNeedsApiClient.getLatestCompleteAssessment(crn)
      val deliusCaseView = workforceAllocationsToDeliusApiClient.getDeliusCaseView(crn, convictionNumber)
      val unallocatedCaseRisks = getCaseRisks(crn, convictionNumber)
      return UnallocatedCaseDetails.from(it, deliusCaseView, assessment, unallocatedCaseRisks).takeUnless { restrictedOrExcluded(crn) }
    }
  }

  suspend fun getCaseOverview(crn: String, convictionNumber: Long): CaseOverview? {
    return findUnallocatedCaseByConvictionNumber(crn, convictionNumber)?.let {
      CaseOverview.from(it)
    }.takeUnless { restrictedOrExcluded(crn) }
  }

  suspend fun getAllByTeam(teamCode: String): Flow<UnallocatedCase> {
    val unallocatedCases = unallocatedCasesRepository.findByTeamCode(teamCode).filter { workforceAllocationsToDeliusApiClient.getUserAccess(it.crn).run { this?.userExcluded == false && !this.userRestricted } }

    val unallocatedCasesFromDelius = workforceAllocationsToDeliusApiClient.getDeliusCaseDetails(unallocatedCases)
      .filter { unallocatedCasesRepository.existsByCrnAndConvictionNumber(it.crn, it.event.number.toInt()) }

    val crnsThatAreCurrentlyManagedOutsideOfThisTeamsRegion = outOfAreaTransferService
      .getCasesThatAreCurrentlyManagedOutsideOfThisTeamsRegion(
        teamCode,
        unallocatedCasesFromDelius,
      ).map { it.first }.toList()

    return unallocatedCasesFromDelius
      .map { deliusCaseDetail ->
        val unallocatedCase = unallocatedCases.first { it.crn == deliusCaseDetail.crn && it.convictionNumber == deliusCaseDetail.event.number.toInt() }
        UnallocatedCase.from(
          unallocatedCase,
          deliusCaseDetail,
          outOfAreaTransfer = crnsThatAreCurrentlyManagedOutsideOfThisTeamsRegion.contains(unallocatedCase.crn),
        )
      }
  }

  suspend fun getCaseConvictions(crn: String, excludeConvictionNumber: Long): UnallocatedCaseConvictions? {
    return findUnallocatedCaseByConvictionNumber(crn, excludeConvictionNumber)?.let {
      val probationRecord = workforceAllocationsToDeliusApiClient.getProbationRecord(crn, excludeConvictionNumber)
      return UnallocatedCaseConvictions.from(it, probationRecord).takeUnless { restrictedOrExcluded(crn) }
    }
  }

  suspend fun getCaseRisks(crn: String, convictionNumber: Long): UnallocatedCaseRisks? {
    return findUnallocatedCaseByConvictionNumber(crn, convictionNumber)?.let { unallocatedCaseEntity ->
      return UnallocatedCaseRisks.from(
        workforceAllocationsToDeliusApiClient.getDeliusRisk(crn),
        unallocatedCaseEntity,
        assessRisksNeedsApiClient.getRosh(crn),
        assessRisksNeedsApiClient.getRiskPredictors(crn)
          .filter { it.rsrScoreLevel != null && it.rsrPercentageScore != null }
          .toList().maxByOrNull { it.completedDate ?: LocalDateTime.MIN },
      ).takeUnless { restrictedOrExcluded(crn) }
    }
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
  suspend fun getCaseDecisionEvidencing(crn: String, convictionNumber: Long, staffCode: String): UnallocatedCaseDecisionEvidencing? {
    return findUnallocatedCaseByConvictionNumber(crn, convictionNumber)?.let { unallocatedCaseEntity ->
      val personOnProbationStaffDetailsResponse = workforceAllocationsToDeliusApiClient.personOnProbationStaffDetails(crn, staffCode)
      return UnallocatedCaseDecisionEvidencing.from(
        unallocatedCaseEntity,
        personOnProbationStaffDetailsResponse,
      ).takeUnless { restrictedOrExcluded(crn) }
    }
  }

  suspend fun restrictedOrExcluded(crn: String): Boolean {
    return workforceAllocationsToDeliusApiClient.getUserAccess(crn)?.run { userExcluded || userRestricted } ?: true
  }
}
