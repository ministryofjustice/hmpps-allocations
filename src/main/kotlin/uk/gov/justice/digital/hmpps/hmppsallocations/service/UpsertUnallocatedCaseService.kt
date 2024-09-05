package uk.gov.justice.digital.hmpps.hmppsallocations.service

import jakarta.transaction.Transactional
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppsallocations.client.HmppsTierApiClient
import uk.gov.justice.digital.hmpps.hmppsallocations.client.WorkforceAllocationsToDeliusApiClient
import uk.gov.justice.digital.hmpps.hmppsallocations.client.dto.ActiveEvent
import uk.gov.justice.digital.hmpps.hmppsallocations.jpa.entity.UnallocatedCaseEntity
import uk.gov.justice.digital.hmpps.hmppsallocations.jpa.repository.UnallocatedCasesRepository

@Service
class UnallocatedDataBaseOperationService(
  private val repository: UnallocatedCasesRepository,
  private val telemetryService: TelemetryService,
  @Qualifier("workforceAllocationsToDeliusApiClient") private val workforceAllocationsToDeliusApiClient: WorkforceAllocationsToDeliusApiClient,
) {
  companion object {
    private val logger = LoggerFactory.getLogger(this::class.java)
  }

  @Transactional
  fun saveNewEvents(
    activeEvents: Map<Int, ActiveEvent>,
    storedUnallocatedEvents: List<UnallocatedCaseEntity>,
    name: String,
    crn: String,
    tier: String,
  ) {
    activeEvents
      .filter { activeEvent -> storedUnallocatedEvents.none { entry -> entry.convictionNumber == activeEvent.key } }
      .map { it.value }
      .forEach { createEvent ->
        logger.debug("Saving new event with CRN $crn, teamCode ${createEvent.teamCode}, convictionNumber ${createEvent.eventNumber.toInt()}")
        repository.upsertUnallocatedCase(name, crn, tier, createEvent.teamCode, createEvent.providerCode, Integer.parseInt(createEvent.eventNumber))
        telemetryService.trackAllocationDemandRaised(crn, createEvent.teamCode, createEvent.providerCode)
      }
  }

  @Transactional
  suspend fun deleteOldEvents(
    storedUnallocatedEvents: List<UnallocatedCaseEntity>,
    activeEvents: Map<Int, ActiveEvent>,
  ) {
    storedUnallocatedEvents
      .filter { !activeEvents.containsKey(it.convictionNumber) }
      .forEach { deleteEvent ->
        logger.debug("Deleting event for CRN: ${deleteEvent.crn}, conviction number: ${deleteEvent.convictionNumber}, teamCode: ${deleteEvent.teamCode}")
        repository.delete(deleteEvent)
        logger.debug("Event $deleteEvent deleted")
        val team = workforceAllocationsToDeliusApiClient.getAllocatedTeam(deleteEvent.crn, deleteEvent.convictionNumber)
        telemetryService.trackUnallocatedCaseAllocated(deleteEvent, team?.teamCode)
      }
  }

  @Transactional
  fun updateExistingEvents(
    activeEvents: Map<Int, ActiveEvent>,
    storedUnallocatedEvents: List<UnallocatedCaseEntity>,
    name: String,
    tier: String,
  ) {
    storedUnallocatedEvents
      .filter { activeEvents.containsKey(it.convictionNumber) }
      .forEach { unallocatedCaseEntity ->
        val activeEvent = activeEvents[unallocatedCaseEntity.convictionNumber]!!
        logger.debug("Updating existing event for crn ${unallocatedCaseEntity.crn}, convictionNumber ${unallocatedCaseEntity.convictionNumber}, teamCode ${activeEvent.teamCode}")
        repository.upsertUnallocatedCase(name, unallocatedCaseEntity.crn, tier, activeEvent.teamCode, activeEvent.providerCode, unallocatedCaseEntity.convictionNumber)
      }
  }
}

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
        hmppsTierApiClient.getTierByCrn(crn)?.let { tier ->
          log.debug("hmpps tier api client: getting tier for crn: $crn")
          val name = unallocatedEvents.name.getCombinedName()
          databaseService.saveNewEvents(activeEvents, storedUnallocatedEvents, name, crn, tier)
          databaseService.updateExistingEvents(activeEvents, storedUnallocatedEvents, name, tier)
          databaseService.deleteOldEvents(storedUnallocatedEvents, activeEvents)
        }
      }
    } ?: databaseService.deleteOldEvents(storedUnallocatedEvents, emptyMap())
  }
}
