package uk.gov.justice.digital.hmpps.hmppsallocations.listener

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.ObjectMapper
import org.slf4j.LoggerFactory
import org.springframework.jms.annotation.JmsListener
import org.springframework.messaging.handler.annotation.MessageExceptionHandler
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.hmppsallocations.jpa.entity.UnallocatedCaseEntity
import uk.gov.justice.digital.hmpps.hmppsallocations.jpa.repository.UnallocatedCasesRepository
import uk.gov.justice.digital.hmpps.hmppsallocations.service.EnrichEventService

@Component
class EventListener(
  private val repository: UnallocatedCasesRepository,
  private val objectMapper: ObjectMapper,
  private val enrichEventService: EnrichEventService
) {

  @JmsListener(destination = "hmppsdomainqueue", containerFactory = "hmppsQueueContainerFactoryProxy")
  fun processMessage(rawMessage: String) {
    val case = getCase(rawMessage)
    log.info("received event for crn: {}", case.crn)

    enrichEventService.getSentenceDate(case.crn, case.convictionId)?.let { sentenceDate ->
      enrichEventService.getTier(case.crn)?.let { tier ->
        val initialAppointment = enrichEventService.getInitialAppointmentDate(case.crn, sentenceDate)
        val name = enrichEventService.getOffenderName(case.crn)
        val (status, previousConvictionDate, offenderManagerDetails) = enrichEventService.getProbationStatus(case.crn)

        val unallocatedCase = UnallocatedCaseEntity(
          null, name,
          case.crn, tier, sentenceDate, initialAppointment, status.status, previousConvictionDate,
          offenderManagerSurname = offenderManagerDetails?.surname,
          offenderManagerForename = offenderManagerDetails?.forenames,
          offenderManagerGrade = offenderManagerDetails?.grade,
          convictionId = case.convictionId
        )

        repository.save(unallocatedCase)
      }
    }
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
