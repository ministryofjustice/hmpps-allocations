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

    try {
      val unallocatedCase = repository.findByCrn(crn)
      val tier = hmppsTierApiClient.getTierByCrn(crn)
      unallocatedCase.tier = tier
      repository.save(unallocatedCase)
      log.info("Tier updated for CRN: $crn")
    } catch (e: Exception) {
      log.warn("No unallocated case with CRN: $crn ")
    }
  }

  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }
}
