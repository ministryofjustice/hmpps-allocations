package uk.gov.justice.digital.hmpps.hmppsallocations.service

import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.hmppsallocations.client.HmppsProbationEstateApiClient
import uk.gov.justice.digital.hmpps.hmppsallocations.client.WorkforceAllocationsToDeliusApiClient
import uk.gov.justice.digital.hmpps.hmppsallocations.service.exception.NotAllowedForAccessException

@Component
class ValidateAccessService(
  @Qualifier("workforceAllocationsToDeliusApiClientUserEnhanced")
  private val workforceAllocationsToDeliusApiClient: WorkforceAllocationsToDeliusApiClient,
  private val hmppsProbationEstateApiClient: HmppsProbationEstateApiClient,
  private val getRegionsService: GetRegionsService,
) {
  suspend fun validateUserAccess(crn: String, staffCode: String): Boolean {
    val allowedRegions = getRegionsService.getRegionsByUser(staffCode).regions
    val probationEstateForPoP = workforceAllocationsToDeliusApiClient.getUnallocatedEvents(crn)?.activeEvents?.map { it.teamCode }
    val probationEstateRegions =
      hmppsProbationEstateApiClient.getRegionsAndTeams(setOf(probationEstateForPoP ?: emptyList()).flatten().toSet())
        ?.map { it.region.code }
    val result = allowedRegions.intersect(probationEstateRegions?.toSet() ?: emptySet()).size > 0
    if (!result) {
      throw NotAllowedForAccessException("User $staffCode does not have access to $crn due to region", crn)
    }
    return true
  }
}
