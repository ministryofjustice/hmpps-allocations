package uk.gov.justice.digital.hmpps.hmppsallocations.service

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppsallocations.client.HmppsTierApiClient
import uk.gov.justice.digital.hmpps.hmppsallocations.jpa.repository.UnallocatedCasesRepository
import javax.transaction.Transactional

@Service
class TierCalculationService(
  private val hmppsTierApiClient: HmppsTierApiClient,
  private val repository: UnallocatedCasesRepository
) {

  @Transactional
  fun updateTier(crn: String) {
    if (repository.existsByCrn(crn)) {
      hmppsTierApiClient.getTierByCrn(crn)?.let { tier ->
        repository.findByCrn(crn).forEach { it.tier = tier }
      }
    }
  }
}
