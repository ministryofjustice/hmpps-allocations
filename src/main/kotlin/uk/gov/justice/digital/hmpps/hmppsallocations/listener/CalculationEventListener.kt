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
import uk.gov.justice.digital.hmpps.hmppsallocations.service.TierCalculationService
import uk.gov.justice.hmpps.sqs.HmppsQueueService

@Component
class CalculationEventListener(
  private val calculationTierService: TierCalculationService,
  private val objectMapper: ObjectMapper,
  private val hmppsQueueService: HmppsQueueService,
) {

  @SqsListener("tiercalculationqueue", factory = "hmppsQueueContainerFactoryProxy")
  fun processMessage(rawMessage: String?) {
    try {
      val calculationEventData = readMessage(rawMessage)
      val crn = crnFrom(calculationEventData)
      log.debug("Processing message in CalculationEventListener for CRN: $crn")
      CoroutineScope(Dispatchers.Default).future {
        calculationTierService.updateTier(crn)
      }.get()
    } catch (e: ListenerExecutionFailedException) {
      log.error("Problem handling message, putting on dlq; $rawMessage")
      sendToDlq(rawMessage!!)
    }
  }

  private fun sendToDlq(rawMessage: String) {
    val dlqName = System.getenv("HMPPS_SQS_QUEUES_HMPPSOFFENDERQUEUE_DLQ_NAME") ?: "Queue Name Not Found"
    val dlqQueue = hmppsQueueService.findByDlqName(dlqName)!!
    val request = SendMessageRequest.builder().queueUrl(dlqName).messageBody(rawMessage).build()
    dlqQueue.sqsDlqClient?.sendMessage(request)
  }

  private fun readMessage(wrapper: String?): CalculationEventData {
    val message = objectMapper.readValue(wrapper, QueueMessage::class.java)
    val queueName = System.getenv("HMPPS_SQS_QUEUES_TIERCALCULATIONQUEUE_QUEUE_NAME") ?: "Queue Name Not Found"
    log.info("Received message from SQS queue {} with messageId:{}", queueName, message.messageId)
    return objectMapper.readValue(message.message, CalculationEventData::class.java)
  }

  private fun crnFrom(calculationEventData: CalculationEventData) =
    calculationEventData.personReference.identifiers.first { it.type == "CRN" }.value

  data class CalculationEventData(val personReference: PersonReference)

  data class PersonReference(val identifiers: List<PersonReferenceType>)

  data class PersonReferenceType(
    val type: String,
    val value: String,
  )
  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }

  @JsonInclude(JsonInclude.Include.NON_NULL)
  data class QueueMessage(@JsonProperty("Message") val message: String, @JsonProperty("MessageId") val messageId: String?)
}
