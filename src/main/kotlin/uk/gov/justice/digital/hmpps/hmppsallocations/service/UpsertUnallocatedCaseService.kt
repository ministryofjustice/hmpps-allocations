package uk.gov.justice.digital.hmpps.hmppsallocations.service

import jakarta.transaction.Transactional
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppsallocations.client.CommunityApiClient
import uk.gov.justice.digital.hmpps.hmppsallocations.client.HmppsTierApiClient
import uk.gov.justice.digital.hmpps.hmppsallocations.client.WorkforceAllocationsToDeliusApiClient
import uk.gov.justice.digital.hmpps.hmppsallocations.client.dto.ActiveEvent
import uk.gov.justice.digital.hmpps.hmppsallocations.jpa.entity.UnallocatedCaseEntity
import uk.gov.justice.digital.hmpps.hmppsallocations.jpa.repository.UnallocatedCasesRepository

@Service
class UpsertUnallocatedCaseService(
  private val repository: UnallocatedCasesRepository,
  @Qualifier("hmppsTierApiClient") private val hmppsTierApiClient: HmppsTierApiClient,
  private val telemetryService: TelemetryService,
  @Qualifier("workforceAllocationsToDeliusApiClient") private val workforceAllocationsToDeliusApiClient: WorkforceAllocationsToDeliusApiClient,
  private val communityApiClient: CommunityApiClient,

) {

  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }

  @Transactional
  suspend fun upsertUnallocatedCase(crn: String) {
    log.debug("upsert unallocated case")
    val storedUnallocatedEvents = repository.findByCrn(crn)
    communityApiClient.getUserAccess(crn)?.takeUnless { it.userExcluded || it.userRestricted }?.let {
      workforceAllocationsToDeliusApiClient.getUnallocatedEvents(crn)?.let { unallocatedEvents ->
        log.debug("workforce to delius api client: getting unallocated events")
        val activeEvents = unallocatedEvents.activeEvents.associateBy { it.eventNumber.toInt() }
        hmppsTierApiClient.getTierByCrn(crn)?.let { tier ->
          log.debug("hmpps tier api client: getting tier by crn")
          val name = unallocatedEvents.name.getCombinedName()
          saveNewEvents(activeEvents, storedUnallocatedEvents, name, crn, tier)
          updateExistingEvents(activeEvents, storedUnallocatedEvents, name, tier)
          deleteOldEvents(storedUnallocatedEvents, activeEvents)
        }
      }
    } ?: deleteOldEvents(storedUnallocatedEvents, emptyMap())
  }

  private fun deleteOldEvents(
    storedUnallocatedEvents: List<UnallocatedCaseEntity>,
    activeEvents: Map<Int, ActiveEvent>,
  ) {
    storedUnallocatedEvents
      .filter { !activeEvents.containsKey(it.convictionNumber) }
      .forEach { deleteEvent ->
        repository.delete(deleteEvent)
        log.debug("Event $deleteEvent deleted")
        telemetryService.trackUnallocatedCaseAllocated(deleteEvent)
      }
  }

  private fun updateExistingEvents(
    activeEvents: Map<Int, ActiveEvent>,
    storedUnallocatedEvents: List<UnallocatedCaseEntity>,
    name: String,
    tier: String,
  ) {
    storedUnallocatedEvents
      .filter { activeEvents.containsKey(it.convictionNumber) }
      .forEach { unallocatedCaseEntity ->
        val activeEvent = activeEvents[unallocatedCaseEntity.convictionNumber]!!
        unallocatedCaseEntity.tier = tier
        unallocatedCaseEntity.name = name
        unallocatedCaseEntity.teamCode = activeEvent.teamCode
        unallocatedCaseEntity.providerCode = activeEvent.providerCode
        log.debug("Updating existing event for crn ${unallocatedCaseEntity.crn}, convictionNumber ${unallocatedCaseEntity.convictionNumber} name $name and teamCode ${activeEvent.teamCode}")
        repository.save(unallocatedCaseEntity)
      }
  }

  private fun saveNewEvents(
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
        log.debug("Saving new event with CRN $crn, teamCode ${createEvent.teamCode}, convictionNumber ${createEvent.eventNumber.toInt()}")
        val savedEntity = repository.save(
          UnallocatedCaseEntity(
            name = name,
            crn = crn,
            tier = tier,
            providerCode = createEvent.providerCode,
            teamCode = createEvent.teamCode,
            convictionNumber = createEvent.eventNumber.toInt(),
          ),
        )
        telemetryService.trackAllocationDemandRaised(savedEntity)
      }
  }
}
