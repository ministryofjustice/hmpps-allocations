package uk.gov.justice.digital.hmpps.hmppsallocations.listener

import com.fasterxml.jackson.databind.ObjectMapper
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.hmppsallocations.client.EventsNotFoundError
import uk.gov.justice.digital.hmpps.hmppsallocations.service.UnallocatedDataBaseOperationService
import uk.gov.justice.digital.hmpps.hmppsallocations.service.UpsertUnallocatedCaseService

class OffenderEventListenerTest {

  @MockK
  lateinit var objectMapper: ObjectMapper

  @MockK
  lateinit var upsertUnallocatedCaseService: UpsertUnallocatedCaseService

  @MockK
  lateinit var unallocatedDataBaseOperationService: UnallocatedDataBaseOperationService

  @InjectMockKs
  lateinit var offenderEventListener: OffenderEventListener

  @BeforeEach
  fun setUp() {
    MockKAnnotations.init(this, relaxUnitFun = true)
  }

  @Test
  fun `process offender event`() {
    val rawMessage = "{\"Message\":\"{\\\"crn\\\":\\\"X123456\\\"}\",\"MessageId\":\"12345678-1234-1234-1234-123456789012\"}"
    val crn = "X123456"
    coEvery { objectMapper.readValue(rawMessage, QueueMessage::class.java) } returns QueueMessage("{\"crn\":\"X123456\"}","12345678-1234-1234-1234-123456789012")
    coEvery { objectMapper.readValue("{\"crn\":\"X123456\"}", HmppsOffenderEvent::class.java) } returns HmppsOffenderEvent("X123456")
    offenderEventListener.processMessage(rawMessage)
    coVerify { upsertUnallocatedCaseService.upsertUnallocatedCase(crn) }
  }
  @Test
  fun `process crn with no events`() {
    val rawMessage = "{\"Message\":\"{\\\"crn\\\":\\\"X123456\\\",\\\"convictionId\\\":123456}\",\"MessageId\":\"12345678-1234-1234-1234-123456789012\"}"
    val crn = "X123456"
    coEvery { objectMapper.readValue(rawMessage, QueueMessage::class.java) } returns QueueMessage("{\"crn\":\"X123456\",\"convictionId\":123456}","12345678-1234-1234-1234-123456789012")
    coEvery { objectMapper.readValue("{\"crn\":\"X123456\",\"convictionId\":123456}", HmppsOffenderEvent::class.java) } returns HmppsOffenderEvent("X123456")
    coEvery { upsertUnallocatedCaseService.upsertUnallocatedCase("X123456") } throws EventsNotFoundError("No events found for X123456")
    offenderEventListener.processMessage(rawMessage)

    coVerify { upsertUnallocatedCaseService.upsertUnallocatedCase(crn)}
    coVerify { unallocatedDataBaseOperationService.deleteEventsForNoActiveEvents(crn) }
  }


}
