package uk.gov.justice.digital.hmpps.hmppsallocations.listener

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.ObjectMapper
import org.slf4j.LoggerFactory
import org.springframework.jms.annotation.JmsListener
import org.springframework.messaging.handler.annotation.MessageExceptionHandler
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.hmppsallocations.service.TierCalculationService
import java.util.UUID

@Component
class CalculationEventListener(
  private val calculationTierService: TierCalculationService,
  private val objectMapper: ObjectMapper
) {

  @JmsListener(destination = "tiercalculationqueue", containerFactory = "hmppsQueueContainerFactoryProxy")
  fun processMessage(rawMessage: String?) {
    val (message) = objectMapper.readValue(rawMessage, Message::class.java)
    val event = objectMapper.readValue(message, CalculationEvent::class.java)

    calculationTierService.updateTier(event.additionalInformation.crn)
    log.info("Tier calculation update consumed successfully for crn: ${event.additionalInformation.crn}")
  }

  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }

  @MessageExceptionHandler()
  fun errorHandler(e: Exception, msg: String) {
    log.warn("Failed to to process Message")
    throw e
  }

  data class CalculationEventData(val crn: String, val calculationId: UUID)

  data class CalculationEvent(val eventType: String, val version: Int, val description: String, val detailUrl: String, val occurredAt: String, val additionalInformation: CalculationEventData)

  data class Message(@JsonProperty("Message") val message: String)
}
