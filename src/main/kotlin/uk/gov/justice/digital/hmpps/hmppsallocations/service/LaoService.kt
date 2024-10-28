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
    val staffCodes = apopUsers.map { it.staffCode }
    val limitedAccessDetails = workforceAllocationsToDeliusApiClient.getAllUserAccessByCrn(crn)
    var apopExcluded = false

    limitedAccessDetails.excludedFrom.forEach {
      if (staffCodes.contains(it.staffCode)) {
        apopExcluded = true
        return@forEach
      }
    }

    return DeliusCrnRestrictions(limitedAccessDetails.excludedFrom.isNotEmpty(), limitedAccessDetails.restrictedTo.isNotEmpty(), apopExcluded)
  }

  suspend fun getCrnRestrictionsForUsers(crn: String, staffCodes: List<String>) = workforceAllocationsToDeliusApiClient.geUserAccessByCrnUsers(crn, staffCodes)
}
