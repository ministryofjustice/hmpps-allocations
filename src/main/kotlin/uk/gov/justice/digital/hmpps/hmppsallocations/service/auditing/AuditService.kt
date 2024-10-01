package uk.gov.justice.digital.hmpps.hmppsallocations.service.auditing

import com.fasterxml.jackson.databind.ObjectMapper
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import software.amazon.awssdk.services.sqs.model.SendMessageRequest
import uk.gov.justice.hmpps.sqs.HmppsQueueService
import uk.gov.justice.hmpps.sqs.MissingQueueException
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.UUID

@Service
class AuditService(
  private val hmppsQueueService: HmppsQueueService,
  private val objectMapper: ObjectMapper,
) {
  private val hmppsAuditQueue by lazy { hmppsQueueService.findByQueueId("hmppsauditqueue") ?: throw MissingQueueException("HmppsQueue hmppsauditqueue not found") }

  companion object {
    val log: Logger = LoggerFactory.getLogger(this::class.java)
  }

  fun createAndSendAuditMessage(
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
              UUID.randomUUID().toString(),
              operation,
              LocalDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME),
              loggedInUser,
              "hmpps-allocations",
              objectMapper.writeValueAsString(auditObject),
            ),
          ),
        )
        .build()
      hmppsAuditQueue.sqsClient.sendMessage(sendMessage)
    } catch (e: MissingQueueException) {
      log.error("Queue missing, can't find queue {}", e.message)
    }
  }

  fun sendAuditMessage(operationId: String, what: String, who: String, service: String, details: String) {
    try {
      val sendMessage = SendMessageRequest.builder()
        .queueUrl(hmppsAuditQueue.queueUrl)
        .messageBody(
          objectMapper.writeValueAsString(
            AuditMessage(
              operationId,
              what,
              LocalDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME),
              who,
              service,
              objectMapper.writeValueAsString(details),
            ),
          ),
        )
        .build()
      hmppsAuditQueue.sqsClient.sendMessage(sendMessage)
    } catch (e: MissingQueueException) {
      log.error("Queue missing, can't find queue {}", e.message)
    }
  }
}
