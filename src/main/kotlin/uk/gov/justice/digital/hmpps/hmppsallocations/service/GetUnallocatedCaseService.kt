package uk.gov.justice.digital.hmpps.hmppsallocations.service

import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono
import uk.gov.justice.digital.hmpps.hmppsallocations.client.AssessmentApiClient
import uk.gov.justice.digital.hmpps.hmppsallocations.client.CommunityApiClient
import uk.gov.justice.digital.hmpps.hmppsallocations.domain.OffenderManagerDetails
import uk.gov.justice.digital.hmpps.hmppsallocations.domain.UnallocatedCase
import uk.gov.justice.digital.hmpps.hmppsallocations.domain.UnallocatedCaseConvictions
import uk.gov.justice.digital.hmpps.hmppsallocations.domain.UnallocatedCaseRisks
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
  private val gradeMapper: GradeMapper
) {

  fun getCase(crn: String): UnallocatedCase? =
    unallocatedCasesRepository.findCaseByCrn(crn)?.let {
      log.info("Found unallocated case for $crn")
      val offenderSummary = communityApiClient.getOffenderSummary(crn)

      val conviction = communityApiClient.getActiveConvictions(crn)
        .map { convictions ->
          convictions.filter { c -> c.sentence != null }
            .maxByOrNull { c -> c.convictionDate ?: LocalDate.MIN }
        }
        .block()!!

      val requirements = communityApiClient.getActiveRequirements(crn, conviction.convictionId)
      val courtReport = communityApiClient.getCourtReports(crn, conviction.convictionId)
        .map { courtReports ->
          Optional.ofNullable(
            courtReports
              .map { cr ->
                cr.courtReportType.description = courtReportMapper.deliusToReportType(cr.courtReportType.code, cr.courtReportType.description)
                cr
              }
              .maxByOrNull { cr -> cr.completedDate ?: LocalDateTime.MIN }
          )
        }

      val assessment = assessmentApiClient.getAssessment(crn)
        .map { assessments -> Optional.ofNullable(assessments.maxByOrNull { a -> a.completed }) }

      val results = Mono.zip(offenderSummary, requirements, courtReport, assessment).block()!!

      val age = Period.between(results.t1.dateOfBirth, LocalDate.now()).years
      return UnallocatedCase.from(
        it, results.t1, age, conviction.offences,
        conviction.sentence?.expectedSentenceEndDate, conviction.sentence?.description, results.t2.requirements,
        results.t3.orElse(null),
        results.t4.orElse(null)
      )
    }

  fun getAll(): List<UnallocatedCase> {
    return unallocatedCasesRepository.findAll().map {
      UnallocatedCase.from(it)
    }
  }

  fun getCaseConvictions(crn: String): UnallocatedCaseConvictions? =
    unallocatedCasesRepository.findCaseByCrn(crn)?.let {
      val convictions = communityApiClient.getAllConvictions(crn)
        .map { convictions ->
          convictions.groupBy { it.active }
        }.blockOptional().orElse(emptyMap())
      val activeConvictions = convictions.getOrDefault(true, emptyList())
        .filter { c -> c.sentence != null }
      val currentConviction = activeConvictions.filter { c -> c.sentence != null }
        .maxByOrNull { c -> c.convictionDate ?: LocalDate.MIN }

      val inactiveConvictions = convictions.getOrDefault(false, emptyList())
        .filter { c -> c.sentence != null }

      val offenderManager = communityApiClient.getOffenderManagerName(crn)
        .map { offenderManager ->
          val grade = gradeMapper.deliusToStaffGrade(offenderManager.grade?.code)
          OffenderManagerDetails(
            forenames = offenderManager.staff.forenames,
            surname = offenderManager.staff.surname,
            grade = grade
          )
        }.block()!!

      return UnallocatedCaseConvictions.from(it, activeConvictions.filter { it.convictionId != currentConviction!!.convictionId }, inactiveConvictions, offenderManager)
    }

  fun getCaseRisks(crn: String): UnallocatedCaseRisks? =
    unallocatedCasesRepository.findCaseByCrn(crn)?.let {
      val registrations = communityApiClient.getAllRegistrations(crn)
        .map { registrations ->
          registrations.registrations.groupBy { it.active }
        }.blockOptional().orElse(emptyMap())
      return UnallocatedCaseRisks.from(it, registrations.getOrDefault(true, emptyList()), registrations.getOrDefault(false, emptyList()))
    }

  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }
}
