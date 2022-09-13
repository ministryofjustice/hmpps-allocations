package uk.gov.justice.digital.hmpps.hmppsallocations.listener

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.ObjectMapper
import org.slf4j.LoggerFactory
import org.springframework.jms.annotation.JmsListener
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.hmppsallocations.client.ForbiddenOffenderError
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
    val crn = getCase(rawMessage)
    try {
      enrichEventService.getAllConvictionIdsAssociatedToCrn(crn)
        .forEach { convictionId ->
          upsertUnallocatedCaseService.upsertUnallocatedCase(crn, convictionId)
        }
    } catch (e: ForbiddenOffenderError) {
      log.warn("Unable to access offender with CRN $crn with error: ${e.message}")
    }
  }

  private fun getCase(rawMessage: String): String {
    val (message) = objectMapper.readValue(rawMessage, SQSMessage::class.java)
    return objectMapper.readValue(message, HmppsOffenderEvent::class.java).crn
  }

  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }
}

data class HmppsOffenderEvent(
  val crn: String
)

data class SQSMessage(
  @JsonProperty("Message") val message: String
)
