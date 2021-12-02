package uk.gov.justice.digital.hmpps.hmppsallocations.service

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppsallocations.client.HmppsTierApiClient
import uk.gov.justice.digital.hmpps.hmppsallocations.jpa.repository.UnallocatedCasesRepository

@Service
class TierCalculationService(
  private val hmppsTierApiClient: HmppsTierApiClient,
  private val repository: UnallocatedCasesRepository
) {

  fun updateTier(crn: String) {
    repository.findByCrn(crn).ifPresent {
      val tier = hmppsTierApiClient.getTierByCrn(crn)
      it.tier = tier
      repository.save(it)
      log.info("Tier updated for CRN: $crn")
    }
  }

  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }
}
