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
import uk.gov.justice.digital.hmpps.hmppsallocations.client.EventsNotFoundError
import uk.gov.justice.digital.hmpps.hmppsallocations.client.ForbiddenOffenderError
import uk.gov.justice.digital.hmpps.hmppsallocations.service.UnallocatedDataBaseOperationService
import uk.gov.justice.digital.hmpps.hmppsallocations.service.UpsertUnallocatedCaseService

@Component
class OffenderEventListener(
  private val objectMapper: ObjectMapper,
  private val upsertUnallocatedCaseService: UpsertUnallocatedCaseService,
  private val unallocatedDataBaseOperationService: UnallocatedDataBaseOperationService,
) {

  @SqsListener("hmppsoffenderqueue", factory = "hmppsQueueContainerFactoryProxy")
  fun processMessage(rawMessage: String?) {
    val offenderEvent = readMessage(rawMessage)
    val crn = offenderEvent.crn
    log.debug("Processing message in OffenderEventListener for CRN: $crn")
    CoroutineScope(Dispatchers.Default).future {
      try {
        upsertUnallocatedCaseService.upsertUnallocatedCase(crn)
      } catch (e: ForbiddenOffenderError) {
        log.warn("Unable to access offender with CRN $crn with error: ${e.message}")
      } catch (e: EventsNotFoundError) {
        log.warn("Unable to find events for CRN $crn with error: ${e.message}")
        unallocatedDataBaseOperationService.deleteEventsForNoActiveEvents(crn)
      }
    }.get()
  }

  private fun readMessage(wrapper: String?): HmppsOffenderEvent {
    val message = objectMapper.readValue(wrapper, QueueMessage::class.java)
    val queueName = System.getenv("HMPPS_SQS_QUEUES_HMPPSOFFENDERQUEUE_QUEUE_NAME") ?: "Queue Name Not Found"
    log.info("Received message from SQS queue {} with messageId: {}", queueName, message.messageId)
    return objectMapper.readValue(message.message, HmppsOffenderEvent::class.java)
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
