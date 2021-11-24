package uk.gov.justice.digital.hmpps.hmppsallocations.integration.unallocatedcases

import com.amazonaws.services.sns.model.MessageAttributeValue
import com.amazonaws.services.sns.model.PublishRequest
import org.assertj.core.api.Assertions.assertThat
import org.awaitility.kotlin.await
import org.awaitility.kotlin.matches
import org.awaitility.kotlin.untilCallTo
import org.junit.jupiter.api.Test
import org.mockserver.matchers.Times
import org.mockserver.model.HttpRequest
import org.mockserver.model.HttpResponse
import org.mockserver.model.MediaType
import uk.gov.justice.digital.hmpps.hmppsallocations.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.hmppsallocations.integration.responses.twoActiveConvictionsResponse
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

class EventListenerTest : IntegrationTestBase() {
  @Test
  fun `retrieve sentenceDate from delius`() {

    singleActiveConvictionResponse("J678910")

    // Given
    val deliusSentenceDate = LocalDate.parse("2019-11-17")
    val firstSentenceDate = LocalDate.now().minusDays(4)
    val firstInitialAppointment = LocalDateTime.now().plusDays(1).truncatedTo(ChronoUnit.SECONDS)
    val unallocatedCase = unallocatedCaseEvent(
      "Dylan Adam Armstrong", "J678910", "C1",
      firstSentenceDate, firstInitialAppointment, "Currently managed"
    )

    // Then
    hmppsDomainSnsClient.publish(
      PublishRequest(hmppsDomainTopicArn, jsonString(unallocatedCase))
        .withMessageAttributes(
          mapOf("eventType" to MessageAttributeValue().withDataType("String").withStringValue(unallocatedCase.eventType))
        )
    )
    await untilCallTo { repository.count() } matches { it!! > 0 }

    val unAllocatedCases = repository.findAll()
    val ucase = unAllocatedCases.iterator().next()

    assertThat(ucase.sentence_date).isEqualTo(deliusSentenceDate)
  }

  @Test
  fun `retrieve sentenceDate for latest conviction from delius`() {

    val convictionsRequest =
      HttpRequest.request().withPath("/secure/offenders/crn/J678910/convictions").withMethod("GET")

    communityApi.`when`(convictionsRequest, Times.exactly(1)).respond(
      HttpResponse.response().withContentType(MediaType.APPLICATION_JSON).withBody(twoActiveConvictionsResponse())
    )

    // Given
    val latestConvictionSentenceDate = LocalDate.parse("2021-11-22")
    val firstSentenceDate = LocalDate.now().minusDays(4)
    val firstInitialAppointment = LocalDateTime.now().plusDays(1).truncatedTo(ChronoUnit.SECONDS)
    val unallocatedCase = unallocatedCaseEvent(
      "Dylan Adam Armstrong", "J678910", "C1",
      firstSentenceDate, firstInitialAppointment, "Currently managed"
    )

    // Then
    hmppsDomainSnsClient.publish(
      PublishRequest(hmppsDomainTopicArn, jsonString(unallocatedCase))
        .withMessageAttributes(
          mapOf("eventType" to MessageAttributeValue().withDataType("String").withStringValue(unallocatedCase.eventType))
        )
    )
    await untilCallTo { repository.count() } matches { it!! > 0 }

    val unAllocatedCases = repository.findAll()
    val ucase = unAllocatedCases.iterator().next()

    assertThat(ucase.sentence_date).isEqualTo(latestConvictionSentenceDate)
  }
}
