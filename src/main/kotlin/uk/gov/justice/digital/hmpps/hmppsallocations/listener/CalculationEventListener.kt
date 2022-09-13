package uk.gov.justice.digital.hmpps.hmppsallocations.listener

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.jms.annotation.JmsListener
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
    val fullMessage = objectMapper.readValue(rawMessage, Message::class.java)
    val (crn) = objectMapper.readValue(fullMessage.message, CalculationEventData::class.java)
    calculationTierService.updateTier(crn)
  }

  data class CalculationEventData(val crn: String, val calculationId: UUID)

  data class Message(@JsonProperty("Message") val message: String)
}
