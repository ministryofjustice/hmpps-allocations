package uk.gov.justice.digital.hmpps.hmppsallocations.service

import com.fasterxml.jackson.databind.ObjectMapper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Service
import software.amazon.awssdk.services.sqs.SqsAsyncClient
import software.amazon.awssdk.services.sqs.model.MessageAttributeValue
import software.amazon.awssdk.services.sqs.model.SendMessageRequest
import uk.gov.justice.digital.hmpps.hmppsallocations.listener.HmppsOffenderEvent
import uk.gov.justice.digital.hmpps.hmppsallocations.listener.SQSMessage
import uk.gov.justice.hmpps.sqs.HmppsQueueService
import uk.gov.justice.hmpps.sqs.MissingQueueException

@Service
class TriggerReprocessService(
  hmppsQueueService: HmppsQueueService,
  private val objectMapper: ObjectMapper,
  @Qualifier("hmppsoffenderqueue-sqs-client") private val hmppsOffenderSqsClient: SqsAsyncClient,
) {

  private val hmppsOffenderQueueUrl = hmppsQueueService.findByQueueId("hmppsoffenderqueue")?.queueUrl ?: throw MissingQueueException("HmppsQueue hmppsoffenderqueue not found")

  suspend fun sendEvents(crns: List<String>) {
    CoroutineScope(Dispatchers.IO).launch {
      crns.forEach { crn ->
        publishToHMPPSOffenderQueue(crn)
      }
    }
  }

  private fun publishToHMPPSOffenderQueue(crn: String) {
    val sendMessage =
      SendMessageRequest.builder()
        .queueUrl(hmppsOffenderQueueUrl)
        .messageBody(
          objectMapper.writeValueAsString(
            crnToOffenderSqsMessage(crn),
          ),
        ).messageAttributes(
          mapOf("eventType" to MessageAttributeValue.builder().dataType("String").stringValue("CONVICTION_CHANGED").build()),
        ).build()
    log.info("publishing event type {} for crn {}", "CONVICTION_CHANGED", crn)
    hmppsOffenderSqsClient.sendMessage(sendMessage)
  }

  private fun crnToOffenderSqsMessage(crn: String): SQSMessage = SQSMessage(
    objectMapper.writeValueAsString(
      HmppsOffenderEvent(crn),
    ),
  )

  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }
}
