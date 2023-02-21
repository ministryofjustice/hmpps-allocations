package uk.gov.justice.digital.hmpps.hmppsallocations.service

import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppsallocations.client.WorkforceAllocationsToDeliusApiClient
import uk.gov.justice.digital.hmpps.hmppsallocations.client.dto.ActiveEvent
import uk.gov.justice.digital.hmpps.hmppsallocations.client.dto.UnallocatedEvents
import uk.gov.justice.digital.hmpps.hmppsallocations.jpa.entity.UnallocatedCaseEntity
import uk.gov.justice.digital.hmpps.hmppsallocations.jpa.repository.UnallocatedCasesRepository
import javax.transaction.Transactional

@Service
class UpsertUnallocatedCaseService(
  private val repository: UnallocatedCasesRepository,
  private val enrichEventService: EnrichEventService,
  private val telemetryService: TelemetryService,
  @Qualifier("workforceAllocationsToDeliusApiClient") private val workforceAllocationsToDeliusApiClient: WorkforceAllocationsToDeliusApiClient,

) {

  @Transactional
  fun upsertUnallocatedCase(crn: String) {
    val storedUnallocatedEvents = repository.findByCrn(crn)
    workforceAllocationsToDeliusApiClient.getUnallocatedEvents(crn).block()?.let { unallocatedEvents ->
      val activeEvents = unallocatedEvents.activeEvents.associateBy { it.eventNumber.toInt() }
      enrichEventService.getTier(crn)?.let { tier ->

        saveNewEvents(activeEvents, storedUnallocatedEvents, unallocatedEvents, tier)
        updateExistingEvents(activeEvents, storedUnallocatedEvents, unallocatedEvents, tier)
        deleteOldEvents(storedUnallocatedEvents, activeEvents)
      }
    } ?: repository.deleteAll(storedUnallocatedEvents)
  }

  private fun deleteOldEvents(
    storedUnallocatedEvents: List<UnallocatedCaseEntity>,
    activeEvents: Map<Int, ActiveEvent>
  ) {
    storedUnallocatedEvents
      .filter { !activeEvents.containsKey(it.convictionNumber) }
      .forEach { deleteEvent ->
        repository.delete(deleteEvent)
        telemetryService.trackUnallocatedCaseAllocated(deleteEvent)
      }
  }

  private fun updateExistingEvents(
    activeEvents: Map<Int, ActiveEvent>,
    storedUnallocatedEvents: List<UnallocatedCaseEntity>,
    unallocatedEvents: UnallocatedEvents,
    tier: String
  ) {
    storedUnallocatedEvents
      .filter { activeEvents.containsKey(it.convictionNumber) }
      .forEach { unallocatedCaseEntity ->
        val activeEvent = activeEvents[unallocatedCaseEntity.convictionNumber]!!
        unallocatedCaseEntity.tier = tier
        unallocatedCaseEntity.name = unallocatedEvents.name.getCombinedName()
        unallocatedCaseEntity.teamCode = activeEvent.teamCode
        unallocatedCaseEntity.providerCode = activeEvent.providerCode
      }
  }

  private fun saveNewEvents(
    activeEvents: Map<Int, ActiveEvent>,
    storedUnallocatedEvents: List<UnallocatedCaseEntity>,
    unallocatedEvents: UnallocatedEvents,
    tier: String
  ) {
    activeEvents
      .filter { activeEvent -> storedUnallocatedEvents.none { entry -> entry.convictionNumber == activeEvent.key } }
      .map { it.value }
      .forEach { createEvent ->
        val savedEntity = repository.save(
          UnallocatedCaseEntity(
            name = unallocatedEvents.name.getCombinedName(),
            crn = unallocatedEvents.crn,
            tier = tier,
            providerCode = createEvent.providerCode,
            teamCode = createEvent.teamCode,
            convictionNumber = createEvent.eventNumber.toInt()
          )
        )
        telemetryService.trackAllocationDemandRaised(savedEntity)
      }
  }

  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }
}
