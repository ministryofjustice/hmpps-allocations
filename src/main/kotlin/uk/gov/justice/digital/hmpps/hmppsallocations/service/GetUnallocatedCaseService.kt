package uk.gov.justice.digital.hmpps.hmppsallocations.service

import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.cache.annotation.Cacheable
import org.springframework.core.io.Resource
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono
import uk.gov.justice.digital.hmpps.hmppsallocations.client.AssessRisksNeedsApiClient
import uk.gov.justice.digital.hmpps.hmppsallocations.client.AssessmentApiClient
import uk.gov.justice.digital.hmpps.hmppsallocations.client.CommunityApiClient
import uk.gov.justice.digital.hmpps.hmppsallocations.config.CacheConfiguration.Companion.INDUCTION_APPOINTMENT_CACHE_NAME
import uk.gov.justice.digital.hmpps.hmppsallocations.domain.CaseTypes
import uk.gov.justice.digital.hmpps.hmppsallocations.domain.Documents
import uk.gov.justice.digital.hmpps.hmppsallocations.domain.UnallocatedCase
import uk.gov.justice.digital.hmpps.hmppsallocations.domain.UnallocatedCaseConviction
import uk.gov.justice.digital.hmpps.hmppsallocations.domain.UnallocatedCaseConvictionPractitioner
import uk.gov.justice.digital.hmpps.hmppsallocations.domain.UnallocatedCaseConvictions
import uk.gov.justice.digital.hmpps.hmppsallocations.domain.UnallocatedCaseDocument
import uk.gov.justice.digital.hmpps.hmppsallocations.domain.UnallocatedCaseRisks
import uk.gov.justice.digital.hmpps.hmppsallocations.jpa.entity.UnallocatedCaseEntity
import uk.gov.justice.digital.hmpps.hmppsallocations.jpa.repository.UnallocatedCasesRepository
import uk.gov.justice.digital.hmpps.hmppsallocations.mapper.CourtReportMapper
import uk.gov.justice.digital.hmpps.hmppsallocations.mapper.GradeMapper
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.Period
import java.util.Optional

@Service
class GetUnallocatedCaseService(
  private val unallocatedCasesRepository: UnallocatedCasesRepository,
  @Qualifier("communityApiClientUserEnhanced") private val communityApiClient: CommunityApiClient,
  private val courtReportMapper: CourtReportMapper,
  @Qualifier("assessmentApiClientUserEnhanced") private val assessmentApiClient: AssessmentApiClient,
  private val gradeMapper: GradeMapper,
  @Qualifier("assessRisksNeedsApiClientUserEnhanced") private val assessRisksNeedsApiClient: AssessRisksNeedsApiClient,
) {

  fun getCase(crn: String, convictionId: Long): UnallocatedCase? =
    unallocatedCasesRepository.findCaseByCrnAndConvictionId(crn, convictionId)?.let {
      log.info("Found unallocated case for $crn")
      val offenderSummary = communityApiClient.getOffenderSummary(crn)

      val conviction = communityApiClient.getConviction(crn, convictionId)

      val requirements = communityApiClient.getActiveRequirements(crn, convictionId)
      val courtReportDocument = communityApiClient.getDocuments(crn, convictionId)
        .map { documents ->
          val documentTypes = documents.groupBy({ document -> document.type.code }, { document ->
            document.subType?.let { subType ->
              subType.description = courtReportMapper.deliusToReportType(subType.code, subType.description)
            }
            UnallocatedCaseDocument.from(document)
          })
            .mapValues { entry -> entry.value.maxByOrNull { document -> document?.completedDate ?: LocalDateTime.MIN } }
          Documents(documentTypes["COURT_REPORT_DOCUMENT"], documentTypes["CPSPACK_DOCUMENT"], documentTypes["PRECONS_DOCUMENT"])
        }

      val assessment = assessmentApiClient.getAssessment(crn)
        .map { assessments -> Optional.ofNullable(assessments.maxByOrNull { a -> a.completed }) }

      val results = Mono.zip(offenderSummary, requirements, courtReportDocument, assessment, conviction).block()!!

      val age = Period.between(results.t1.dateOfBirth, LocalDate.now()).years
      return UnallocatedCase.from(
        it, results.t1, age, results.t5.get().offences,
        results.t5.map { it.sentence?.expectedSentenceEndDate }.orElse(null), results.t5.map { it.sentence?.description }.orElse(null), results.t2.requirements,
        results.t3,
        results.t4.orElse(null)
      )
    }

  fun getCaseOverview(crn: String, convictionId: Long): UnallocatedCase? =
    unallocatedCasesRepository.findCaseByCrnAndConvictionId(crn, convictionId)?.let {
      UnallocatedCase.from(it)
    }

  fun getAll(): List<UnallocatedCase> {
    return unallocatedCasesRepository.findAll().map { unallocatedCase ->
      enrichInductionAppointment(unallocatedCase)
    }.map { it.map { unallocatedCase -> UnallocatedCase.from(unallocatedCase) }.block()!! }
  }

  @Cacheable(INDUCTION_APPOINTMENT_CACHE_NAME)
  fun enrichInductionAppointment(unallocatedCaseEntity: UnallocatedCaseEntity): Mono<UnallocatedCaseEntity> {
    if (inductionCaseTypes.contains(unallocatedCaseEntity.caseType)) {
      return communityApiClient.getInductionContacts(unallocatedCaseEntity.crn, unallocatedCaseEntity.sentenceDate).map { contacts ->
        unallocatedCaseEntity.initialAppointment = contacts.map { it.contactStart }.maxByOrNull { it }?.toLocalDate()
        unallocatedCasesRepository.save(unallocatedCaseEntity)
        unallocatedCaseEntity
      }
    }
    return Mono.just(unallocatedCaseEntity)
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
          val practitioner = conviction.orderManagers.maxByOrNull { orderManager -> orderManager.dateStartOfAllocation ?: LocalDateTime.MIN }?.let { orderManager -> UnallocatedCaseConvictionPractitioner(orderManager.name, gradeMapper.deliusToStaffGrade(orderManager.gradeCode)) }
          UnallocatedCaseConviction.from(conviction, conviction.sentence!!.startDate, null, practitioner)
        }

      val inactiveConvictions = convictions.getOrDefault(false, emptyList())
        .filter { c -> c.sentence != null }
        .map { conviction ->
          val practitioner = conviction.orderManagers.maxByOrNull { orderManager -> orderManager.dateStartOfAllocation ?: LocalDateTime.MIN }?.let { orderManager -> UnallocatedCaseConvictionPractitioner(orderManager.name, gradeMapper.deliusToStaffGrade(orderManager.gradeCode)) }
          UnallocatedCaseConviction.from(conviction, null, conviction.sentence!!.terminationDate, practitioner)
        }

      return UnallocatedCaseConvictions.from(it, activeConvictions, inactiveConvictions)
    }

  fun getCaseRisks(crn: String, convictionId: Long): UnallocatedCaseRisks? =
    unallocatedCasesRepository.findCaseByCrnAndConvictionId(crn, convictionId)?.let {
      val registrations = communityApiClient.getAllRegistrations(crn)
        .map { registrations ->
          registrations.registrations?.groupBy { registration -> registration.active } ?: emptyMap()
        }
      val riskSummary = assessRisksNeedsApiClient.getRiskSummary(crn)

      val latestRiskPredictor = assessRisksNeedsApiClient.getRiskPredictors(crn)
        .map { riskPredictors ->
          Optional.ofNullable(
            riskPredictors
              .filter { riskPredictor -> riskPredictor.rsrScoreLevel != null && riskPredictor.rsrPercentageScore != null }
              .maxByOrNull { riskPredictor -> riskPredictor.completedDate ?: LocalDateTime.MIN }
          )
        }

      val ogrs = communityApiClient.getAssessment(crn)

      val results = Mono.zip(registrations, riskSummary, latestRiskPredictor, ogrs).block()!!
      return UnallocatedCaseRisks.from(it, results.t1.getOrDefault(true, emptyList()), results.t1.getOrDefault(false, emptyList()), results.t2.orElse(null), results.t3.orElse(null), results.t4.orElse(null))
    }

  fun getCaseDocument(crn: String, documentId: String): Mono<ResponseEntity<Resource>> {
    return communityApiClient.getDocuments(crn, documentId)
  }

  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
    private val inductionCaseTypes = setOf(CaseTypes.COMMUNITY, CaseTypes.LICENSE)
  }
}
