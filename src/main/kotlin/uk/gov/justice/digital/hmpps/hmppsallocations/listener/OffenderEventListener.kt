package uk.gov.justice.digital.hmpps.hmppsallocations.listener

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.ObjectMapper
import io.awspring.cloud.sqs.annotation.SqsListener
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.hmppsallocations.client.ForbiddenOffenderError
import uk.gov.justice.digital.hmpps.hmppsallocations.service.UpsertUnallocatedCaseService
import java.util.concurrent.ConcurrentHashMap

@Component
class OffenderEventListener(
  private val objectMapper: ObjectMapper,
  private val upsertUnallocatedCaseService: UpsertUnallocatedCaseService,
) {
  @SqsListener("hmppsoffenderqueue", factory = "hmppsQueueContainerFactoryProxy")
  fun processMessage(rawMessage: String) {
    val crn = getCrn(rawMessage)
    log.debug("Processing message in OffenderEventListener for CRN: $crn")
    val lock = lockMap.computeIfAbsent(crn) { Any() }
    synchronized(lock) {
      CoroutineScope(Dispatchers.Default).launch {
        try {
          upsertUnallocatedCaseService.upsertUnallocatedCase(crn)
        } catch (e: ForbiddenOffenderError) {
          log.warn("Unable to access offender with CRN $crn with error: ${e.message}")
        } finally {
          lockMap.remove(crn)
        }
      }
    }
  }

  private fun getCrn(rawMessage: String): String {
    val message = objectMapper.readValue(rawMessage, QueueMessage::class.java)
    val queueName = System.getenv("HMPPS_SQS_QUEUES_HMPPSOFFENDERQUEUE_QUEUE_NAME") ?: "Queue Name Not Found"
    log.info("Received message from SQS queue {} with messageId: {}", queueName, message.messageId)
    return objectMapper.readValue(message.message, HmppsOffenderEvent::class.java).crn
  }

  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
    private val lockMap = ConcurrentHashMap<String, Any>()
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
