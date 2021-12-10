package uk.gov.justice.digital.hmpps.hmppsallocations.service

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppsallocations.client.CommunityApiClient
import uk.gov.justice.digital.hmpps.hmppsallocations.client.HmppsTierApiClient
import uk.gov.justice.digital.hmpps.hmppsallocations.domain.UnallocatedCase
import uk.gov.justice.digital.hmpps.hmppsallocations.jpa.repository.UnallocatedCasesRepository
import uk.gov.justice.digital.hmpps.hmppsallocations.service.ProbationStatusType.CURRENTLY_MANAGED
import uk.gov.justice.digital.hmpps.hmppsallocations.service.ProbationStatusType.NEW_TO_PROBATION
import uk.gov.justice.digital.hmpps.hmppsallocations.service.ProbationStatusType.PREVIOUSLY_MANAGED
import java.time.LocalDate

@Service
class UnallocatedCasesService(
  private val unallocatedCasesRepository: UnallocatedCasesRepository,
  private val communityApiClient: CommunityApiClient,
  private val hmppsTierApiClient: HmppsTierApiClient
) {

  fun getAll(): List<UnallocatedCase> {
    return unallocatedCasesRepository.findAll().map {
      UnallocatedCase.from(it)
    }
  }

  fun getSentenceDate(crn: String): LocalDate {
    val convictions = communityApiClient.getActiveConvictions(crn)
    log.info("convictions from com-api : {}", convictions.size)
    return convictions.sortedByDescending { c -> c.convictionDate }.first().sentence.startDate
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
      activeConvictions > 1 -> ProbationStatus(CURRENTLY_MANAGED)
      else -> {
        val allConvictions = communityApiClient.getInactiveConvictions(crn)
        return when {
          allConvictions.size > 1 -> {
            val mostRecentInactiveConvictionEndDate =
              allConvictions.filter { c -> c.sentence.terminationDate != null }
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
  val previousConvictionDate: LocalDate? = null
)

enum class ProbationStatusType(
  val status: String
) {
  CURRENTLY_MANAGED("Currently managed"),
  PREVIOUSLY_MANAGED("Previously managed"),
  NEW_TO_PROBATION("New to probation")
}
