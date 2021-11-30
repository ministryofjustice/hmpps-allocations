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
import org.mockserver.model.MediaType.APPLICATION_JSON
import uk.gov.justice.digital.hmpps.hmppsallocations.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.hmppsallocations.integration.responses.twoActiveInductionResponse
import java.time.LocalDate

class EventListenerTest : IntegrationTestBase() {
  @Test
  fun `retrieve sentenceDate from delius`() {
    val crn = "J678910"
    singleActiveConvictionResponse(crn)
    noActiveInductionResponse(crn)
    offenderSummaryResponse(crn)
    tierCalculationResponse(crn)
    singleActiveConvictionResponse(crn)
    singleActiveConvictionResponse(crn)

    // Given
    val deliusSentenceDate = LocalDate.parse("2019-11-17")
    val unallocatedCase = unallocatedCaseEvent(
      crn
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
    twoActiveConvictionsResponse(crn)
    noActiveInductionResponse(crn)
    offenderSummaryResponse(crn)
    tierCalculationResponse(crn)
    twoActiveConvictionsResponse(crn)
    twoActiveConvictionsResponse(crn)

    // Given
    val latestConvictionSentenceDate = LocalDate.parse("2021-11-22")
    val unallocatedCase = unallocatedCaseEvent(
      crn
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
    offenderSummaryResponse(crn)
    tierCalculationResponse(crn)
    singleActiveConvictionResponse(crn)
    singleActiveConvictionResponse(crn)

    // Given
    val deliusInitialAppointmentDate = LocalDate.parse("2021-11-30")
    val unallocatedCase = unallocatedCaseEvent(
      crn
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

  @Test
  fun `retrieve no initialAppointmentDate from delius`() {
    val crn = "J678910"
    singleActiveConvictionResponse(crn)
    noActiveInductionResponse(crn)
    offenderSummaryResponse(crn)
    tierCalculationResponse(crn)
    singleActiveConvictionResponse(crn)

    // Given
    val unallocatedCase = unallocatedCaseEvent(
      crn
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

    assertThat(case.initial_appointment).isNull()
  }

  @Test
  fun `retrieve first initialAppointmentDate from delius`() {
    // Given
    val crn = "J678910"
    singleActiveConvictionResponse(crn)
    offenderSummaryResponse(crn)
    tierCalculationResponse(crn)
    singleActiveConvictionResponse(crn)
    singleActiveConvictionResponse(crn)

    val inductionRequest =
      HttpRequest.request().withPath("/offenders/crn/$crn/contact-summary/inductions").withMethod("GET")

    communityApi.`when`(inductionRequest, Times.exactly(1)).respond(
      HttpResponse.response()
        .withContentType(APPLICATION_JSON).withBody(twoActiveInductionResponse())
    )

    val deliusInitialAppointmentDate = LocalDate.parse("2021-10-30")
    val unallocatedCase = unallocatedCaseEvent(
      crn
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

  @Test
  fun `retrieve name from delius`() {
    val crn = "J678910"
    singleActiveConvictionResponse(crn)
    noActiveInductionResponse(crn)
    offenderSummaryResponse(crn)
    tierCalculationResponse(crn)
    singleActiveConvictionResponse(crn)
    singleActiveConvictionResponse(crn)

    // Given
    val unallocatedCase = unallocatedCaseEvent(
      crn
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

    assertThat(case.name).isEqualTo("Tester TestSurname")
  }

  @Test
  fun `retrieve tier from hmpps tier`() {
    val crn = "J678910"
    singleActiveConvictionResponse(crn)
    noActiveInductionResponse(crn)
    offenderSummaryResponse(crn)
    tierCalculationResponse(crn)
    singleActiveConvictionResponse(crn)
    singleActiveConvictionResponse(crn)

    // Given
    val unallocatedCase = unallocatedCaseEvent(
      crn
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

    assertThat(case.tier).isEqualTo("B3")
  }

  @Test
  fun `retrieve new to probation status from delius`() {
    val crn = "J678910"
    singleActiveConvictionResponse(crn)
    noActiveInductionResponse(crn)
    offenderSummaryResponse(crn)
    tierCalculationResponse(crn)
    singleActiveConvictionResponse(crn)
    singleActiveConvictionResponse(crn)

    // Given
    val unallocatedCase = unallocatedCaseEvent(
      crn
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

    assertThat(case.status).isEqualTo("New to probation")
  }

  @Test
  fun `retrieve currently managed probation status from delius`() {
    val crn = "J678910"
    twoActiveConvictionsResponse(crn)
    noActiveInductionResponse(crn)
    offenderSummaryResponse(crn)
    tierCalculationResponse(crn)
    twoActiveConvictionsResponse(crn)
    // Given
    val unallocatedCase = unallocatedCaseEvent(
      crn
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

    assertThat(case.status).isEqualTo("Currently managed")
  }

  @Test
  fun `retrieve previously managed probation status from delius`() {
    val crn = "J678910"
    singleActiveConvictionResponse(crn)
    noActiveInductionResponse(crn)
    offenderSummaryResponse(crn)
    tierCalculationResponse(crn)
    singleActiveConvictionResponse(crn)
    singleActiveAndInactiveConvictionsResponse(crn)

    // Given
    val unallocatedCase = unallocatedCaseEvent(
      crn
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

    assertThat(case.status).isEqualTo("Previously managed")
    assertThat(case.previous_conviction_date).isEqualTo("2019-12-13")
  }
}
