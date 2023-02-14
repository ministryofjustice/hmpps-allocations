package uk.gov.justice.digital.hmpps.hmppsallocations.service

import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Service
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import uk.gov.justice.digital.hmpps.hmppsallocations.client.AssessRisksNeedsApiClient
import uk.gov.justice.digital.hmpps.hmppsallocations.client.AssessmentApiClient
import uk.gov.justice.digital.hmpps.hmppsallocations.client.WorkforceAllocationsToDeliusApiClient
import uk.gov.justice.digital.hmpps.hmppsallocations.domain.CaseCountByTeam
import uk.gov.justice.digital.hmpps.hmppsallocations.domain.CaseOverview
import uk.gov.justice.digital.hmpps.hmppsallocations.domain.UnallocatedCase
import uk.gov.justice.digital.hmpps.hmppsallocations.domain.UnallocatedCaseConvictions
import uk.gov.justice.digital.hmpps.hmppsallocations.domain.UnallocatedCaseDetails
import uk.gov.justice.digital.hmpps.hmppsallocations.domain.UnallocatedCaseRisks
import uk.gov.justice.digital.hmpps.hmppsallocations.jpa.repository.UnallocatedCasesRepository
import java.time.LocalDateTime
import java.util.Optional

@Service
class GetUnallocatedCaseService(
  private val unallocatedCasesRepository: UnallocatedCasesRepository,
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
    val unallocatedCases = unallocatedCasesRepository.findByTeamCode(teamCode)
    return workforceAllocationsToDeliusApiClient.getDeliusCaseDetails(unallocatedCases)
      .filter { unallocatedCasesRepository.existsByCrnAndConvictionNumber(it.crn, it.event.number.toInt()) }
      .map { deliusCaseDetail ->
        val unallocatedCase = unallocatedCases.first { it.crn == deliusCaseDetail.crn && it.convictionNumber == deliusCaseDetail.event.number.toInt() }
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

  fun getCaseRisks(crn: String, convictionNumber: Long): UnallocatedCaseRisks? {
    return UnallocatedCaseRisks.from(
      workforceAllocationsToDeliusApiClient.getDeliusRisk(crn).block(),
      findUnallocatedCaseByConvictionNumber(crn, convictionNumber)!!,
      assessRisksNeedsApiClient.getRosh(crn).block(),
      assessRisksNeedsApiClient.getRiskPredictors(crn)
        .filter { it.rsrScoreLevel != null && it.rsrPercentageScore != null }
        .collectList().block()?.maxByOrNull { it.completedDate ?: LocalDateTime.MIN }
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
