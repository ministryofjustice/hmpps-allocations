package uk.gov.justice.digital.hmpps.hmppsallocations.service

import org.slf4j.LoggerFactory
import reactor.core.publisher.Mono
import uk.gov.justice.digital.hmpps.hmppsallocations.client.AssessmentApiClient
import uk.gov.justice.digital.hmpps.hmppsallocations.client.CommunityApiClient
import uk.gov.justice.digital.hmpps.hmppsallocations.client.HmppsTierApiClient
import uk.gov.justice.digital.hmpps.hmppsallocations.domain.OffenderManagerDetails
import uk.gov.justice.digital.hmpps.hmppsallocations.domain.UnallocatedCase
import uk.gov.justice.digital.hmpps.hmppsallocations.domain.UnallocatedCaseConvictions
import uk.gov.justice.digital.hmpps.hmppsallocations.jpa.repository.UnallocatedCasesRepository
import uk.gov.justice.digital.hmpps.hmppsallocations.mapper.CourtReportMapper
import uk.gov.justice.digital.hmpps.hmppsallocations.mapper.GradeMapper
import uk.gov.justice.digital.hmpps.hmppsallocations.service.ProbationStatusType.CURRENTLY_MANAGED
import uk.gov.justice.digital.hmpps.hmppsallocations.service.ProbationStatusType.NEW_TO_PROBATION
import uk.gov.justice.digital.hmpps.hmppsallocations.service.ProbationStatusType.PREVIOUSLY_MANAGED
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.Period
import java.util.Optional

class UnallocatedCasesService(
  private val unallocatedCasesRepository: UnallocatedCasesRepository,
  private val communityApiClient: CommunityApiClient,
  private val hmppsTierApiClient: HmppsTierApiClient,
  private val assessmentApiClient: AssessmentApiClient,
  private val gradeMapper: GradeMapper,
  private val courtReportMapper: CourtReportMapper
) {

  fun getAll(): List<UnallocatedCase> {
    return unallocatedCasesRepository.findAll().map {
      UnallocatedCase.from(it)
    }
  }

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
        .map { a -> Optional.ofNullable(a.assessedOn) }

      val results = Mono.zip(offenderSummary, requirements, courtReport, assessment).block()!!

      val age = Period.between(results.t1.dateOfBirth, LocalDate.now()).years
      return UnallocatedCase.from(
        it, results.t1, age, conviction.offences,
        conviction.sentence?.expectedSentenceEndDate, results.t2.requirements,
        results.t3.orElse(null),
        results.t4.orElse(null)
      )
    }

  fun getSentenceDate(crn: String): LocalDate {
    return communityApiClient.getActiveConvictions(crn)
      .map { convictions ->
        log.info("convictions from com-api : {}", convictions.size)
        convictions.filter { c -> c.sentence != null }
          .maxByOrNull { c -> c.convictionDate ?: LocalDate.MIN }!!.sentence!!.startDate
      }
      .block()!!
  }

  fun getInitialAppointmentDate(crn: String, contactsFromDate: LocalDate): LocalDate? {
    return communityApiClient.getInductionContacts(crn, contactsFromDate)
      .mapNotNull { contacts ->
        log.info("contacts from com-api : {}", contacts.size)
        contacts.minByOrNull { c -> c.contactStart }?.contactStart?.toLocalDate()
      }
      .block()
  }

  fun getOffenderName(crn: String): String {
    return communityApiClient.getOffenderSummary(crn)
      .map { "${it.firstName} ${it.surname}" }
      .block()!!
  }

  fun getTier(crn: String): String {
    return hmppsTierApiClient.getTierByCrn(crn)
  }

  fun getProbationStatus(crn: String): ProbationStatus {
    val activeConvictions = (communityApiClient.getActiveConvictions(crn).block() ?: emptyList()).size
    return when {

      activeConvictions > 1 -> {
        return communityApiClient.getOffenderManagerName(crn)
          .map { offenderManager ->
            val grade = gradeMapper.deliusToStaffGrade(offenderManager.grade?.code)
            ProbationStatus(
              CURRENTLY_MANAGED,
              offenderManagerDetails = OffenderManagerDetails(
                forenames = offenderManager.staff.forenames,
                surname = offenderManager.staff.surname,
                grade = grade
              )
            )
          }.block()!!
      }
      else -> {
        val inactiveConvictions = communityApiClient.getInactiveConvictions(crn).block() ?: emptyList()
        return when {
          inactiveConvictions.isNotEmpty() -> {
            val mostRecentInactiveConvictionEndDate =
              inactiveConvictions.filter { c -> c.sentence.terminationDate != null }
                .map { c -> c.sentence.terminationDate!! }
                .maxByOrNull { it }
            ProbationStatus(PREVIOUSLY_MANAGED, mostRecentInactiveConvictionEndDate)
          }
          else -> ProbationStatus(NEW_TO_PROBATION)
        }
      }
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

  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }
}

data class ProbationStatus(
  val status: ProbationStatusType,
  val previousConvictionDate: LocalDate? = null,
  val offenderManagerDetails: OffenderManagerDetails? = null
)

enum class ProbationStatusType(
  val status: String
) {
  CURRENTLY_MANAGED("Currently managed"),
  PREVIOUSLY_MANAGED("Previously managed"),
  NEW_TO_PROBATION("New to probation")
}
