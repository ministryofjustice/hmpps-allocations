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
import uk.gov.justice.hmpps.sqs.HmppsQueueService

@Component
class CalculationEventListener(
  private val calculationTierService: TierCalculationService,
  private val objectMapper: ObjectMapper,
  private val hmppsQueueService: HmppsQueueService,
) {

  @SqsListener("tiercalculationqueue", factory = "hmppsQueueContainerFactoryProxy")
  fun processMessage(rawMessage: String?) {
    val calculationEventData = readMessage(rawMessage)
    CoroutineScope(Dispatchers.Default).future {
      calculationTierService.updateTier(crnFrom(calculationEventData))
    }.get()
  }

  private fun readMessage(wrapper: String?): CalculationEventData {
    val message = objectMapper.readValue(wrapper, QueueMessage::class.java)
    val queueName = hmppsQueueService.findByQueueName("tiercalculationqueue")?.queueName
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

  // data class Message(@JsonProperty("Message") val message: String)
}
