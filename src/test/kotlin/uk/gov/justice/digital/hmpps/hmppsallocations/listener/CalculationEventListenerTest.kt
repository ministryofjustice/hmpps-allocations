package uk.gov.justice.digital.hmpps.hmppsallocations.listener

import com.fasterxml.jackson.databind.ObjectMapper
import io.mockk.MockKAnnotations
import io.mockk.coVerify
import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.hmppsallocations.service.TierCalculationService

class CalculationEventListenerTest {

  @MockK
  lateinit var objectMapper: ObjectMapper

  @MockK
  lateinit var tierCalculationService: TierCalculationService

  @InjectMockKs
  lateinit var calculationEventListener: CalculationEventListener

  @BeforeEach
  fun setUp() {
    MockKAnnotations.init(this, relaxUnitFun = true)
  }

  @Test
  fun `process calculation event`() {
    val rawMessage = "{\"Message\":\"{\\\"personReference\\\":{\\\"identifiers\\\":[{\\\"type\\\":\\\"CRN\\\",\\\"value\\\":\\\"X123456\\\"}]}}\",\"MessageId\":\"12345678-1234-1234-1234-123456789012\"}"
    val message = "{\\\"personReference\\\":{\\\"identifiers\\\":[{\\\"type\\\":\\\"CRN\\\",\\\"value\\\":\\\"X123456\\\"}]}}"
    val messageId = "12345678-1234-1234-1234-123456789012"
    val crn = "X123456"
    val personReferenceType = CalculationEventListener.PersonReferenceType("CRN", "X123456")
    val personReference = CalculationEventListener.PersonReference(listOf(personReferenceType))
    val calculationEventData = CalculationEventListener.CalculationEventData(personReference)
    every { objectMapper.readValue(rawMessage, CalculationEventListener.QueueMessage::class.java) } returns CalculationEventListener.QueueMessage(message, messageId)
    every { objectMapper.readValue(message, CalculationEventListener.CalculationEventData::class.java) } returns calculationEventData

    calculationEventListener.processMessage(rawMessage)

    coVerify { tierCalculationService.updateTier(crn) }
  }
}
