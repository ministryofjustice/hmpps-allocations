package uk.gov.justice.digital.hmpps.hmppsallocations.service

import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Service
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import uk.gov.justice.digital.hmpps.hmppsallocations.client.AssessRisksNeedsApiClient
import uk.gov.justice.digital.hmpps.hmppsallocations.client.AssessmentApiClient
import uk.gov.justice.digital.hmpps.hmppsallocations.client.CommunityApiClient
import uk.gov.justice.digital.hmpps.hmppsallocations.client.WorkforceAllocationsToDeliusApiClient
import uk.gov.justice.digital.hmpps.hmppsallocations.controller.ChooseOffenderManagerCase
import uk.gov.justice.digital.hmpps.hmppsallocations.controller.ChooseOffenderManagerCase.Companion.from
import uk.gov.justice.digital.hmpps.hmppsallocations.domain.CaseCountByTeam
import uk.gov.justice.digital.hmpps.hmppsallocations.domain.Conviction
import uk.gov.justice.digital.hmpps.hmppsallocations.domain.Documents
import uk.gov.justice.digital.hmpps.hmppsallocations.domain.UnallocatedCase
import uk.gov.justice.digital.hmpps.hmppsallocations.domain.UnallocatedCaseConviction
import uk.gov.justice.digital.hmpps.hmppsallocations.domain.UnallocatedCaseConvictionPractitioner
import uk.gov.justice.digital.hmpps.hmppsallocations.domain.UnallocatedCaseConvictions
import uk.gov.justice.digital.hmpps.hmppsallocations.domain.UnallocatedCaseDetails
import uk.gov.justice.digital.hmpps.hmppsallocations.domain.UnallocatedCaseDocument
import uk.gov.justice.digital.hmpps.hmppsallocations.domain.UnallocatedCaseRisks
import uk.gov.justice.digital.hmpps.hmppsallocations.jpa.repository.UnallocatedCasesRepository
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.Optional

@Service
class GetUnallocatedCaseService(
  private val unallocatedCasesRepository: UnallocatedCasesRepository,
  @Qualifier("communityApiClientUserEnhanced") private val communityApiClient: CommunityApiClient,
  @Qualifier("assessmentApiClientUserEnhanced") private val assessmentApiClient: AssessmentApiClient,
  @Qualifier("assessRisksNeedsApiClientUserEnhanced") private val assessRisksNeedsApiClient: AssessRisksNeedsApiClient,
  @Qualifier("workforceAllocationsToDeliusApiClientUserEnhanced") private val workforceAllocationsToDeliusApiClient: WorkforceAllocationsToDeliusApiClient
) {

  fun getCase(crn: String, convictionId: Long): UnallocatedCaseDetails? =
    findUnallocatedCaseByConvictionIdOrConvictionNumber(crn, convictionId)?.let {
      val offenderSummary = communityApiClient.getOffenderDetails(crn)

      val conviction = communityApiClient.getConviction(crn, it.convictionId)

      val requirements = communityApiClient.getActiveRequirements(crn, it.convictionId)
      val associatedDocuments = getDocuments(crn, it.convictionNumber.toString())

      val assessment = assessmentApiClient.getAssessment(crn)
        .map { assessments -> Optional.ofNullable(assessments.maxByOrNull { a -> a.completed }) }
      val results = Mono.zip(offenderSummary, requirements, associatedDocuments, assessment, conviction).block()!!
      return UnallocatedCaseDetails.from(
        it, results.t1, results.t5?.offences,
        results.t5?.sentence?.expectedSentenceEndDate, results.t5?.sentence?.description, results.t2.requirements,
        results.t3,
        results.t4.orElse(null)
      )
    }

  private fun getDocuments(
    crn: String,
    convictionNumber: String
  ) = workforceAllocationsToDeliusApiClient.getDocuments(crn)
    .filter { document ->
      document.relatedTo.event == null || document.relatedTo.event.eventNumber == convictionNumber
    }
    .collectList()
    .map { documents ->
      val cpsPack = UnallocatedCaseDocument.from(
        documents.filter { it.relatedTo.type == "CPSPACK" }.maxByOrNull {
          it.dateCreated ?: LocalDateTime.MIN.atZone(
            ZoneId.systemDefault()
          )
        }
      )
      val preConviction = UnallocatedCaseDocument.from(
        documents.filter { it.relatedTo.type == "PRECONS" }.maxByOrNull {
          it.dateCreated ?: LocalDateTime.MIN.atZone(
            ZoneId.systemDefault()
          )
        }
      )
      val courtReport = UnallocatedCaseDocument.from(
        documents.filter { it.relatedTo.type == "COURT_REPORT" }.maxByOrNull {
          it.dateCreated ?: LocalDateTime.MIN.atZone(
            ZoneId.systemDefault()
          )
        }
      )
      Documents(courtReport, cpsPack, preConviction)
    }

  fun getCaseOverview(crn: String, convictionId: Long): UnallocatedCase? =
    findUnallocatedCaseByConvictionIdOrConvictionNumber(crn, convictionId)?.let {
      UnallocatedCase.from(it)
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

  fun getCaseConvictions(crn: String, excludeConvictionId: Long?): UnallocatedCaseConvictions? =
    unallocatedCasesRepository.findFirstCaseByCrn(crn)?.let {
      val convictions = communityApiClient.getAllConvictions(crn)
        .map { convictions ->
          convictions.groupBy { it.active }
        }.blockOptional().orElse(emptyMap())
      val activeConvictions = convictions.getOrDefault(true, emptyList())
        .filter { c -> c.sentence != null }
        .filter { c -> c.convictionId != excludeConvictionId }
        .map { conviction ->
          val practitioner = getCurrentOrderManager(conviction)

          UnallocatedCaseConviction.from(conviction, conviction.sentence!!.startDate, null, practitioner)
        }

      val inactiveConvictions = convictions.getOrDefault(false, emptyList())
        .filter { c -> c.sentence != null }
        .map { conviction ->
          val practitioner = getCurrentOrderManager(conviction)
          UnallocatedCaseConviction.from(conviction, null, conviction.sentence!!.terminationDate, practitioner)
        }

      return UnallocatedCaseConvictions.from(it, activeConvictions, inactiveConvictions)
    }

  private fun getCurrentOrderManager(conviction: Conviction): UnallocatedCaseConvictionPractitioner? =
    conviction.orderManagers.maxByOrNull { orderManager ->
      orderManager.dateStartOfAllocation ?: LocalDateTime.MIN
    }?.takeUnless { orderManager -> orderManager.isUnallocated }
      ?.let { orderManager -> UnallocatedCaseConvictionPractitioner(orderManager.name, orderManager.staffGrade) }

  fun getCaseRisks(crn: String, convictionId: Long): UnallocatedCaseRisks? =
    unallocatedCasesRepository.findCaseByCrnAndConvictionId(crn, convictionId)?.let {
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

  fun getChooseOffenderManagerCase(crn: String, convictionId: Long): ChooseOffenderManagerCase? =
    findUnallocatedCaseByConvictionIdOrConvictionNumber(
      crn,
      convictionId
    )?.let { from(it) }

  private fun findUnallocatedCaseByConvictionIdOrConvictionNumber(
    crn: String,
    convictionId: Long
  ) = if (convictionId < 100) {
    unallocatedCasesRepository.findCaseByCrnAndConvictionNumber(crn, convictionId.toInt())
  } else {
    unallocatedCasesRepository.findCaseByCrnAndConvictionId(crn, convictionId)
  }
}
