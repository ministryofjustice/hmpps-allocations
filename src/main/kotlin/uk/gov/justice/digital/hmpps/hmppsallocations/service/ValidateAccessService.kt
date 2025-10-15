package uk.gov.justice.digital.hmpps.hmppsallocations.service

import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClientResponseException
import uk.gov.justice.digital.hmpps.hmppsallocations.client.AllocationsFailedDependencyException
import uk.gov.justice.digital.hmpps.hmppsallocations.client.HmppsProbationEstateApiClient
import uk.gov.justice.digital.hmpps.hmppsallocations.client.WorkforceAllocationsToDeliusApiClient
import uk.gov.justice.digital.hmpps.hmppsallocations.service.exception.EntityNotFoundException
import uk.gov.justice.digital.hmpps.hmppsallocations.service.exception.NotAllowedForAccessException

@Suppress("SwallowedException")
@Component
class ValidateAccessService(
  @Qualifier("workforceAllocationsToDeliusApiClientUserEnhanced")
  private val workforceAllocationsToDeliusApiClient: WorkforceAllocationsToDeliusApiClient,
  private val hmppsProbationEstateApiClient: HmppsProbationEstateApiClient,
  private val getRegionsService: GetRegionsService,
) {
  companion object {
    val log = LoggerFactory.getLogger(this::class.java)
  }

  @Suppress("TooGenericExceptionCaught")
  suspend fun validateUserAccessForCrnAndStaff(staffCode: String, crn: String): Boolean {
    try {
      val allowedRegions = getRegionsService.getRegionsByUser(staffCode).regions

      val probationEstateForCrn =
        workforceAllocationsToDeliusApiClient.getCrnDetails(crn).manager.teamCode
      val probationEstateRegion =
        try {
          hmppsProbationEstateApiClient.getRegionsAndTeams(setOf(probationEstateForCrn))
        } catch (e: AllocationsFailedDependencyException) {
          log.warn("Probation estate client failed to get regions and teams for $probationEstateForCrn, failed with 424 error")
          null
        }
          ?.map { it.region.code }?.get(0)
      val result = allowedRegions.contains(probationEstateRegion)
      if (!result) {
        throw NotAllowedForAccessException(
          "User $staffCode does not have access to $crn due to region $probationEstateRegion",
          crn,
        )
      }
      return true
    } catch (e: IndexOutOfBoundsException) {
      throw EntityNotFoundException("Problem fetching regions: ${e.message}")
    } catch (e: WebClientResponseException) {
      if (e.statusCode.isSameCodeAs(HttpStatus.FORBIDDEN)) {
        throw NotAllowedForAccessException("User $staffCode does not have access to $crn", crn)
      } else {
        throw e
      }
    }
  }

  @Suppress("TooGenericExceptionCaught")
  suspend fun validateUserAccess(staffCode: String, crn: String, convictionNumber: String): Boolean {
    try {
      val allowedRegions = getRegionsService.getRegionsByUser(staffCode).regions

      val probationEstateForPoP =
        workforceAllocationsToDeliusApiClient.getUnallocatedEvents(crn)?.activeEvents?.filter { it.eventNumber == convictionNumber }
          ?.map { it.teamCode }?.distinct()?.get(0)
      val probationEstateRegion =
        try {
          hmppsProbationEstateApiClient.getRegionsAndTeams(setOf(probationEstateForPoP ?: ""))
        } catch (e: AllocationsFailedDependencyException) {
          log.warn("Probation estate client failed to get regions and teams for $probationEstateForPoP, failed with 424 error")
          null
        }
          ?.map { it.region.code }?.get(0)
      val result = allowedRegions.contains(probationEstateRegion)
      if (!result) {
        throw NotAllowedForAccessException(
          "User $staffCode does not have access to $crn due to region $probationEstateRegion",
          crn,
        )
      }
      return true
    } catch (e: IndexOutOfBoundsException) {
      throw EntityNotFoundException("Problem fetching regions: ${e.message}")
    } catch (e: WebClientResponseException) {
      if (e.statusCode.isSameCodeAs(HttpStatus.FORBIDDEN)) {
        throw NotAllowedForAccessException("User $staffCode does not have access to $crn", crn)
      } else {
        throw e
      }
    }
  }

  suspend fun validateUserAccess(userName: String, pdu: String): Boolean {
    val allowedRegions = getRegionsService.getRegionsByUser(userName).regions

    val pduRegion = hmppsProbationEstateApiClient.getProbationDeliveryUnitByCode(pdu)?.region?.code
    val validPdu = allowedRegions.contains(pduRegion)

    if (validPdu) {
      return true
    }

    throw NotAllowedForAccessException("User $userName does not have access to pdu $pdu", "")
  }

  suspend fun validateUserRegionAccess(userName: String, region: String): Boolean {
    val allowedRegions = workforceAllocationsToDeliusApiClient.getTeamsByUsername(userName).datasets.map { it.code }.distinct()

    val validRegion = allowedRegions.contains(region)

    if (validRegion) {
      return true
    }

    throw NotAllowedForAccessException("User $userName does not have access to region $region", "")
  }
}
