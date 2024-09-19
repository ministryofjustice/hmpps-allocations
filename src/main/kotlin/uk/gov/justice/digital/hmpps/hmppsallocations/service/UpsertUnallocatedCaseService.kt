package uk.gov.justice.digital.hmpps.hmppsallocations.service

import jakarta.transaction.Transactional
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppsallocations.client.HmppsTierApiClient
import uk.gov.justice.digital.hmpps.hmppsallocations.client.MissingTierException
import uk.gov.justice.digital.hmpps.hmppsallocations.client.WorkforceAllocationsToDeliusApiClient
import uk.gov.justice.digital.hmpps.hmppsallocations.jpa.repository.UnallocatedCasesRepository

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
    workforceAllocationsToDeliusApiClient.getUserAccess(crn = crn)?.takeUnless { it.userExcluded || it.userRestricted }?.let {
      workforceAllocationsToDeliusApiClient.getUnallocatedEvents(crn)?.let { unallocatedEvents ->
        log.debug("workforce to delius api client: getting unallocated events for crn $crn")
        val activeEvents = unallocatedEvents.activeEvents.associateBy { it.eventNumber.toInt() }
        if (activeEvents.isEmpty()) {
          log.debug("No active events found for crn $crn")
        } else {
          log.debug("Active events found for crn $crn: $activeEvents")
        }
        try {
          hmppsTierApiClient.getTierByCrn(crn)?.let { tier ->
            log.debug("hmpps tier api client: getting tier for crn: $crn")
            val name = unallocatedEvents.name.getCombinedName()
            databaseService.saveNewEvents(activeEvents, storedUnallocatedEvents, name, crn, tier)
            databaseService.updateExistingEvents(activeEvents, storedUnallocatedEvents, name, tier)
          }
        } catch (e: MissingTierException) {
          log.error("Tier Missing for crn $crn; ${e.message}")
        } finally {
          databaseService.deleteOldEvents(storedUnallocatedEvents, activeEvents)
        }
      }
    } ?: databaseService.deleteOldEvents(storedUnallocatedEvents, emptyMap())
  }
}
