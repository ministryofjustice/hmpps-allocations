package uk.gov.justice.digital.hmpps.hmppsallocations.listener

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.ObjectMapper
import org.slf4j.LoggerFactory
import org.springframework.jms.annotation.JmsListener
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.hmppsallocations.service.TierCalculationService
import java.time.Duration
import java.time.LocalDateTime

@Component
class CalculationEventListener(
  private val calculationTierService: TierCalculationService,
  private val objectMapper: ObjectMapper
) {

  @JmsListener(destination = "tiercalculationqueue", containerFactory = "hmppsQueueContainerFactoryProxy")
  fun processMessage(rawMessage: String?) {
    val calculationEventData = readMessage(rawMessage)
    calculationTierService.updateTier(crnFrom(calculationEventData))
  }

  private fun readMessage(wrapper: String?): CalculationEventData {
    val (message) = objectMapper.readValue(wrapper, Message::class.java)
    val eventData = objectMapper.readValue(message, CalculationEventData::class.java)
    eventData.occurredAt.let {
      val timeToDeliverMs = Duration.between(it, LocalDateTime.now())
      if (timeToDeliverMs.seconds > 1000) {
        log.warn("Delayed TierCalculationEvent delivery (${timeToDeliverMs.seconds}): Crn ${crnFrom(eventData)}")
      }
    }
    return eventData
  }

  private fun crnFrom(calculationEventData: CalculationEventData) =
    calculationEventData.personReference.identifiers.first { it.type == "CRN" }.value

  data class CalculationEventData(
    val personReference: PersonReference,
    val occurredAt: LocalDateTime
  )

  data class PersonReference(val identifiers: List<PersonReferenceType>)

  data class PersonReferenceType(
    val type: String,
    val value: String
  )

  data class Message(@JsonProperty("Message") val message: String)

  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }
}
