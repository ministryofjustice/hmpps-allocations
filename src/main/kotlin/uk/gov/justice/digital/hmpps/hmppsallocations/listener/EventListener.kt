package uk.gov.justice.digital.hmpps.hmppsallocations.listener

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.ObjectMapper
import org.slf4j.LoggerFactory
import org.springframework.jms.annotation.JmsListener
import org.springframework.messaging.handler.annotation.MessageExceptionHandler
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.hmppsallocations.service.UpsertUnallocatedCaseService

@Component
class EventListener(
  private val objectMapper: ObjectMapper,
  private val upsertUnallocatedCaseService: UpsertUnallocatedCaseService
) {

  @JmsListener(destination = "hmppsdomainqueue", containerFactory = "hmppsQueueContainerFactoryProxy")
  fun processMessage(rawMessage: String) {
    val case = getCase(rawMessage)
    log.info("received event for crn: {}", case.crn)
    upsertUnallocatedCaseService.upsertUnallocatedCase(case.crn, case.convictionId)
  }

  @MessageExceptionHandler()
  fun errorHandler(e: Exception, msg: String) {
    log.warn("Failed to create an unallocated case  with CRN ${getCase(msg).crn} with error: ${e.message}")
    throw e
  }

  private fun getCase(rawMessage: String): HmppsUnallocatedCase {
    val (message) = objectMapper.readValue(rawMessage, Message::class.java)
    val event = objectMapper.readValue(message, HmppsEvent::class.java)
    return event.additionalInformation
  }

  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }
}

data class HmppsUnallocatedCase(
  val crn: String,
  val convictionId: Long
)
data class HmppsEvent(val eventType: String, val version: Int, val description: String, val detailUrl: String, val occurredAt: String, val additionalInformation: HmppsUnallocatedCase)

data class Message(
  @JsonProperty("Message") val message: String,

)
