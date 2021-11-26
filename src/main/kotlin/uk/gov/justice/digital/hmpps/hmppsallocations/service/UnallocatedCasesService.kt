package uk.gov.justice.digital.hmpps.hmppsallocations.service

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppsallocations.client.CommunityApiClient
import uk.gov.justice.digital.hmpps.hmppsallocations.domain.UnallocatedCase
import uk.gov.justice.digital.hmpps.hmppsallocations.jpa.repository.UnallocatedCasesRepository
import java.time.LocalDate

@Service
class UnallocatedCasesService(
  private val unallocatedCasesRepository: UnallocatedCasesRepository,
  private val communityApiClient: CommunityApiClient
) {

  fun getAll(): List<UnallocatedCase> {
    return unallocatedCasesRepository.findAll().map {
      UnallocatedCase.from(it)
    }
  }

  fun getSentenceDate(crn: String): LocalDate {
    val convictions = communityApiClient.getConvictions(crn)
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

  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }
}
