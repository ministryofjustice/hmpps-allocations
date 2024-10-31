package uk.gov.justice.digital.hmpps.hmppsallocations.service

import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppsallocations.client.WorkforceAllocationsToDeliusApiClient
import uk.gov.justice.digital.hmpps.hmppsallocations.client.dto.CrnStaffRestrictionDetail
import uk.gov.justice.digital.hmpps.hmppsallocations.client.dto.CrnStaffRestrictions
import uk.gov.justice.digital.hmpps.hmppsallocations.client.dto.DeliusCrnRestrictions

@Service
class LaoService(
  @Qualifier("workforceAllocationsToDeliusApiClientUserEnhanced")
  private val workforceAllocationsToDeliusApiClient: WorkforceAllocationsToDeliusApiClient,
) {

  suspend fun getCrnRestrictions(crn: String): DeliusCrnRestrictions {
    val apopUsers = workforceAllocationsToDeliusApiClient.getApopUsers()
    val apopUsersStaffCodes = apopUsers.map { it.staffCode }
    val limitedAccessDetails = workforceAllocationsToDeliusApiClient.getUserAccessRestrictionsByCrn(crn)
    var apopExcluded = false

    limitedAccessDetails.excludedFrom.forEach {
      if (apopUsersStaffCodes.contains(it.staffCode)) {
        apopExcluded = true
        return@forEach
      }
    }

    return DeliusCrnRestrictions(limitedAccessDetails.excludedFrom.isNotEmpty(), limitedAccessDetails.restrictedTo.isNotEmpty(), apopExcluded)
  }

  suspend fun isCrnRestricted(crn: String, forApopUsers: Boolean): Boolean {
    val limitedAccessDetails = workforceAllocationsToDeliusApiClient.getUserAccessRestrictionsByCrn(crn)

    // No exclusions
    if (limitedAccessDetails.excludedFrom.isEmpty()) {
      return false
    }

    // exclusions, not checking apopusers
    if (!forApopUsers) {
      return true
    }

    val apopUsers = workforceAllocationsToDeliusApiClient.getApopUsers()
    val apopUsersStaffCodes = apopUsers.map { it.staffCode }

    limitedAccessDetails.excludedFrom.forEach {
      if (apopUsersStaffCodes.contains(it.staffCode)) {
        // apop user excluded
        return true
      }
    }

    // no apop user excluded
    return false
  }

  suspend fun getCrnRestrictionsForUsers(crn: String, staffCodes: List<String>): CrnStaffRestrictions {
    val deliusAccessRestrictionDetails = workforceAllocationsToDeliusApiClient.getAccessRestrictionsForStaffCodesByCrn(crn, staffCodes)
    val restrictedStaffCodes = deliusAccessRestrictionDetails.excludedFrom.map { it.staffCode }

    val arrStaffRestrictions = ArrayList<CrnStaffRestrictionDetail>()
    staffCodes.forEach {
      arrStaffRestrictions.add(CrnStaffRestrictionDetail(it, restrictedStaffCodes.contains(it)))
    }
    return CrnStaffRestrictions(crn, arrStaffRestrictions)
  }
}
