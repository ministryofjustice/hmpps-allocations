package uk.gov.justice.digital.hmpps.hmppsallocations.service

import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppsallocations.client.WorkforceAllocationsToDeliusApiClient
import uk.gov.justice.digital.hmpps.hmppsallocations.client.dto.RegionList

@Service
class GetRegionsService(
  @Qualifier("workforceAllocationsToDeliusApiClientUserEnhanced")
  private val workforceAllocationsToDeliusApiClient: WorkforceAllocationsToDeliusApiClient,
) {

  suspend fun getRegionsByUser(staffId: String): RegionList {
    val teams = workforceAllocationsToDeliusApiClient.getTeamsByStaffId(staffId)
    return RegionList(teams.datasets.map { it.code }.distinct())
  }
}
