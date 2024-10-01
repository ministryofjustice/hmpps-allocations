package uk.gov.justice.digital.hmpps.hmppsallocations.service.auditing

import com.fasterxml.jackson.databind.ObjectMapper
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import software.amazon.awssdk.services.sqs.SqsAsyncClient
import software.amazon.awssdk.services.sqs.model.SendMessageRequest
import software.amazon.awssdk.services.sqs.model.SendMessageResponse
import uk.gov.justice.hmpps.sqs.HmppsQueue
import uk.gov.justice.hmpps.sqs.HmppsQueueService
import java.util.concurrent.CompletableFuture

class AuditServiceTest {
  @MockK
  private lateinit var hmppsQueueService: HmppsQueueService

  @MockK
  private lateinit var hmppsQueue: HmppsQueue

  @MockK
  private lateinit var sqsClient: SqsAsyncClient

  @MockK
  private lateinit var objectMapper: ObjectMapper

  @InjectMockKs
  lateinit var cut: AuditService

  @BeforeEach
  fun setUp() {
    MockKAnnotations.init(this, relaxUnitFun = true)
  }

  @Test
  fun `send audit message correctly`() {
    every { hmppsQueueService.findByQueueId("hmppsauditqueue") } returns hmppsQueue
    every { hmppsQueue.queueUrl } returns "url"
    every { hmppsQueue.sqsClient } returns sqsClient
    val futureResponse = CompletableFuture.completedFuture(SendMessageResponse.builder().build())
    every { objectMapper.writeValueAsString(any()) } returns "{crn:\"0000000\"}"
    every { sqsClient.sendMessage(any<SendMessageRequest>()) } returns futureResponse
    cut.createAndSendAuditMessage(
      AuditObjectForTest("0000000"),
      "testUser",
      "TestOperation",
    )
    verify(exactly = 1) { sqsClient.sendMessage(any<SendMessageRequest>()) }
  }

  private class AuditObjectForTest(crn: String) : AuditObject
}
