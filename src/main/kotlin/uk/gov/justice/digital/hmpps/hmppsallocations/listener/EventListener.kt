package uk.gov.justice.digital.hmpps.hmppsallocations.listener

import com.fasterxml.jackson.databind.ObjectMapper
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.jms.annotation.JmsListener
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.hmppsallocations.jpa.entity.UnallocatedCaseEntity
import uk.gov.justice.digital.hmpps.hmppsallocations.jpa.repository.UnallocatedCasesRepository
import java.time.LocalDateTime

@Component
class EventListener(@Autowired val repository: UnallocatedCasesRepository, private val objectMapper: ObjectMapper) {

  @JmsListener(destination = "hmppsdomainqueue", containerFactory = "hmppsQueueContainerFactoryProxy")
  fun processMessage(rawMessage: String?) {
    val (Message) = objectMapper.readValue(rawMessage, Message::class.java)
    val event = objectMapper.readValue(Message, HmppsEvent::class.java)
    log.info("received event for crn: {}", event.additionalInformation.crn)
    val unallocatedCase = UnallocatedCaseEntity(null, event.additionalInformation.name, event.additionalInformation.crn, event.additionalInformation.tier, event.additionalInformation.sentence_date, event.additionalInformation.initial_appointment, event.additionalInformation.status)
    repository.save(unallocatedCase)
  }
  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }
}

data class HmppsUnallocatedCase(val name: String, val crn: String, val tier: String, val sentence_date: LocalDateTime, val initial_appointment: LocalDateTime?, val status: String)
data class HmppsEvent(val eventType: String, val version: Int, val description: String, val detailUrl: String, val occurredAt: String, val additionalInformation: HmppsUnallocatedCase)
data class EventType(val Value: String, val Type: String)
data class MessageAttributes(val eventType: EventType)
data class Message(
  val Message: String,
  val MessageId: String,
  val MessageAttributes: MessageAttributes
)
