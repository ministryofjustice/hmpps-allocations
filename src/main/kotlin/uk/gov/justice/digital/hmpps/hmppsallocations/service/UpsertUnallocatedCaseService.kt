package uk.gov.justice.digital.hmpps.hmppsallocations.service

import jakarta.transaction.Transactional
import org.slf4j.LoggerFactory
import org.slf4j.MDC
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppsallocations.client.HmppsTierApiClient
import uk.gov.justice.digital.hmpps.hmppsallocations.client.MissingTierException
import uk.gov.justice.digital.hmpps.hmppsallocations.client.WorkforceAllocationsToDeliusApiClient
import uk.gov.justice.digital.hmpps.hmppsallocations.jpa.repository.UnallocatedCasesRepository

const val LAO = "LAO logging"
private const val CRN = "CRN"

@Service
class UpsertUnallocatedCaseService(
  private val databaseService: UnallocatedDataBaseOperationService,
  private val repository: UnallocatedCasesRepository,
  @Qualifier("hmppsTierApiClient") private val hmppsTierApiClient: HmppsTierApiClient,
  @Qualifier("workforceAllocationsToDeliusApiClient") private val workforceAllocationsToDeliusApiClient: WorkforceAllocationsToDeliusApiClient,
) {

  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }

  @Transactional
  suspend fun upsertUnallocatedCase(crn: String) {
    log.debug("upsert unallocated case for crn: $crn")
    val storedUnallocatedEvents = repository.findByCrn(crn)
    val userAccess = workforceAllocationsToDeliusApiClient.getUserAccess(crn = crn)
    MDC.put(LAO, "Incoming cases")
    MDC.put(CRN, crn)
    if (userAccess!!.userRestricted) {
      log.info("Allocations receiving a Restricted case CRN, not included: $crn")
    } else if (userAccess.userExcluded) {
      log.info("Allocations receiving a Excluded case CRN, case included: $crn")
    } else {
      log.info("Allocations receiving a non LAO case CRN, case case included: $crn")
    }
    MDC.remove(LAO)
    MDC.remove(CRN)

    userAccess.takeUnless { it.userRestricted }?.let {
      workforceAllocationsToDeliusApiClient.getUnallocatedEvents(crn)?.let { unallocatedEvents ->
        log.debug("workforce to delius api client: getting unallocated events for crn $crn")
        val activeEvents = unallocatedEvents.activeEvents.associateBy { it.eventNumber.toInt() }
        if (activeEvents.isEmpty()) {
          log.debug("No active events found for crn $crn")
        } else {
          log.debug("Active events found for crn $crn: $activeEvents")
        }
        try {
          val tier = getTier(crn)
          log.debug("hmpps tier api client: got tier for crn: $crn")
          val name = unallocatedEvents.name.getCombinedName()
          databaseService.saveNewEvents(activeEvents, storedUnallocatedEvents, name, crn, tier)
          databaseService.updateExistingEvents(activeEvents, storedUnallocatedEvents, name, tier)
        } catch (e: MissingTierException) {
          log.error("Tier Missing for crn $crn; ${e.message}")
        } finally {
          databaseService.deleteOldEvents(storedUnallocatedEvents, activeEvents)
        }
      }
    } ?: databaseService.deleteOldEvents(storedUnallocatedEvents, emptyMap())
  }

  suspend fun getTier(crn: String): String {
    return hmppsTierApiClient.getTierByCrn(crn = crn) ?: throw MissingTierException("Missing tier: $crn")
  }
}
