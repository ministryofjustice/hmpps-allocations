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

  suspend fun getCrnRestrictions(userName: String, crn: String): DeliusCrnRestrictions {
    val limitedAccessDetails = workforceAllocationsToDeliusApiClient.getUserAccessRestrictionsByCrn(crn)
    var apopExcluded = false

    if (limitedAccessDetails.restrictedTo.isNotEmpty() && !limitedAccessDetails.restrictedTo.map { it.username.uppercase() }.contains(userName.uppercase())) {
      apopExcluded = true
    }

    if (!apopExcluded) {
      apopExcluded = limitedAccessDetails.excludedFrom.map { it.username.uppercase() }.contains(userName.uppercase())
    }

    return DeliusCrnRestrictions(limitedAccessDetails.excludedFrom.isNotEmpty(), limitedAccessDetails.restrictedTo.isNotEmpty(), apopExcluded)
  }

  suspend fun getCrnRestrictions(username: String, crns: List<String>): DeliusUserAccess {
    val crnsAccess = workforceAllocationsToDeliusApiClient.getUserAccess(crns).access

    crnsAccess.forEach {
      if (it.userRestricted || it.userExcluded) {
        val limitedAccessDetail = workforceAllocationsToDeliusApiClient.getUserAccessRestrictionsByCrn(it.crn)

        var apopExcluded = false
        if (it.userExcluded && limitedAccessDetail.excludedFrom.map { it.username.uppercase() }.contains(username.uppercase())) {
          apopExcluded = true
        }

        if (it.userRestricted && !limitedAccessDetail.restrictedTo.map { it.username.uppercase() }.contains(username.uppercase())) {
          apopExcluded = true
        }

        it.userRestricted = apopExcluded
        it.userExcluded = true
      }
    }

    return DeliusUserAccess(crnsAccess)
  }

  suspend fun getCrnRestrictionStatus(userName: String, crn: String): DeliusCrnRestrictionStatus {
    val limitedAccessDetails = workforceAllocationsToDeliusApiClient.getUserAccessRestrictionsByCrn(crn)
    var apopExcluded = false

    // restrictedTo list exists and user not on it ?
    if (limitedAccessDetails.restrictedTo.isNotEmpty()) {
      if (!limitedAccessDetails.restrictedTo.map { it.username.uppercase() }.contains(userName.uppercase())) {
        apopExcluded = true
      }
    }

    // excluded from list exists and user is on it
    if (!apopExcluded) {
      apopExcluded = limitedAccessDetails.excludedFrom.map { it.username.uppercase() }.contains(userName.uppercase())
    }

    return DeliusCrnRestrictionStatus(
      crn,
      limitedAccessDetails.restrictedTo.isNotEmpty() || limitedAccessDetails.excludedFrom.isNotEmpty(),
      apopExcluded,
    )
  }

  suspend fun isCrnRestricted(userName: String, crn: String): Boolean {
    val limitedAccessDetails = workforceAllocationsToDeliusApiClient.getUserAccessRestrictionsByCrn(crn)

    // Nobody excluded from and nobody restricted to
    if (limitedAccessDetails.excludedFrom.isEmpty() && limitedAccessDetails.restrictedTo.isEmpty()) {
      return false
    }

    // Case is excluded from certain individuals

    val excludedList = limitedAccessDetails.excludedFrom.map { it.username.uppercase() }

    if (excludedList.contains(userName.uppercase())) {
      // Case excluded from user
      throw NotAllowedForLAOException("User is excluded from viewing this case", crn)
    }

    // restricted to certain individuals only
    if (limitedAccessDetails.restrictedTo.isNotEmpty()) {
      if (!limitedAccessDetails.restrictedTo.map { it.username.uppercase() }.contains(userName.uppercase())) {
        throw NotAllowedForLAOException("User is excluded from viewing this case", crn)
      }
    }

    return limitedAccessDetails.excludedFrom.isNotEmpty() || limitedAccessDetails.restrictedTo.isNotEmpty()
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
    val deliusAccessRestrictionDetails = workforceAllocationsToDeliusApiClient.getUserAccessRestrictionsByCrn(crn)
    val excludedFromStaffCodes = deliusAccessRestrictionDetails.excludedFrom.map { it.staffCode }
    val restrictedToStaffCodes = deliusAccessRestrictionDetails.restrictedTo.map { it.staffCode }

    val arrStaffRestrictions = ArrayList<CrnStaffRestrictionDetail>()
    staffCodes.forEach {
      var isExcluded = !validStaffCodes.contains(it)
      if (!isExcluded) {
        if (restrictedToStaffCodes.isNotEmpty()) {
          isExcluded = !restrictedToStaffCodes.contains(it)
        }

        if (!isExcluded) {
          isExcluded = excludedFromStaffCodes.contains(it)
        }
      }
      arrStaffRestrictions.add(CrnStaffRestrictionDetail(it, isExcluded))
    }
    return CrnStaffRestrictions(crn, arrStaffRestrictions)
  }
}
