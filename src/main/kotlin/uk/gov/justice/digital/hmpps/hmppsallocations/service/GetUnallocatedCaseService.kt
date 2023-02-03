package uk.gov.justice.digital.hmpps.hmppsallocations.service

import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Service
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import uk.gov.justice.digital.hmpps.hmppsallocations.client.AssessRisksNeedsApiClient
import uk.gov.justice.digital.hmpps.hmppsallocations.client.AssessmentApiClient
import uk.gov.justice.digital.hmpps.hmppsallocations.client.CommunityApiClient
import uk.gov.justice.digital.hmpps.hmppsallocations.client.WorkforceAllocationsToDeliusApiClient
import uk.gov.justice.digital.hmpps.hmppsallocations.domain.CaseCountByTeam
import uk.gov.justice.digital.hmpps.hmppsallocations.domain.CaseOverview
import uk.gov.justice.digital.hmpps.hmppsallocations.domain.RoshSummary
import uk.gov.justice.digital.hmpps.hmppsallocations.domain.UnallocatedCase
import uk.gov.justice.digital.hmpps.hmppsallocations.domain.UnallocatedCaseConvictions
import uk.gov.justice.digital.hmpps.hmppsallocations.domain.UnallocatedCaseDetails
import uk.gov.justice.digital.hmpps.hmppsallocations.domain.UnallocatedCaseOgrs
import uk.gov.justice.digital.hmpps.hmppsallocations.domain.UnallocatedCaseRegistration
import uk.gov.justice.digital.hmpps.hmppsallocations.domain.UnallocatedCaseRisks
import uk.gov.justice.digital.hmpps.hmppsallocations.domain.UnallocatedCaseRsr
import uk.gov.justice.digital.hmpps.hmppsallocations.jpa.repository.UnallocatedCasesRepository
import java.util.Optional

@Service
class GetUnallocatedCaseService(
  private val unallocatedCasesRepository: UnallocatedCasesRepository,
  @Qualifier("communityApiClientUserEnhanced") private val communityApiClient: CommunityApiClient,
  @Qualifier("assessmentApiClientUserEnhanced") private val assessmentApiClient: AssessmentApiClient,
  @Qualifier("assessRisksNeedsApiClientUserEnhanced") private val assessRisksNeedsApiClient: AssessRisksNeedsApiClient,
  @Qualifier("workforceAllocationsToDeliusApiClientUserEnhanced") private val workforceAllocationsToDeliusApiClient: WorkforceAllocationsToDeliusApiClient
) {

  fun getCase(crn: String, convictionNumber: Long): UnallocatedCaseDetails? =
    findUnallocatedCaseByConvictionNumber(crn, convictionNumber)?.let {

      val assessment = assessmentApiClient.getAssessment(crn)
        .map { assessments -> Optional.ofNullable(assessments.maxByOrNull { a -> a.completed }) }
      val results = Mono.zip(workforceAllocationsToDeliusApiClient.getDeliusCaseView(crn, convictionNumber), assessment).block()!!
      return UnallocatedCaseDetails.from(it, results.t1, results.t2.orElse(null))
    }

  fun getCaseOverview(crn: String, convictionNumber: Long): CaseOverview? =
    findUnallocatedCaseByConvictionNumber(crn, convictionNumber)?.let {
      CaseOverview.from(it)
    }

  fun getAllByTeam(teamCode: String): Flux<UnallocatedCase> {
    return workforceAllocationsToDeliusApiClient.getDeliusCaseDetails(unallocatedCasesRepository.findByTeamCode(teamCode))
      .filter { unallocatedCasesRepository.existsByCrnAndConvictionNumber(it.crn, it.event.number.toInt()) }
      .map { deliusCaseDetail ->
        val unallocatedCase = unallocatedCasesRepository.findCaseByCrnAndConvictionNumber(
          deliusCaseDetail.crn,
          deliusCaseDetail.event.number.toInt()
        )!!
        unallocatedCase.initialAppointment = deliusCaseDetail.initialAppointment?.date
        unallocatedCasesRepository.save(unallocatedCase)
        UnallocatedCase.from(
          unallocatedCase, deliusCaseDetail
        )
      }
  }

  fun getCaseConvictions(crn: String, excludeConvictionNumber: Long): UnallocatedCaseConvictions? =
    findUnallocatedCaseByConvictionNumber(crn, excludeConvictionNumber)?.let {
      val probationRecord = workforceAllocationsToDeliusApiClient.getProbationRecord(crn, excludeConvictionNumber).block()
      return UnallocatedCaseConvictions.from(it, probationRecord)
    }
  fun getCaseRisk(crn: String, convictionNumber: Long): UnallocatedCaseRisks? {
    val deliusRisk = workforceAllocationsToDeliusApiClient.getDeliusRisk(crn).block()
    val unallocatedCaseEntity = findUnallocatedCaseByConvictionNumber(crn, convictionNumber)!!
    return UnallocatedCaseRisks(
      deliusRisk.name.forename + " " + deliusRisk.name.middleName + " " + deliusRisk.name.surname,
      crn,
      unallocatedCaseEntity.tier,
      deliusRisk.activeRegistrations.map { UnallocatedCaseRegistration(it.description, it.startDate, it.notes, null) },
      deliusRisk.inactiveRegistrations.map { UnallocatedCaseRegistration(it.description, it.startDate, it.notes, it.endDate) },
      RoshSummary(deliusRisk.rosh.overallRisk, deliusRisk.rosh.assessmentDate, deliusRisk.rosh.riskInCommunity),
      UnallocatedCaseRsr(deliusRisk.rsr.levelScore, deliusRisk.rsr.completedDate, deliusRisk.rsr.percentageScore),
      UnallocatedCaseOgrs(deliusRisk.ogrs?.lastUpdatedDate, deliusRisk.ogrs?.score).takeUnless { deliusRisk.ogrs == null },
      unallocatedCaseEntity.convictionNumber
    )
  }

  fun getCaseCountByTeam(teamCodes: List<String>): Flux<CaseCountByTeam> =
    Flux.fromIterable(unallocatedCasesRepository.getCaseCountByTeam(teamCodes))
      .map { CaseCountByTeam(it.getTeamCode(), it.getCaseCount()) }

  private fun findUnallocatedCaseByConvictionNumber(
    crn: String,
    convictionNumber: Long
  ) = unallocatedCasesRepository.findCaseByCrnAndConvictionNumber(crn, convictionNumber.toInt())
}
