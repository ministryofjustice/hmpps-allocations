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
    val convictionId = 123456789L
    unallocatedConvictionResponse(crn, convictionId)
    noActiveInductionResponse(crn)
    offenderSummaryResponse(crn)
    getStaffWithGradeFromDelius(crn)
    tierCalculationResponse(crn)
    singleActiveConvictionResponse(crn)
    singleActiveConvictionResponseForAllConvictions(crn)

    // Given
    val deliusSentenceDate = LocalDate.parse("2019-11-17")
    val unallocatedCase = unallocatedCaseEvent(
      crn, convictionId
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

    assertThat(case.sentenceDate).isEqualTo(deliusSentenceDate)
  }

  @Test
  fun `retrieve initialAppointmentDate from delius`() {
    val crn = "J678910"
    val convictionId = 123456789L
    unallocatedConvictionResponse(crn, convictionId)
    singleActiveInductionResponse(crn)
    offenderSummaryResponse(crn)
    getStaffWithGradeFromDelius(crn)
    tierCalculationResponse(crn)
    singleActiveConvictionResponse(crn)
    singleActiveConvictionResponseForAllConvictions(crn)

    // Given
    val deliusInitialAppointmentDate = LocalDate.parse("2021-11-30")
    val unallocatedCase = unallocatedCaseEvent(
      crn, convictionId
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

    assertThat(case.initialAppointment).isEqualTo(deliusInitialAppointmentDate)
  }

  @Test
  fun `retrieve no initialAppointmentDate from delius`() {
    val crn = "J678910"
    val convictionId = 123456789L
    unallocatedConvictionResponse(crn, convictionId)
    noActiveInductionResponse(crn)
    offenderSummaryResponse(crn)
    getStaffWithGradeFromDelius(crn)
    tierCalculationResponse(crn)
    singleActiveConvictionResponse(crn)
    singleActiveConvictionResponseForAllConvictions(crn)

    // Given
    val unallocatedCase = unallocatedCaseEvent(
      crn, convictionId
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

    assertThat(case.initialAppointment).isNull()
  }

  @Test
  fun `retrieve first initialAppointmentDate from delius`() {
    // Given
    val crn = "J678910"
    val convictionId = 123456789L
    unallocatedConvictionResponse(crn, convictionId)
    offenderSummaryResponse(crn)
    getStaffWithGradeFromDelius(crn)
    tierCalculationResponse(crn)
    singleActiveConvictionResponse(crn)
    singleActiveConvictionResponseForAllConvictions(crn)

    val inductionRequest =
      HttpRequest.request().withPath("/offenders/crn/$crn/contact-summary/inductions").withMethod("GET")

    communityApi.`when`(inductionRequest, Times.exactly(1)).respond(
      HttpResponse.response()
        .withContentType(APPLICATION_JSON).withBody(twoActiveInductionResponse())
    )

    val deliusInitialAppointmentDate = LocalDate.parse("2021-10-30")
    val unallocatedCase = unallocatedCaseEvent(
      crn, convictionId
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

    assertThat(case.initialAppointment).isEqualTo(deliusInitialAppointmentDate)
  }

  @Test
  fun `retrieve name from delius`() {
    val crn = "J678910"
    val convictionId = 123456789L
    unallocatedConvictionResponse(crn, convictionId)
    noActiveInductionResponse(crn)
    offenderSummaryResponse(crn)
    getStaffWithGradeFromDelius(crn)
    tierCalculationResponse(crn)
    singleActiveConvictionResponse(crn)
    singleActiveConvictionResponseForAllConvictions(crn)

    // Given
    val unallocatedCase = unallocatedCaseEvent(
      crn, convictionId
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
    val convictionId = 123456789L
    unallocatedConvictionResponse(crn, convictionId)
    noActiveInductionResponse(crn)
    offenderSummaryResponse(crn)
    getStaffWithGradeFromDelius(crn)
    tierCalculationResponse(crn)
    singleActiveConvictionResponse(crn)
    singleActiveConvictionResponseForAllConvictions(crn)

    // Given
    val unallocatedCase = unallocatedCaseEvent(
      crn, convictionId
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
    val convictionId = 123456789L
    unallocatedConvictionResponse(crn, convictionId)
    noActiveInductionResponse(crn)
    offenderSummaryResponse(crn)
    getStaffWithGradeFromDelius(crn)
    tierCalculationResponse(crn)
    singleActiveConvictionResponse(crn)
    singleActiveConvictionResponseForAllConvictions(crn)

    // Given
    val unallocatedCase = unallocatedCaseEvent(
      crn, convictionId
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
    assertThat(case.offenderManagerGrade).isEqualTo("PSO")
    assertThat(case.offenderManagerForename).isEqualTo("Sheila Linda")
    assertThat(case.offenderManagerSurname).isEqualTo("Hancock")
  }

  @Test
  fun `retrieve currently managed probation status from delius`() {
    val crn = "J678910"
    val convictionId = 123456789L
    unallocatedConvictionResponse(crn, convictionId)
    noActiveInductionResponse(crn)
    offenderSummaryResponse(crn)
    tierCalculationResponse(crn)
    twoActiveConvictionsResponse(crn)
    getStaffWithGradeFromDelius(crn)

    // Given
    val unallocatedCase = unallocatedCaseEvent(
      crn, convictionId
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
    assertThat(case.offenderManagerGrade).isEqualTo("PSO")
    assertThat(case.offenderManagerForename).isEqualTo("Sheila Linda")
    assertThat(case.offenderManagerSurname).isEqualTo("Hancock")
  }

  @Test
  fun `retrieve previously managed probation status from delius`() {
    val crn = "J678910"
    val convictionId = 123456789L
    unallocatedConvictionResponse(crn, convictionId)
    noActiveInductionResponse(crn)
    offenderSummaryResponse(crn)
    getStaffWithGradeFromDelius(crn)
    tierCalculationResponse(crn)
    singleActiveConvictionResponse(crn)
    singleActiveAndInactiveConvictionsResponse(crn)

    // Given
    val unallocatedCase = unallocatedCaseEvent(
      crn, convictionId
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
    assertThat(case.previousConvictionDate).isEqualTo("2009-10-12")
    assertThat(case.offenderManagerGrade).isEqualTo("PSO")
    assertThat(case.offenderManagerForename).isEqualTo("Sheila Linda")
    assertThat(case.offenderManagerSurname).isEqualTo("Hancock")
  }

  @Test
  fun `retrieve offenderManager without grade for currently managed `() {
    val crn = "J678910"
    val convictionId = 123456789L
    unallocatedConvictionResponse(crn, convictionId)
    noActiveInductionResponse(crn)
    offenderSummaryResponse(crn)
    tierCalculationResponse(crn)
    twoActiveConvictionsResponse(crn)
    getStaffWithoutGradeFromDelius(crn)
    // Given
    val unallocatedCase = unallocatedCaseEvent(
      crn, convictionId
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

    assertThat(case.offenderManagerGrade).isNull()
  }
}
