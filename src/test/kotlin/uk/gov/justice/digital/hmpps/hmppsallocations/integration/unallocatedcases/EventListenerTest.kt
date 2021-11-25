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

class EventListenerTest : IntegrationTestBase() {
  @Test
  fun `retrieve sentenceDate from delius`() {
    val crn = "J678910"
    singleActiveConvictionResponse(crn)
    noActiveInductionResponse(crn)
    // Given
    val deliusSentenceDate = LocalDate.parse("2019-11-17")
    val unallocatedCase = unallocatedCaseEvent(
      "Dylan Adam Armstrong", crn, "C1",
      "Currently managed"
    )

    // Then
    hmppsDomainSnsClient.publish(
      PublishRequest(hmppsDomainTopicArn, jsonString(unallocatedCase))
        .withMessageAttributes(
          mapOf("eventType" to MessageAttributeValue().withDataType("String").withStringValue(unallocatedCase.eventType))
        )
    )
    await untilCallTo { repository.count() } matches { it!! > 0 }

    val case = repository.findAll().first()

    assertThat(case.sentence_date).isEqualTo(deliusSentenceDate)
  }

  @Test
  fun `retrieve sentenceDate for latest conviction from delius`() {
    val crn = "J678910"
    val convictionsRequest =
      HttpRequest.request().withPath("/secure/offenders/crn/$crn/convictions").withMethod("GET")

    communityApi.`when`(convictionsRequest, Times.exactly(1)).respond(
      HttpResponse.response().withContentType(MediaType.APPLICATION_JSON).withBody(twoActiveConvictionsResponse())
    )
    noActiveInductionResponse(crn)

    // Given
    val latestConvictionSentenceDate = LocalDate.parse("2021-11-22")
    val unallocatedCase = unallocatedCaseEvent(
      "Dylan Adam Armstrong", crn, "C1",
      "Currently managed"
    )

    // Then
    hmppsDomainSnsClient.publish(
      PublishRequest(hmppsDomainTopicArn, jsonString(unallocatedCase))
        .withMessageAttributes(
          mapOf(
            "eventType" to MessageAttributeValue().withDataType("String").withStringValue(unallocatedCase.eventType)
          )
        )
    )
    await untilCallTo { repository.count() } matches { it!! > 0 }

    val case = repository.findAll().first()

    assertThat(case.sentence_date).isEqualTo(latestConvictionSentenceDate)
  }

  @Test
  fun `retrieve initialAppointmentDate from delius`() {
    val crn = "J678910"
    singleActiveConvictionResponse(crn)
    singleActiveInductionResponse(crn)
    // Given
    val deliusInitialAppointmentDate = LocalDate.parse("2021-11-30")
    val unallocatedCase = unallocatedCaseEvent(
      "Dylan Adam Armstrong", crn, "C1",
      "Currently managed"
    )

    // Then
    hmppsDomainSnsClient.publish(
      PublishRequest(hmppsDomainTopicArn, jsonString(unallocatedCase))
        .withMessageAttributes(
          mapOf(
            "eventType" to MessageAttributeValue().withDataType("String").withStringValue(unallocatedCase.eventType)
          )
        )
    )
    await untilCallTo { repository.count() } matches { it!! > 0 }

    val case = repository.findAll().first()

    assertThat(case.initial_appointment).isEqualTo(deliusInitialAppointmentDate)
  }
}
