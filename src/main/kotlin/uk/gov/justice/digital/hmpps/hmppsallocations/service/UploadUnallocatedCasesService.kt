package uk.gov.justice.digital.hmpps.hmppsallocations.service

import com.amazonaws.services.sns.model.MessageAttributeValue
import com.amazonaws.services.sns.model.PublishRequest
import com.fasterxml.jackson.databind.ObjectMapper
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono
import uk.gov.justice.digital.hmpps.hmppsallocations.client.CommunityApiClient
import uk.gov.justice.digital.hmpps.hmppsallocations.controller.UnallocatedCaseCsv
import uk.gov.justice.digital.hmpps.hmppsallocations.jpa.repository.UnallocatedCasesRepository
import uk.gov.justice.digital.hmpps.hmppsallocations.listener.HmppsEvent
import uk.gov.justice.digital.hmpps.hmppsallocations.listener.HmppsUnallocatedCase
import uk.gov.justice.hmpps.sqs.HmppsQueueService
import uk.gov.justice.hmpps.sqs.MissingQueueException
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter.ISO_ZONED_DATE_TIME

@Service
class UploadUnallocatedCasesService(
  private val hmppsQueueService: HmppsQueueService,
  private val objectMapper: ObjectMapper,
  private val unallocatedCasesRepository: UnallocatedCasesRepository,
  @Qualifier("communityApiClient") private val communityApiClient: CommunityApiClient
) {
  companion object {
    val log: Logger = LoggerFactory.getLogger(this::class.java)
  }

  private val hmppsDomainTopic by lazy {
    hmppsQueueService.findByTopicId("hmppsdomaintopic")
      ?: throw MissingQueueException("HmppsTopic hmppsdomaintopic not found")
  }

  @Async
  fun sendEvents(unallocatedCases: List<UnallocatedCaseCsv>) {
    unallocatedCasesRepository.deleteAll()
    unallocatedCases
      .map { publishToHmppsDomainTopic(it) }
      .forEach {
        it
          .onErrorResume {
            Mono.empty()
          }
          .block()
      }
  }

  private fun publishToHmppsDomainTopic(unallocatedCase: UnallocatedCaseCsv): Mono<Any> {
    log.info("Processing CRN {}", unallocatedCase.crn)
    return communityApiClient.getAllConvictions(unallocatedCase.crn!!)
      .map { convictions ->
        convictions
          .map { conviction ->
            val hmppsEvent = HmppsEvent(
              "ALLOCATION_REQUIRED", 0, "Generated Allocated Event", "http://dummy.com",
              ZonedDateTime.now().format(
                ISO_ZONED_DATE_TIME
              ),
              HmppsUnallocatedCase(
                unallocatedCase.crn!!,
                conviction.convictionId
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
  }
}
