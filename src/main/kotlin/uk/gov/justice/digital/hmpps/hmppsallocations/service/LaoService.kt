package uk.gov.justice.digital.hmpps.hmppsallocations.service

import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppsallocations.client.WorkforceAllocationsToDeliusApiClient
import uk.gov.justice.digital.hmpps.hmppsallocations.client.dto.CrnStaffRestrictionDetail
import uk.gov.justice.digital.hmpps.hmppsallocations.client.dto.CrnStaffRestrictions
import uk.gov.justice.digital.hmpps.hmppsallocations.client.dto.DeliusCrnRestrictions
import uk.gov.justice.digital.hmpps.hmppsallocations.service.exception.NotAllowedForLAOException

@Service
class LaoService(
  @Qualifier("workforceAllocationsToDeliusApiClientUserEnhanced")
  private val workforceAllocationsToDeliusApiClient: WorkforceAllocationsToDeliusApiClient,
) {

  suspend fun getCrnRestrictions(crn: String): DeliusCrnRestrictions {
    val apopUsers = workforceAllocationsToDeliusApiClient.getApopUsers()
    val apopUsersStaffCodes = apopUsers.filter { it.staffCode != null }.map { it.staffCode }
    val limitedAccessDetails = workforceAllocationsToDeliusApiClient.getUserAccessRestrictionsByCrn(crn)
    var apopExcluded = false

    if (limitedAccessDetails.restrictedTo.isNotEmpty()) {
      apopExcluded = true
    } else {
      limitedAccessDetails.excludedFrom.forEach {
        if (apopUsersStaffCodes.contains(it.staffCode)) {
          apopExcluded = true
          return@forEach
        }
      }
    }

    return DeliusCrnRestrictions(limitedAccessDetails.excludedFrom.isNotEmpty(), limitedAccessDetails.restrictedTo.isNotEmpty(), apopExcluded)
  }

  suspend fun isCrnRestricted(crn: String): Boolean {
    val limitedAccessDetails = workforceAllocationsToDeliusApiClient.getUserAccessRestrictionsByCrn(crn)

    // Nobody excluded from and nobody restricted to
    if (limitedAccessDetails.excludedFrom.isEmpty() && limitedAccessDetails.restrictedTo.isEmpty()) {
      return false
    }

    // restricted to certain individuals only
    if (limitedAccessDetails.restrictedTo.isNotEmpty()) {
      throw NotAllowedForLAOException("A user of APoP is excluded from viewing this case", crn)
    }

    val apopUsers = workforceAllocationsToDeliusApiClient.getApopUsers()
    val apopUsersStaffCodes = apopUsers.map { it.staffCode }

    // Case is excluded from certain individuals

    limitedAccessDetails.excludedFrom.filter { it.staffCode != null }.forEach {
      if (apopUsersStaffCodes.contains(it.staffCode)) {
        // Case excluded from an APoP user
        throw NotAllowedForLAOException("A user of APoP is excluded from viewing this case", crn)
      }
    }

    return true
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
