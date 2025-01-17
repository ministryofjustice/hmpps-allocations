package uk.gov.justice.digital.hmpps.hmppsallocations.service

import jakarta.transaction.Transactional
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppsallocations.client.HmppsTierApiClient
import uk.gov.justice.digital.hmpps.hmppsallocations.client.MissingTierException
import uk.gov.justice.digital.hmpps.hmppsallocations.jpa.repository.UnallocatedCasesRepository

@Service
class TierCalculationService(
  private val hmppsTierApiClient: HmppsTierApiClient,
  private val repository: UnallocatedCasesRepository,
) {

  @Transactional
  suspend fun updateTier(crn: String) {
    if (repository.existsByCrn(crn)) {
      val tier = getTier(crn)
      repository.findByCrn(crn).forEach {
        it.tier = tier
        repository.save(it)
      }
    }
  }

  suspend fun getTier(crn: String): String = hmppsTierApiClient.getTierByCrn(crn = crn) ?: throw MissingTierException("Missing tier: $crn")
}
