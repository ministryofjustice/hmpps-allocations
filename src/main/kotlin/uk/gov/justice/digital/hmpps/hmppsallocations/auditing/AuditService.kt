package uk.gov.justice.digital.hmpps.hmppsallocations.auditing

import com.fasterxml.jackson.databind.ObjectMapper
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import software.amazon.awssdk.services.sqs.model.SendMessageRequest
import uk.gov.justice.hmpps.sqs.HmppsQueueService
import uk.gov.justice.hmpps.sqs.MissingQueueException
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@Service
class AuditService(
  private val hmppsQueueService: HmppsQueueService,
  private val objectMapper: ObjectMapper,
) {
  private val hmppsAuditQueue by lazy { hmppsQueueService.findByQueueId("hmppsauditqueue") ?: throw MissingQueueException("HmppsQueue hmppsauditqueue not found") }

  companion object {
    val log: Logger = LoggerFactory.getLogger(this::class.java)
  }

  fun sendAuditMessage(
    auditObject: AuditObject,
    loggedInUser: String,
    crn: String,
    operation: String,
  ) {
    try {
      val sendMessage = SendMessageRequest.builder()
        .queueUrl(hmppsAuditQueue.queueUrl)
        .messageBody(
          objectMapper.writeValueAsString(
            AuditMessage(
              crn,
              loggedInUser,
              auditObject,
              operation,
              LocalDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME),
            ),
          ),
        )
        .build()
      hmppsAuditQueue.sqsClient.sendMessage(sendMessage)
    } catch (e: MissingQueueException) {
      log.error("Queue missing, can't find queue {}", e.message)
    }
  }

  private class AuditMessage(crn: String, loggedInUser: String, auditObject: AuditObject, operation: String, operationDateTime: String)
}
