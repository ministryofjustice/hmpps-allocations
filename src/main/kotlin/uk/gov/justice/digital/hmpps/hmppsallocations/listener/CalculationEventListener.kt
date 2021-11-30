package uk.gov.justice.digital.hmpps.hmppsallocations.listener

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.ObjectMapper
import org.slf4j.LoggerFactory
import org.springframework.jms.annotation.JmsListener
import org.springframework.messaging.handler.annotation.MessageExceptionHandler
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.hmppsallocations.client.HmppsTierApiClient
import java.io.IOException
import java.util.UUID

@Component
class CalculationEventListener(hmppsTierApiClient: HmppsTierApiClient) {

  @JmsListener(destination = "tiercalculationqueue", containerFactory = "hmppsQueueContainerFactoryProxy")
  fun onRegisterChange(message: String) {
    val calculationEventData: CalculationEventData? = CalculationEventData.fromJSON(message)
  }

  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
    private val objectMapper = ObjectMapper()
  }

  @MessageExceptionHandler()
  fun errorHandler(e: Exception, msg: String) {
    log.warn("Failed to to process Message")
    throw e
  }

  data class CalculationEventData(val crn: String, val calculationId: UUID) {
    companion object {
      @Throws(JsonProcessingException::class, IOException::class)
      fun fromJSON(json: String?): CalculationEventData? {
        val message = objectMapper.readValue(json, Message::class.java)
        return objectMapper.readValue(message.message, CalculationEventData::class.java)
      }
    }
  }

  private data class Message(@JsonProperty("Message") val message: String)
}
