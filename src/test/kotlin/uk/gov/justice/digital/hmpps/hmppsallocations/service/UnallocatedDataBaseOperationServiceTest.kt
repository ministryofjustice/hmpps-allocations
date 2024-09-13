package uk.gov.justice.digital.hmpps.hmppsallocations.service

import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.just
import io.mockk.runs
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.hmppsallocations.client.AllocatedEvent
import uk.gov.justice.digital.hmpps.hmppsallocations.client.WorkforceAllocationsToDeliusApiClient
import uk.gov.justice.digital.hmpps.hmppsallocations.client.dto.ActiveEvent
import uk.gov.justice.digital.hmpps.hmppsallocations.jpa.entity.UnallocatedCaseEntity
import uk.gov.justice.digital.hmpps.hmppsallocations.jpa.repository.UnallocatedCasesRepository
import java.time.ZonedDateTime

class UnallocatedDataBaseOperationServiceTest {
  val storedUnallocatedEvents = listOf(
    UnallocatedCaseEntity(1L, "Bob Jones", "J778881", "C2", "N54ERT", "PC001", ZonedDateTime.now(), 1),
    UnallocatedCaseEntity(2L, "Bob Jones", "J778881", "C2", "N54ERT", "PC001", ZonedDateTime.now(), 2),
  )
  val storedUnallocatedEventsSameConNumber = listOf(
    UnallocatedCaseEntity(1L, "Bob Jones", "J778881", "C2", "N54ERT", "PC001", ZonedDateTime.now(), 1),
    UnallocatedCaseEntity(2L, "Bob Jones", "J778881", "C2", "N54ERT", "PC001", ZonedDateTime.now(), 1),
  )
  val storedUnallocatedEventsForSave = listOf(
    UnallocatedCaseEntity(1L, "Bob Jones", "J778881", "C2", "N54ERT", "PC001", ZonedDateTime.now(), 3),
    UnallocatedCaseEntity(2L, "Bob Jones", "J778881", "C2", "N54ERT", "PC001", ZonedDateTime.now(), 4),
    UnallocatedCaseEntity(3L, "Bob Jones", "J778881", "C2", "N54ERT", "PC001", ZonedDateTime.now(), 5),
  )
  val storedUnallocatedEventsForUpdate = listOf(
    UnallocatedCaseEntity(1L, "Bob Jones", "J778881", "C1", "N54ERT", "PC001", ZonedDateTime.now(), 1),
    UnallocatedCaseEntity(2L, "Bob Jones", "J778881", "C2", "N54ERT", "PC001", ZonedDateTime.now(), 2),
  )
  val activeEvents = hashMapOf(Pair(1, ActiveEvent("1", "N54ERT", "PC001")))

  @MockK
  lateinit var repository: UnallocatedCasesRepository

  @MockK
  lateinit var telemetryService: TelemetryService

  @MockK
  lateinit var workforceAllocationsToDeliusApiClient: WorkforceAllocationsToDeliusApiClient

  @InjectMockKs
  lateinit var cut: UnallocatedDataBaseOperationService

  @BeforeEach
  fun setUp() {
    MockKAnnotations.init(this, relaxUnitFun = true)
  }

  @Test
  fun `delete the correct event`() = runTest {
    val unallocatedCaseEntity = storedUnallocatedEvents.get(0)
    coEvery { workforceAllocationsToDeliusApiClient.getAllocatedTeam(any(), any()) } returns AllocatedEvent(unallocatedCaseEntity.teamCode)
    cut.deleteOldEvents(storedUnallocatedEvents, activeEvents)
    verify(exactly = 1) { repository.delete(storedUnallocatedEvents.get(1)) }
    verify(exactly = 1) { telemetryService.trackUnallocatedCaseAllocated(storedUnallocatedEvents.get(1), any()) }
  }

  @Test
  fun `conviction number the same - dont delete`() = runTest {
    val unallocatedCaseEntity = storedUnallocatedEventsSameConNumber.get(0)
    coEvery { workforceAllocationsToDeliusApiClient.getAllocatedTeam(any(), any()) } returns AllocatedEvent(unallocatedCaseEntity.teamCode)
    cut.deleteOldEvents(storedUnallocatedEventsSameConNumber, activeEvents)
    verify(exactly = 0) { telemetryService.trackUnallocatedCaseAllocated(any(), any()) }
  }

  @Test
  fun `will save a new event`() = runTest {
    val uae = storedUnallocatedEvents.get(1)
    coEvery { repository.upsertUnallocatedCase(any(), any(), any(), any(), any(), any()) } just runs
    cut.saveNewEvents(activeEvents, storedUnallocatedEventsForSave, uae.name, uae.crn, uae.teamCode)
    verify(exactly = 1) { telemetryService.trackAllocationDemandRaised(any(), any(), any()) }
  }

  @Test
  fun `wont save if the event isnt eligible`() = runTest {
    val uae = storedUnallocatedEvents.get(1)
    coEvery { repository.upsertUnallocatedCase(any(), any(), any(), any(), any(), any()) } just runs
    cut.saveNewEvents(activeEvents, storedUnallocatedEvents, uae.name, uae.crn, uae.teamCode)
    verify(exactly = 0) { telemetryService.trackAllocationDemandRaised(any(), any(), any()) }
  }

  @Test
  fun `update event if tier is different`() {
    val uae = storedUnallocatedEventsForUpdate.get(1)
    coEvery { repository.upsertUnallocatedCase(any(), any(), any(), any(), any(), any()) } just runs
    cut.updateExistingEvents(activeEvents, storedUnallocatedEventsForUpdate, uae.name, uae.tier)
    verify(exactly = 1) { repository.upsertUnallocatedCase(any(), any(), any(), any(), any(), any()) }
  }
}
