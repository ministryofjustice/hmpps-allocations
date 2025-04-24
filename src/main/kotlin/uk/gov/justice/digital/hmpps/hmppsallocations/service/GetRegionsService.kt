package uk.gov.justice.digital.hmpps.hmppsallocations.service

import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppsallocations.client.WorkforceAllocationsToDeliusApiClient
import uk.gov.justice.digital.hmpps.hmppsallocations.client.dto.RegionList
import uk.gov.justice.digital.hmpps.hmppsallocations.service.exception.EntityNotFoundException

@Service
class GetRegionsService(
  @Qualifier("workforceAllocationsToDeliusApiClientUserEnhanced")
  private val workforceAllocationsToDeliusApiClient: WorkforceAllocationsToDeliusApiClient,
) {

  suspend fun getRegionsByUser(userName: String): RegionList {
    val users = workforceAllocationsToDeliusApiClient.getApopUsers()
    val staffCodes = users.filter { it.username == userName && it.staffCode != null }.map { it.staffCode }.distinct()
    if (staffCodes.isEmpty() || staffCodes.first() == null) {
      throw EntityNotFoundException("User name not found :$staffCodes")
    } else {
      val teams = workforceAllocationsToDeliusApiClient.getTeamsByStaffId(staffCodes.first()!!)
      return RegionList(teams.teams.map { it.localAdminUnit.probationDeliveryUnit.provider.code }.distinct())
    }
  }
}
