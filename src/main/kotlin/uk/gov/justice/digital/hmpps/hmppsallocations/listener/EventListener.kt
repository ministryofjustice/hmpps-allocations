package uk.gov.justice.digital.hmpps.hmppsallocations.listener

import com.fasterxml.jackson.databind.ObjectMapper
import org.slf4j.LoggerFactory
import org.springframework.jms.annotation.JmsListener
import org.springframework.stereotype.Component

@Component
class EventListener(private val objectMapper: ObjectMapper) {

  @JmsListener(destination = "hmppsdomainqueue", containerFactory = "hmppsQueueContainerFactoryProxy")
  fun processMessage(rawMessage: String?) {
    val (Message) = objectMapper.readValue(rawMessage, Message::class.java)
    val event = objectMapper.readValue(Message, HmppsEvent::class.java)
    log.info("event is: {}", event)
  }
  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }
}
data class HmppsEvent(val id: String, val type: String, val contents: String)
data class EventType(val Value: String, val Type: String)
data class MessageAttributes(val eventType: EventType)
data class Message(
  val Message: String,
  val MessageId: String,
  val MessageAttributes: MessageAttributes
)
