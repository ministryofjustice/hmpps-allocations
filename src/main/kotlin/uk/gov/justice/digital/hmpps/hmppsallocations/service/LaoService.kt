package uk.gov.justice.digital.hmpps.hmppsallocations.service

import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppsallocations.client.DeliusUserAccess
import uk.gov.justice.digital.hmpps.hmppsallocations.client.WorkforceAllocationsToDeliusApiClient
import uk.gov.justice.digital.hmpps.hmppsallocations.client.dto.CrnStaffRestrictionDetail
import uk.gov.justice.digital.hmpps.hmppsallocations.client.dto.CrnStaffRestrictions
import uk.gov.justice.digital.hmpps.hmppsallocations.client.dto.DeliusCrnRestrictionStatus
import uk.gov.justice.digital.hmpps.hmppsallocations.client.dto.DeliusCrnRestrictions
import uk.gov.justice.digital.hmpps.hmppsallocations.service.exception.NotAllowedForLAOException

@Service
class LaoService(
  @Qualifier("workforceAllocationsToDeliusApiClientUserEnhanced")
  private val workforceAllocationsToDeliusApiClient: WorkforceAllocationsToDeliusApiClient,
) {
  companion object {
    val log = LoggerFactory.getLogger(this::class.java)!!
  }

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

  suspend fun getCrnRestrictions(crns: List<String>): DeliusUserAccess {
    val apopUsers = workforceAllocationsToDeliusApiClient.getApopUsers()
    val apopUsersStaffCodes = apopUsers.filter { it.staffCode != null }.map { it.staffCode }
    val crnsAccess = workforceAllocationsToDeliusApiClient.getUserAccess(crns).access

    crnsAccess.forEach {
      if (!it.userRestricted) {
        if (it.userExcluded) {
          val limitedAccessDetail = workforceAllocationsToDeliusApiClient.getUserAccessRestrictionsByCrn(it.crn)

          var apopExcluded = false
          limitedAccessDetail.excludedFrom.forEach {
            if (apopUsersStaffCodes.contains(it.staffCode)) {
              apopExcluded = true
              return@forEach
            }
          }
          it.userRestricted = apopExcluded
        }
      }
    }

    return DeliusUserAccess(crnsAccess)
  }

  suspend fun getCrnRestrictionStatus(crn: String): DeliusCrnRestrictionStatus {
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

    return DeliusCrnRestrictionStatus(
      crn,
      limitedAccessDetails.restrictedTo.isNotEmpty() || limitedAccessDetails.excludedFrom.isNotEmpty(),
      apopExcluded,
    )
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
    val validStaffCodes: ArrayList<String> = ArrayList()
    staffCodes.forEach { staffCode ->
      try {
        validStaffCodes.add(workforceAllocationsToDeliusApiClient.getOfficerView(staffCode).code)
      } catch (exception: Exception) {
        log.warn("Officer with code $staffCode not found with message ${exception.message} for crn $crn")
      }
    }
    val deliusAccessRestrictionDetails = workforceAllocationsToDeliusApiClient.getAccessRestrictionsForStaffCodesByCrn(crn, staffCodes)
    val restrictedStaffCodes = deliusAccessRestrictionDetails.excludedFrom.map { it.staffCode }

    val arrStaffRestrictions = ArrayList<CrnStaffRestrictionDetail>()
    staffCodes.forEach {
      var result = !validStaffCodes.contains(it)
      if (!result) result = restrictedStaffCodes.contains(it)
      arrStaffRestrictions.add(CrnStaffRestrictionDetail(it, result))
    }
    return CrnStaffRestrictions(crn, arrStaffRestrictions)
  }
}
