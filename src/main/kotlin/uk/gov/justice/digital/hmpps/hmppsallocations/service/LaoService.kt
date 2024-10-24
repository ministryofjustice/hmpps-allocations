package uk.gov.justice.digital.hmpps.hmppsallocations.service

import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppsallocations.client.WorkforceAllocationsToDeliusApiClient
import uk.gov.justice.digital.hmpps.hmppsallocations.client.dto.DeliusCrnRestrictions

@Service
class LaoService(
  @Qualifier("workforceAllocationsToDeliusApiClientUserEnhanced")
  private val workforceAllocationsToDeliusApiClient: WorkforceAllocationsToDeliusApiClient,
) {

  suspend fun getCrnRestrictions(crn: String): DeliusCrnRestrictions {
    val apopUsers = workforceAllocationsToDeliusApiClient.getApopUsers()
    val usernames = apopUsers.map { it.username }
    val limitedAccessDetails = workforceAllocationsToDeliusApiClient.getAllUserAccessByCrn(crn)
    var apopExcluded = false

    limitedAccessDetails.restrictedTo.isNotEmpty()
    limitedAccessDetails.excludedFrom.isNotEmpty()

    limitedAccessDetails.restrictedTo.forEach {
      if (usernames.contains(it.username)) {
        apopExcluded = true
        return@forEach
      }
    }

    return DeliusCrnRestrictions(limitedAccessDetails.restrictedTo.isNotEmpty(), limitedAccessDetails.excludedFrom.isNotEmpty(), apopExcluded)
  }

  suspend fun getCrnRestrictionsForUsers(crn: String, staffIds: List<String>) = workforceAllocationsToDeliusApiClient.geUserAccessByCrnUsers(crn, staffIds)
}
