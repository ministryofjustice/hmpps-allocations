package uk.gov.justice.digital.hmpps.hmppsallocations.service

import com.amazonaws.services.sqs.AmazonSQS
import com.amazonaws.services.sqs.model.MessageAttributeValue
import com.amazonaws.services.sqs.model.SendMessageRequest
import com.amazonaws.services.sqs.model.SendMessageResult
import com.fasterxml.jackson.databind.ObjectMapper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppsallocations.listener.HmppsOffenderEvent
import uk.gov.justice.digital.hmpps.hmppsallocations.listener.SQSMessage
import uk.gov.justice.hmpps.sqs.HmppsQueueService
import uk.gov.justice.hmpps.sqs.MissingQueueException

@Service
class TriggerReprocessService(
  private val hmppsQueueService: HmppsQueueService,
  private val objectMapper: ObjectMapper,
  @Qualifier("hmppsoffenderqueue-sqs-client") private val hmppsOffenderSqsClient: AmazonSQS,
) {

  private val hmppsOffenderQueueUrl = hmppsQueueService.findByQueueId("hmppsoffenderqueue")?.queueUrl ?: throw MissingQueueException("HmppsQueue hmppsoffenderqueue not found")

  suspend fun sendEvents(crns: List<String>) {
    CoroutineScope(Dispatchers.IO).launch {
      crns.forEach { crn ->
        publishToHMPPSOffenderQueue(crn)
      }
    }
  }

  private fun publishToHMPPSOffenderQueue(crn: String): SendMessageResult {
    val sendMessage = SendMessageRequest(
      hmppsOffenderQueueUrl,
      objectMapper.writeValueAsString(
        crnToOffenderSqsMessage(crn),
      ),
    ).withMessageAttributes(
      mapOf("eventType" to MessageAttributeValue().withDataType("String").withStringValue("CONVICTION_CHANGED")),
    )
    log.info("publishing event type {} for crn {}", "CONVICTION_CHANGED", crn)
    return hmppsOffenderSqsClient.sendMessage(sendMessage)
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
