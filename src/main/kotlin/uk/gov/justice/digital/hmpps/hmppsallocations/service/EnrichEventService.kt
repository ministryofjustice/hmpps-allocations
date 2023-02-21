package uk.gov.justice.digital.hmpps.hmppsallocations.service

import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppsallocations.client.HmppsTierApiClient

@Service
class EnrichEventService(
  @Qualifier("hmppsTierApiClient") private val hmppsTierApiClient: HmppsTierApiClient
) {

  fun getTier(crn: String): String? {
    return hmppsTierApiClient.getTierByCrn(crn)
  }
}
