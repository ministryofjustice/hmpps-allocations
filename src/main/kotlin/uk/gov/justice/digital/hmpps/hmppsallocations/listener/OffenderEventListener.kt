package uk.gov.justice.digital.hmpps.hmppsallocations.listener

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.ObjectMapper
import io.awspring.cloud.sqs.annotation.SqsListener
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.future.future
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.hmppsallocations.client.ForbiddenOffenderError
import uk.gov.justice.digital.hmpps.hmppsallocations.service.UpsertUnallocatedCaseService
import uk.gov.justice.hmpps.sqs.HmppsQueueService

@Component
class OffenderEventListener(
  private val objectMapper: ObjectMapper,
  private val upsertUnallocatedCaseService: UpsertUnallocatedCaseService,
  private val hmppsQueueService: HmppsQueueService,
) {

  @SqsListener("hmppsoffenderqueue", factory = "hmppsQueueContainerFactoryProxy")
  fun processMessage(rawMessage: String) {
    val crn = getCrn(rawMessage)
    CoroutineScope(Dispatchers.Default).future {
      try {
        upsertUnallocatedCaseService.upsertUnallocatedCase(crn)
      } catch (e: ForbiddenOffenderError) {
        log.warn("Unable to access offender with CRN $crn with error: ${e.message}")
      }
    }.get()
  }

  private fun getCrn(rawMessage: String): String {
    val message = objectMapper.readValue(rawMessage, QueueMessage::class.java)
    val queueName = hmppsQueueService.findByQueueName("hmppsoffenderqueue")?.queueName
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
