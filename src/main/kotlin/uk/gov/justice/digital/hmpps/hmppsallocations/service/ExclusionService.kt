package uk.gov.justice.digital.hmpps.hmppsallocations.service

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppsallocations.client.WorkforceAllocationsToDeliusApiClient

@Service
class ExclusionService(
  private val workforceAllocationsToDeliusApiClient: WorkforceAllocationsToDeliusApiClient,
) {

  suspend fun checkIfAnyUserExcluded(crn: String?): Boolean {
    TODO("need Gary's changes to the client merged to implement")
  }
}
