package uk.gov.justice.digital.hmpps.hmppsallocations.service

import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppsallocations.client.CommunityApiClient
import uk.gov.justice.digital.hmpps.hmppsallocations.client.HmppsTierApiClient
import uk.gov.justice.digital.hmpps.hmppsallocations.domain.OffenderManagerDetails
import uk.gov.justice.digital.hmpps.hmppsallocations.mapper.GradeMapper
import java.time.LocalDate

@Service
class EnrichEventService(
  @Qualifier("communityApiClient") private val communityApiClient: CommunityApiClient,
  @Qualifier("hmppsTierApiClient") private val hmppsTierApiClient: HmppsTierApiClient,
  private val gradeMapper: GradeMapper
) {

  fun getSentenceDate(crn: String, convictionId: Long): LocalDate? {
    return communityApiClient
      .getConviction(crn, convictionId)
      .map { conviction ->
        conviction
          .map { it.sentence?.startDate }
          .orElse(null)
      }.block()!!
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

  fun getTier(crn: String): String? {
    return hmppsTierApiClient.getTierByCrn(crn)
  }

  fun getProbationStatus(crn: String): ProbationStatus {
    val activeConvictions = (communityApiClient.getActiveConvictions(crn).block() ?: emptyList()).size
    val offenderManager = communityApiClient.getOffenderManagerName(crn)
      .map { offenderManager ->
        val grade = gradeMapper.deliusToStaffGrade(offenderManager.grade?.code)
        OffenderManagerDetails(
          forenames = offenderManager.staff.forenames,
          surname = offenderManager.staff.surname,
          grade = grade
        )
      }.block()!!
    return when {
      activeConvictions > 1 -> {
        ProbationStatus(
          ProbationStatusType.CURRENTLY_MANAGED,
          offenderManagerDetails = offenderManager
        )
      }
      else -> {
        val inactiveConvictions = communityApiClient.getInactiveConvictions(crn).block() ?: emptyList()
        return when {
          inactiveConvictions.isNotEmpty() -> {
            val mostRecentInactiveConvictionEndDate =
              inactiveConvictions.filter { c -> c.sentence.terminationDate != null }
                .map { c -> c.sentence.terminationDate!! }
                .maxByOrNull { it }
            ProbationStatus(ProbationStatusType.PREVIOUSLY_MANAGED, mostRecentInactiveConvictionEndDate, offenderManager)
          }
          else -> ProbationStatus(ProbationStatusType.NEW_TO_PROBATION, offenderManagerDetails = offenderManager)
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
