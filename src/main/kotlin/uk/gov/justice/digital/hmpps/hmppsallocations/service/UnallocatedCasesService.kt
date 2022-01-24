package uk.gov.justice.digital.hmpps.hmppsallocations.service

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppsallocations.client.CommunityApiClient
import uk.gov.justice.digital.hmpps.hmppsallocations.client.HmppsTierApiClient
import uk.gov.justice.digital.hmpps.hmppsallocations.domain.OffenderManagerDetails
import uk.gov.justice.digital.hmpps.hmppsallocations.domain.UnallocatedCase
import uk.gov.justice.digital.hmpps.hmppsallocations.jpa.repository.UnallocatedCasesRepository
import uk.gov.justice.digital.hmpps.hmppsallocations.mapper.GradeMapper
import uk.gov.justice.digital.hmpps.hmppsallocations.service.ProbationStatusType.CURRENTLY_MANAGED
import uk.gov.justice.digital.hmpps.hmppsallocations.service.ProbationStatusType.NEW_TO_PROBATION
import uk.gov.justice.digital.hmpps.hmppsallocations.service.ProbationStatusType.PREVIOUSLY_MANAGED
import java.time.LocalDate
import java.time.Period

@Service
class UnallocatedCasesService(
  private val unallocatedCasesRepository: UnallocatedCasesRepository,
  private val communityApiClient: CommunityApiClient,
  private val hmppsTierApiClient: HmppsTierApiClient,
  private val gradeMapper: GradeMapper
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
      val age = Period.between(offenderSummary.dateOfBirth, LocalDate.now()).years
      val conviction = communityApiClient.getActiveConvictions(crn).filter { c -> c.sentence != null }
        .maxByOrNull { c -> c.convictionDate ?: LocalDate.MIN }!!
      val requirements = communityApiClient.getActiveRequirements(crn, conviction.convictionId)
      return UnallocatedCase.from(
        it, offenderSummary.gender, offenderSummary.dateOfBirth, age, conviction.offences,
        conviction.sentence?.expectedSentenceEndDate, requirements.requirements
      )
    }

  fun getSentenceDate(crn: String): LocalDate {
    val convictions = communityApiClient.getActiveConvictions(crn)
    log.info("convictions from com-api : {}", convictions.size)
    return convictions.filter { c -> c.sentence != null }
      .maxByOrNull { c -> c.convictionDate ?: LocalDate.MIN }!!.sentence!!.startDate
  }

  fun getInitialAppointmentDate(crn: String, contactsFromDate: LocalDate): LocalDate? {
    val contacts = communityApiClient.getInductionContacts(crn, contactsFromDate)
    log.info("contacts from com-api : {}", contacts.size)

    return contacts.minByOrNull { c -> c.contactStart }?.contactStart?.toLocalDate()
  }

  fun getOffenderName(crn: String): String {
    val offenderSummary = communityApiClient.getOffenderSummary(crn)
    return "${offenderSummary.firstName} ${offenderSummary.surname}"
  }

  fun getTier(crn: String): String {
    return hmppsTierApiClient.getTierByCrn(crn)
  }

  fun getProbationStatus(crn: String): ProbationStatus {
    val activeConvictions = communityApiClient.getActiveConvictions(crn).size
    return when {

      activeConvictions > 1 -> {
        val offenderManager = communityApiClient.getOffenderManagerName(crn)
        val grade = gradeMapper.deliusToStaffGrade(offenderManager.grade?.code)
        return ProbationStatus(
          CURRENTLY_MANAGED,
          offenderManagerDetails = OffenderManagerDetails(
            forenames = offenderManager.staff.forenames,
            surname = offenderManager.staff.surname,
            grade = grade
          )
        )
      }
      else -> {
        val inactiveConvictions = communityApiClient.getInactiveConvictions(crn)
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
