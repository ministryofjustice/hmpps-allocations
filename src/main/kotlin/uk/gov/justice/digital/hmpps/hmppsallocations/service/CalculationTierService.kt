package uk.gov.justice.digital.hmpps.hmppsallocations.service

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppsallocations.client.HmppsTierApiClient
import uk.gov.justice.digital.hmpps.hmppsallocations.jpa.repository.UnallocatedCasesRepository

@Service
class CalculationTierService(
  private val hmppsTierApiClient: HmppsTierApiClient,
  private val repository: UnallocatedCasesRepository
) {

  fun updateCalculationTier(crn: String) {

    try {
      val tier = hmppsTierApiClient.getTierByCrn(crn)
      val uce = repository.findByCrn(crn)
      uce.tier = tier
      repository.save(uce)
      log.info("Tier: $tier has bene updated for crn : $crn")
    } catch (e: Exception) {
      log.warn("No CRN: $crn found ")
    }
  }

  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }
}
