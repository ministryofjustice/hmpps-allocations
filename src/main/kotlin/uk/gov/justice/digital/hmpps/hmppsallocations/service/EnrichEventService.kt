package uk.gov.justice.digital.hmpps.hmppsallocations.service

import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Service
import reactor.core.publisher.Flux
import uk.gov.justice.digital.hmpps.hmppsallocations.client.CommunityApiClient
import uk.gov.justice.digital.hmpps.hmppsallocations.client.HmppsTierApiClient
import uk.gov.justice.digital.hmpps.hmppsallocations.domain.Conviction
import uk.gov.justice.digital.hmpps.hmppsallocations.jpa.projection.ConvictionIdentifiers
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
      .mapNotNull { it.contactStart }
      .collectList()
      .block()?.maxOrNull()?.toLocalDate()
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

  fun getAllConvictionIdentifiersAssociatedToCrn(crn: String): Flux<ConvictionIdentifiers> =
    Flux.merge(
      Flux.fromIterable(unallocatedCasesRepository.findConvictionIdentifiersByCrn(crn)),
      communityApiClient.getActiveConvictions(crn)
        .map { conviction -> ConvictionIdentifiers(conviction.convictionId, conviction.convictionNumber) }
    ).distinct()
}
