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
import uk.gov.justice.digital.hmpps.hmppsallocations.service.TierCalculationService

@Component
class CalculationEventListener(
  private val calculationTierService: TierCalculationService,
  private val objectMapper: ObjectMapper,
) {

  @SqsListener("tiercalculationqueue", factory = "hmppsQueueContainerFactoryProxy")
  fun processMessage(rawMessage: String?) {
    val calculationEventData = readMessage(rawMessage)
    val crn = crnFrom(calculationEventData)
    log.debug("Processing message CRN: $crn in CalculationEventListener")
    CoroutineScope(Dispatchers.Default).future {
      calculationTierService.updateTier(crn)
    }.get()
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
