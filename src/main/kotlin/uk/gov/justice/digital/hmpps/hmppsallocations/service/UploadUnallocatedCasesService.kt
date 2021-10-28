package uk.gov.justice.digital.hmpps.hmppsallocations.service

import com.amazonaws.services.sns.model.MessageAttributeValue
import com.amazonaws.services.sns.model.PublishRequest
import com.fasterxml.jackson.databind.ObjectMapper
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppsallocations.controller.UnallocatedCaseCsv
import uk.gov.justice.digital.hmpps.hmppsallocations.listener.HmppsEvent
import uk.gov.justice.digital.hmpps.hmppsallocations.listener.HmppsUnallocatedCase
import uk.gov.justice.hmpps.sqs.HmppsQueueService
import uk.gov.justice.hmpps.sqs.MissingQueueException
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

@Service
class UploadUnallocatedCasesService(
  private val hmppsQueueService: HmppsQueueService,
  private val objectMapper: ObjectMapper
) {
  companion object {
    val log: Logger = LoggerFactory.getLogger(this::class.java)
  }

  private val hmppsDomainTopic by lazy {
    hmppsQueueService.findByTopicId("hmppsdomaintopic")
      ?: throw MissingQueueException("HmppsTopic hmppsdomaintopic not found")
  }

  fun sendEvents(unallocatedCases: List<UnallocatedCaseCsv>) {
    unallocatedCases.forEach {
      publishToHmppsDomainTopic(it)
    }
  }

  private fun publishToHmppsDomainTopic(unallocatedCase: UnallocatedCaseCsv) {
    val hmppsEvent = HmppsEvent(
      "ALLOCATION_REQUIRED", 0, "Generated Allocated Event", "http://dummy.com",
      ZonedDateTime.now().format(
        DateTimeFormatter.ISO_ZONED_DATE_TIME
      ),
      HmppsUnallocatedCase(
        unallocatedCase.name!!,
        unallocatedCase.crn!!,
        unallocatedCase.tier!!,
        unallocatedCase.sentence_date!!,
        unallocatedCase.initial_appointment,
        unallocatedCase.status!!
      )
    )
    hmppsDomainTopic.snsClient.publish(
      PublishRequest(hmppsDomainTopic.arn, objectMapper.writeValueAsString(hmppsEvent))
        .withMessageAttributes(
          mapOf("eventType" to MessageAttributeValue().withDataType("String").withStringValue(hmppsEvent.eventType))
        )
        .also { log.info("Published event to HMPPS Domain Topic for CRN ${unallocatedCase.crn}") }
    )
  }
}
