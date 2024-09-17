package uk.gov.justice.digital.hmpps.hmppsallocations.listener

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.ObjectMapper
import io.awspring.cloud.sqs.annotation.SqsListener
import io.awspring.cloud.sqs.listener.ListenerExecutionFailedException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.future.future
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import software.amazon.awssdk.services.sqs.model.SendMessageRequest
import uk.gov.justice.digital.hmpps.hmppsallocations.client.EventsNotFoundError
import uk.gov.justice.digital.hmpps.hmppsallocations.client.ForbiddenOffenderError
import uk.gov.justice.digital.hmpps.hmppsallocations.service.UnallocatedDataBaseOperationService
import uk.gov.justice.digital.hmpps.hmppsallocations.service.UpsertUnallocatedCaseService
import uk.gov.justice.hmpps.sqs.HmppsQueueService

@Component
class OffenderEventListener(
  private val objectMapper: ObjectMapper,
  private val upsertUnallocatedCaseService: UpsertUnallocatedCaseService,
  private val hmppsQueueService: HmppsQueueService,
  private val unallocatedDataBaseOperationService: UnallocatedDataBaseOperationService,
) {

  @SqsListener("hmppsoffenderqueue", factory = "hmppsQueueContainerFactoryProxy")
  fun processMessage(rawMessage: String) {
    try {
      val crn = getCrn(rawMessage)
      log.debug("Processing message in OffenderEventListener for CRN: $crn")
      CoroutineScope(Dispatchers.Default).future {
        try {
          upsertUnallocatedCaseService.upsertUnallocatedCase(crn)
        } catch (e: ForbiddenOffenderError) {
          log.warn("Unable to access offender with CRN $crn with error: ${e.message}")
        }
      }.get()
    } catch (e: ListenerExecutionFailedException) {
      sendToDlq(rawMessage)
      log.error("Problem handling message, putting on dlq; $rawMessage")
    }
  }

  private fun sendToDlq(rawMessage: String) {
    val dlqName = System.getenv("HMPPS_SQS_QUEUES_HMPPSOFFENDERQUEUE_DLQ_NAME") ?: "Queue Name Not Found"
    val dlqQueue = hmppsQueueService.findByDlqName(dlqName)!!
    val message = objectMapper.readValue(rawMessage, QueueMessage::class.java)
    val request = SendMessageRequest.builder().queueUrl(dlqName).messageBody(message.message).build()
    dlqQueue.sqsDlqClient?.sendMessage(request)
  }

  private fun getCrn(rawMessage: String): String {
    val message = objectMapper.readValue(rawMessage, QueueMessage::class.java)
    val queueName = System.getenv("HMPPS_SQS_QUEUES_HMPPSOFFENDERQUEUE_QUEUE_NAME") ?: "Queue Name Not Found"
    log.info("Received message from SQS queue {} with messageId: {}", queueName, message.messageId)
    return objectMapper.readValue(message.message, HmppsOffenderEvent::class.java).crn
  }

  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }
}

data class HmppsOffenderEvent(
  val crn: String,
)

@JsonInclude(JsonInclude.Include.NON_NULL)
data class QueueMessage(
  @JsonProperty("Message") val message: String,
  @JsonProperty("MessageId") val messageId: String?,
)

data class SQSMessage(
  @JsonProperty("Message") val message: String,
)
