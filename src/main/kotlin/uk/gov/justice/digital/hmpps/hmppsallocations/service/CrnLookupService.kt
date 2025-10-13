package uk.gov.justice.digital.hmpps.hmppsallocations.service

import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppsallocations.client.WorkforceAllocationsToDeliusApiClient
import uk.gov.justice.digital.hmpps.hmppsallocations.client.dto.CrnDetails

@Service
class CrnLookupService(
  @Qualifier("workforceAllocationsToDeliusApiClientUserEnhanced")
  private val workforceAllocationsToDeliusApiClient: WorkforceAllocationsToDeliusApiClient,
)
{

  suspend fun getCrnDetails(crn: String) : CrnDetails
    {
      return workforceAllocationsToDeliusApiClient.getCrnDetails(crn)
    }
}