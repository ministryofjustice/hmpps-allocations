package uk.gov.justice.digital.hmpps.hmppsallocations.service

import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Service
import reactor.core.publisher.Flux
import uk.gov.justice.digital.hmpps.hmppsallocations.client.CommunityApiClient
import uk.gov.justice.digital.hmpps.hmppsallocations.client.HmppsTierApiClient
import uk.gov.justice.digital.hmpps.hmppsallocations.domain.Conviction
import uk.gov.justice.digital.hmpps.hmppsallocations.domain.OffenderManagerDetails
import uk.gov.justice.digital.hmpps.hmppsallocations.jpa.repository.UnallocatedCasesRepository
import java.time.LocalDate

@Service
class EnrichEventService(
  @Qualifier("communityApiClient") private val communityApiClient: CommunityApiClient,
  @Qualifier("hmppsTierApiClient") private val hmppsTierApiClient: HmppsTierApiClient,
  private val unallocatedCasesRepository: UnallocatedCasesRepository
) {

  fun getInitialAppointmentDate(crn: String, contactsFromDate: LocalDate): LocalDate? {
    return communityApiClient.getInductionContacts(crn, contactsFromDate)
      .mapNotNull { contacts ->
        contacts.maxByOrNull { c -> c.contactStart }?.contactStart?.toLocalDate()
      }
      .block()
  }

  fun getOffenderName(crn: String): String {
    return communityApiClient.getOffenderDetails(crn)
      .map { "${it.firstName} ${it.surname}" }
      .block()!!
  }

  fun getTier(crn: String): String? {
    return hmppsTierApiClient.getTierByCrn(crn)
  }

  fun getActiveSentencedConvictions(crn: String): List<Conviction> {
    return communityApiClient.getActiveConvictions(crn).filter { conviction -> conviction.sentence != null }
      .collectList()
      .block() ?: emptyList()
  }

  fun getProbationStatus(crn: String, activeConvictions: List<Conviction>): ProbationStatus {
    val offenderManager = communityApiClient.getOffenderManagerName(crn)
      .block()!!
    return when {
      activeConvictions.size > 1 && !offenderManager.isUnallocated -> {
        ProbationStatus(ProbationStatusType.CURRENTLY_MANAGED, offenderManagerDetails = OffenderManagerDetails.from(offenderManager))
      }
      else -> {
        val inactiveConvictions = communityApiClient.getInactiveConvictions(crn).block() ?: emptyList()
        return when {
          inactiveConvictions.isNotEmpty() -> {
            ProbationStatus(
              ProbationStatusType.PREVIOUSLY_MANAGED,
              OffenderManagerDetails.from(offenderManager).takeUnless { offenderManager.isUnallocated }
            )
          }
          else -> ProbationStatus(
            ProbationStatusType.NEW_TO_PROBATION,
            offenderManagerDetails = OffenderManagerDetails.from(offenderManager)
          )
        }
      }
    }
  }

  fun getAllConvictionIdsAssociatedToCrn(crn: String): Flux<Long> =
    Flux.merge(
      Flux.fromIterable(unallocatedCasesRepository.findConvictionIdsByCrn(crn)).map { it.getConvictionId() },
      communityApiClient.getActiveConvictions(crn)
        .map { conviction -> conviction.convictionId }
    ).distinct()
}

data class ProbationStatus(
  val status: ProbationStatusType,
  val offenderManagerDetails: OffenderManagerDetails? = null
)

enum class ProbationStatusType(
  val status: String
) {
  CURRENTLY_MANAGED("Currently managed"),
  PREVIOUSLY_MANAGED("Previously managed"),
  NEW_TO_PROBATION("New to probation")
}
