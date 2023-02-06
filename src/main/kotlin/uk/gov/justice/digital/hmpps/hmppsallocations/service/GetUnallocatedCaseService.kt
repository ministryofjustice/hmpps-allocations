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

  fun getCaseRisks(crn: String, convictionNumber: Long): UnallocatedCaseRisks? =
    findUnallocatedCaseByConvictionNumber(crn, convictionNumber)?.let {
      val registrations = communityApiClient.getAllRegistrations(crn)
        .map { registrations ->
          registrations.registrations?.groupBy { registration -> registration.active } ?: emptyMap()
        }

      val rosh = assessRisksNeedsApiClient.getRosh(crn)

      val rsr = assessRisksNeedsApiClient.getRiskPredictors(crn)
        .map { riskPredictors ->
          Optional.ofNullable(
            riskPredictors
              .filter { riskPredictor -> riskPredictor.rsrScoreLevel != null && riskPredictor.rsrPercentageScore != null }
              .maxByOrNull { riskPredictor -> riskPredictor.completedDate ?: LocalDateTime.MIN }
          )
        }

      val ogrs = communityApiClient.getAssessment(crn)

      val results = Mono.zip(registrations, rosh, rsr, ogrs).block()!!
      return UnallocatedCaseRisks.from(
        it,
        results.t1.getOrDefault(true, emptyList()),
        results.t1.getOrDefault(false, emptyList()),
        results.t2.orElse(null),
        results.t3.orElse(null),
        results.t4.orElse(null)
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
