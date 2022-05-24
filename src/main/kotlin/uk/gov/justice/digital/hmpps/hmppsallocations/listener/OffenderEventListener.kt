package uk.gov.justice.digital.hmpps.hmppsallocations.listener

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.ObjectMapper
import org.slf4j.LoggerFactory
import org.springframework.jms.annotation.JmsListener
import org.springframework.messaging.handler.annotation.MessageExceptionHandler
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.hmppsallocations.service.EnrichEventService
import uk.gov.justice.digital.hmpps.hmppsallocations.service.UpsertUnallocatedCaseService

@Component
class OffenderEventListener(
  private val objectMapper: ObjectMapper,
  private val upsertUnallocatedCaseService: UpsertUnallocatedCaseService,
  private val enrichEventService: EnrichEventService
) {

  @JmsListener(destination = "hmppsoffenderqueue", containerFactory = "hmppsQueueContainerFactoryProxy")
  fun processMessage(rawMessage: String) {
    val case = getCase(rawMessage)
    log.info("received event for crn: {}", case.crn)
    enrichEventService.getAllConvictionIdsAssociatedToCrn(case.crn)
      .forEach { convictionId ->
        upsertUnallocatedCaseService.upsertUnallocatedCase(case.crn, convictionId)
      }
  }

  @MessageExceptionHandler()
  fun errorHandler(e: Exception, msg: String) {
    log.warn("Failed to create an unallocated case  with CRN ${getCase(msg).crn} with error: ${e.message}")
    throw e
  }

  private fun getCase(rawMessage: String): HmppsOffenderEvent {
    val (message) = objectMapper.readValue(rawMessage, Message::class.java)
    return objectMapper.readValue(message, HmppsOffenderEvent::class.java)
  }

  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }
}

data class HmppsOffenderEvent(
  val crn: String
)

data class SQSMessage(
  @JsonProperty("Message") val message: String,

)
