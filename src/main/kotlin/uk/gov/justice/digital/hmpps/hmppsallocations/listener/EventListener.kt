package uk.gov.justice.digital.hmpps.hmppsallocations.listener

import com.fasterxml.jackson.databind.ObjectMapper
import org.slf4j.LoggerFactory
import org.springframework.jms.annotation.JmsListener
import org.springframework.messaging.handler.annotation.MessageExceptionHandler
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.hmppsallocations.jpa.entity.UnallocatedCaseEntity
import uk.gov.justice.digital.hmpps.hmppsallocations.jpa.repository.UnallocatedCasesRepository
import uk.gov.justice.digital.hmpps.hmppsallocations.service.UnallocatedCasesService

@Component
class EventListener(
  private val repository: UnallocatedCasesRepository,
  private val objectMapper: ObjectMapper,
  private val unallocatedCasesService: UnallocatedCasesService
) {

  @JmsListener(destination = "hmppsdomainqueue", containerFactory = "hmppsQueueContainerFactoryProxy")
  fun processMessage(rawMessage: String?) {
    val (Message) = objectMapper.readValue(rawMessage, Message::class.java)
    val event = objectMapper.readValue(Message, HmppsEvent::class.java)
    val crn = event.additionalInformation.crn
    log.info("received event for crn: {}", crn)
    val sentenceDate = unallocatedCasesService.getSentenceDate(crn)
    val initialAppointment = unallocatedCasesService.getInitialAppointmentDate(crn, sentenceDate)
    val name = unallocatedCasesService.getOffenderName(crn)
    val tier = unallocatedCasesService.getTier(crn)
    val status = unallocatedCasesService.getProbationStatus(crn)
    val unallocatedCase = UnallocatedCaseEntity(
      null, name,
      crn, tier, sentenceDate, initialAppointment, status.status, status.previousConvictionDate
    )

    repository.save(unallocatedCase)
  }

  @MessageExceptionHandler()
  fun errorHandler(e: Exception, msg: String) {
    log.warn("Failed to to process Message")
    throw e
  }

  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }
}

data class HmppsUnallocatedCase(
  val crn: String
)
data class HmppsEvent(val eventType: String, val version: Int, val description: String, val detailUrl: String, val occurredAt: String, val additionalInformation: HmppsUnallocatedCase)
data class EventType(val Value: String, val Type: String)
data class MessageAttributes(val eventType: EventType)
data class Message(
  val Message: String,
  val MessageId: String,
  val MessageAttributes: MessageAttributes
)
