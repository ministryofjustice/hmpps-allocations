package uk.gov.justice.digital.hmpps.hmppsallocations.service

import uk.gov.justice.digital.hmpps.hmppsallocations.client.HmppsTierApiClient

class CalculationTierService(private val hmppsTierApiClient: HmppsTierApiClient) {

  fun sendCalculationDataEvent(crn: String) {
    hmppsTierApiClient.getTierByCrn(crn)
  }
}
