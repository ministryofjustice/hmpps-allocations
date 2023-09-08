package uk.gov.justice.digital.hmpps.hmppsallocations.listener

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.ObjectMapper
import io.awspring.cloud.sqs.annotation.SqsListener
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.future.future
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.hmppsallocations.client.ForbiddenOffenderError
import uk.gov.justice.digital.hmpps.hmppsallocations.service.UpsertUnallocatedCaseService

@Component
class OffenderEventListener(
  private val objectMapper: ObjectMapper,
  private val upsertUnallocatedCaseService: UpsertUnallocatedCaseService,
) {

  @SqsListener("hmppsoffenderqueue", factory = "hmppsQueueContainerFactoryProxy")
  fun processMessage(rawMessage: String) {
    log.debug("processing message")
    val crn = getCrn(rawMessage)
    log.debug("Retrieved message CRN: $crn")
    CoroutineScope(Dispatchers.Default).future {
      try {
        upsertUnallocatedCaseService.upsertUnallocatedCase(crn)
      } catch (e: ForbiddenOffenderError) {
        log.warn("Unable to access offender with CRN $crn with error: ${e.message}")
      }
    }.get()
  }

  private fun getCrn(rawMessage: String): String {
    val (message) = objectMapper.readValue(rawMessage, SQSMessage::class.java)
    return objectMapper.readValue(message, HmppsOffenderEvent::class.java).crn
  }

  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }
}

data class HmppsOffenderEvent(
  val crn: String,
)

data class SQSMessage(
  @JsonProperty("Message") val message: String,
)
