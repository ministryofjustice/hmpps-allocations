package uk.gov.justice.digital.hmpps.hmppsallocations.service

import com.amazonaws.services.sqs.model.SendMessageRequest
import com.fasterxml.jackson.databind.ObjectMapper
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppsallocations.jpa.entity.UnallocatedCaseEntity
import uk.gov.justice.digital.hmpps.hmppsallocations.jpa.repository.UnallocatedCasesRepository
import uk.gov.justice.digital.hmpps.hmppsallocations.listener.HmppsOffenderEvent
import uk.gov.justice.digital.hmpps.hmppsallocations.listener.OffenderEventListener
import uk.gov.justice.digital.hmpps.hmppsallocations.listener.SQSMessage
import uk.gov.justice.hmpps.sqs.HmppsQueueService
import uk.gov.justice.hmpps.sqs.MissingQueueException

@Service
class UploadUnallocatedCasesService(
  private val hmppsQueueService: HmppsQueueService,
  private val objectMapper: ObjectMapper,
  private val unallocatedCasesrepository: UnallocatedCasesRepository,
  private val offenderEventListener: OffenderEventListener,
  // @Qualifier("hmppsoffenderqueue-sqs-client") private val hmppsOffenderSqsClient: AmazonSQSAsync,
) {
  companion object {
    val log: Logger = LoggerFactory.getLogger(this::class.java)
  }

  private val hmppsOffenderQueueUrl by lazy { hmppsQueueService.findByQueueId("hmppsoffenderqueue")?.queueUrl ?: throw MissingQueueException("HmppsQueue hmppsoffenderqueue not found") }

  @Async
  fun sendEvents() {
    val unallocatedCases = unallocatedCasesrepository.findByConvictionNumberIsNull()
    unallocatedCases
      .forEach {
        callListenerDirectly(it)
        // publishToHmppsDomainTopic(it)
      }
  }

  private fun callListenerDirectly(unallocatedCase: UnallocatedCaseEntity) {
    val message = caseToOffenderSqsMessage(unallocatedCase.crn)
    offenderEventListener.processMessage(objectMapper.writeValueAsString(message))
  }

  private fun publishToHmppsDomainTopic(unallocatedCase: UnallocatedCaseEntity) {
    val sendMessage = SendMessageRequest(
      hmppsOffenderQueueUrl,
      objectMapper.writeValueAsString(
        caseToOffenderSqsMessage(unallocatedCase.crn!!)
      )
    ).withMessageAttributes(
      mapOf("eventType" to com.amazonaws.services.sqs.model.MessageAttributeValue().withDataType("String").withStringValue("OFFENDER_DETAILS_CHANGED"))
    )
    log.info("publishing event type {} for crn {}", "OFFENDER_DETAILS_CHANGED", unallocatedCase.crn!!)
    // hmppsOffenderSqsClient.sendMessage(sendMessage)
  }

  private fun caseToOffenderSqsMessage(crn: String): SQSMessage = SQSMessage(
    objectMapper.writeValueAsString(
      HmppsOffenderEvent(crn)
    )
  )
}
