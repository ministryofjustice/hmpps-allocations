package uk.gov.justice.digital.hmpps.hmppsallocations.integration.unallocatedcases

import com.amazonaws.services.sns.model.MessageAttributeValue
import com.amazonaws.services.sns.model.PublishRequest
import io.mockk.Called
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.awaitility.kotlin.await
import org.awaitility.kotlin.matches
import org.awaitility.kotlin.untilCallTo
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.hmppsallocations.domain.CaseTypes
import uk.gov.justice.digital.hmpps.hmppsallocations.integration.IntegrationTestBase
import java.time.LocalDate

class CreateUnallocatedCaseOffenderEventListenerTests : IntegrationTestBase() {

  @Test
  fun `retrieve event with minimum required data will save`() {
    val crn = "J678910"
    val convictionId = 123456789L
    singleActiveConvictionResponseForAllConvictions(crn)
    unallocatedConvictionResponse(crn, convictionId)
    singleActiveInductionResponse(crn)
    tierCalculationResponse(crn)
    offenderDetailsResponse(crn)
    getStaffWithGradeFromDelius(crn)
    singleActiveConvictionResponse(crn)
    singleActiveConvictionResponseForAllConvictions(crn)

    hmppsOffenderSnsClient.publish(
      PublishRequest(hmppsOffenderTopicArn, jsonString(offenderEvent(crn))).withMessageAttributes(
        mapOf("eventType" to MessageAttributeValue().withDataType("String").withStringValue("CONVICTION_CHANGED"))
      )
    )

    await untilCallTo { repository.count() } matches { it!! > 0 }

    val case = repository.findAll().first()

    assertThat(case.sentenceDate).isEqualTo(LocalDate.parse("2019-11-17"))
    assertThat(case.initialAppointment).isEqualTo(LocalDate.parse("2021-11-30"))
    assertThat(case.name).isEqualTo("Tester TestSurname")
    assertThat(case.tier).isEqualTo("B3")
    assertThat(case.status).isEqualTo("New to probation")
    assertThat(case.offenderManagerGrade).isEqualTo("PSO")
    assertThat(case.offenderManagerForename).isEqualTo("Sheila Linda")
    assertThat(case.offenderManagerSurname).isEqualTo("Hancock")
    assertThat(case.caseType).isEqualTo(CaseTypes.CUSTODY)
    assertThat(case.teamCode).isEqualTo("TM1")
    assertThat(case.providerCode).isEqualTo("PAC1")

    verify(exactly = 1) {
      telemetryClient.trackEvent(
        "AllocationDemandRaised",
        mapOf(
          "crn" to crn,
          "teamCode" to "TM1",
          "providerCode" to "PAC1"
        ),
        null
      )
    }
  }

  @Test
  fun `do not save when conviction allocated to actual officer`() {
    val crn = "J678910"
    val convictionId = 123456789L
    singleActiveConvictionResponseForAllConvictions(crn)
    allocatedConvictionResponse(crn, convictionId)
    singleActiveInductionResponse(crn)
    tierCalculationResponse(crn)
    offenderDetailsResponse(crn)
    singleActiveConvictionResponse(crn)
    singleActiveConvictionResponseForAllConvictions(crn)

    hmppsOffenderSnsClient.publish(
      PublishRequest(hmppsOffenderTopicArn, jsonString(offenderEvent(crn))).withMessageAttributes(
        mapOf("eventType" to MessageAttributeValue().withDataType("String").withStringValue("CONVICTION_CHANGED"))
      )
    )

    await untilCallTo { countMessagesOnOffenderEventQueue() } matches { it == 0 }

    assertThat(countMessagesOnOffenderEventDeadLetterQueue()).isEqualTo(0)
    assertThat(repository.count()).isEqualTo(0)

    verify { telemetryClient wasNot Called }
  }

  @Test
  fun `do not save when crn is not found`() {
    val crn = "J678910"
    notFoundAllConvictionResponse(crn)

    hmppsOffenderSnsClient.publish(
      PublishRequest(hmppsOffenderTopicArn, jsonString(offenderEvent(crn))).withMessageAttributes(
        mapOf("eventType" to MessageAttributeValue().withDataType("String").withStringValue("CONVICTION_CHANGED"))
      )
    )

    await untilCallTo { countMessagesOnOffenderEventQueue() } matches { it == 0 }

    assertThat(countMessagesOnOffenderEventDeadLetterQueue()).isEqualTo(0)
    assertThat(repository.count()).isEqualTo(0)
  }

  @Test
  fun `do not save when restricted case`() {
    val crn = "J678910"
    val convictionId = 123456789L
    singleActiveConvictionResponseForAllConvictions(crn)
    unallocatedConvictionResponse(crn, convictionId)
    singleActiveInductionResponse(crn)
    tierCalculationResponse(crn)
    offenderDetailsForbiddenResponse(crn)
    getStaffWithGradeFromDelius(crn)
    singleActiveConvictionResponse(crn)
    singleActiveConvictionResponseForAllConvictions(crn)

    hmppsOffenderSnsClient.publish(
      PublishRequest(hmppsOffenderTopicArn, jsonString(offenderEvent(crn))).withMessageAttributes(
        mapOf("eventType" to MessageAttributeValue().withDataType("String").withStringValue("CONVICTION_CHANGED"))
      )
    )

    await untilCallTo { countMessagesOnOffenderEventQueue() } matches { it == 0 }
    await untilCallTo { countMessagesOnOffenderEventDeadLetterQueue() } matches { it == 0 }
  }

  @Test
  fun `do not save when conviction is not sentenced yet`() {
    val crn = "J678910"
    val convictionId = 123456789L
    singleActiveConvictionResponseForAllConvictions(crn)
    convictionWithNoSentenceResponse(crn, convictionId)
    singleActiveInductionResponse(crn)
    tierCalculationResponse(crn)
    offenderDetailsResponse(crn)
    singleActiveConvictionResponse(crn)
    singleActiveConvictionResponseForAllConvictions(crn)

    hmppsOffenderSnsClient.publish(
      PublishRequest(hmppsOffenderTopicArn, jsonString(offenderEvent(crn))).withMessageAttributes(
        mapOf("eventType" to MessageAttributeValue().withDataType("String").withStringValue("CONVICTION_CHANGED"))
      )
    )

    await untilCallTo { countMessagesOnOffenderEventQueue() } matches { it == 0 }

    assertThat(countMessagesOnOffenderEventDeadLetterQueue()).isEqualTo(0)
    assertThat(repository.count()).isEqualTo(0)
  }

  @Test
  fun `do not save when conviction is not active`() {
    val crn = "J678910"
    val convictionId = 123456789L
    singleActiveConvictionResponseForAllConvictions(crn)
    inactiveConvictionResponse(crn, convictionId)
    singleActiveInductionResponse(crn)
    tierCalculationResponse(crn)
    offenderDetailsResponse(crn)
    singleActiveConvictionResponse(crn)
    singleActiveConvictionResponseForAllConvictions(crn)

    hmppsOffenderSnsClient.publish(
      PublishRequest(hmppsOffenderTopicArn, jsonString(offenderEvent(crn))).withMessageAttributes(
        mapOf("eventType" to MessageAttributeValue().withDataType("String").withStringValue("CONVICTION_CHANGED"))
      )
    )

    await untilCallTo { countMessagesOnOffenderEventQueue() } matches { it == 0 }

    assertThat(countMessagesOnOffenderEventDeadLetterQueue()).isEqualTo(0)
    assertThat(repository.count()).isEqualTo(0)
  }

  @Test
  fun `do not save when conviction is not found`() {
    val crn = "J678910"
    val convictionId = 123456789L
    singleActiveConvictionResponseForAllConvictions(crn)
    notFoundConvictionResponse(crn, convictionId)
    singleActiveInductionResponse(crn)
    tierCalculationResponse(crn)
    offenderDetailsResponse(crn)
    singleActiveConvictionResponse(crn)
    singleActiveConvictionResponseForAllConvictions(crn)

    hmppsOffenderSnsClient.publish(
      PublishRequest(hmppsOffenderTopicArn, jsonString(offenderEvent(crn))).withMessageAttributes(
        mapOf("eventType" to MessageAttributeValue().withDataType("String").withStringValue("CONVICTION_CHANGED"))
      )
    )

    await untilCallTo { countMessagesOnOffenderEventQueue() } matches { it == 0 }

    assertThat(countMessagesOnOffenderEventDeadLetterQueue()).isEqualTo(0)
    assertThat(repository.count()).isEqualTo(0)
  }

  @Test
  fun `create case when one conviction is sentenced and other is still being sentenced`() {
    val crn = "J678910"
    val convictionId = 123456789L
    singleActiveConvictionResponseForAllConvictions(crn)
    unallocatedConvictionResponse(crn, convictionId)
    singleActiveInductionResponse(crn)
    tierCalculationResponse(crn)
    offenderDetailsResponse(crn)
    getStaffWithGradeFromDelius(crn)
    activeSentenacedAndPreConvictionResponse(crn)
    singleActiveConvictionResponseForAllConvictions(crn)

    hmppsOffenderSnsClient.publish(
      PublishRequest(hmppsOffenderTopicArn, jsonString(offenderEvent(crn))).withMessageAttributes(
        mapOf("eventType" to MessageAttributeValue().withDataType("String").withStringValue("CONVICTION_CHANGED"))
      )
    )

    await untilCallTo { repository.count() } matches { it!! > 0 }

    val case = repository.findAll().first()

    assertThat(case.sentenceDate).isEqualTo(LocalDate.parse("2019-11-17"))
    assertThat(case.initialAppointment).isEqualTo(LocalDate.parse("2021-11-30"))
    assertThat(case.name).isEqualTo("Tester TestSurname")
    assertThat(case.tier).isEqualTo("B3")
    assertThat(case.status).isEqualTo("New to probation")
    assertThat(case.caseType).isEqualTo(CaseTypes.CUSTODY)
  }
}
