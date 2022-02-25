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
    repository.findFirstByCrn(crn).ifPresent {
      hmppsTierApiClient.getTierByCrn(crn)?.let { tier ->
        it.tier = tier
      }
    }
  }
}
