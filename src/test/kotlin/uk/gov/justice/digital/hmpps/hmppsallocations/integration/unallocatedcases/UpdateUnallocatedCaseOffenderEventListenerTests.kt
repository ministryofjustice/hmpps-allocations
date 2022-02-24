package uk.gov.justice.digital.hmpps.hmppsallocations.integration.unallocatedcases

import com.amazonaws.services.sns.model.MessageAttributeValue
import com.amazonaws.services.sns.model.PublishRequest
import org.assertj.core.api.Assertions.assertThat
import org.awaitility.kotlin.await
import org.awaitility.kotlin.matches
import org.awaitility.kotlin.untilCallTo
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.hmppsallocations.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.hmppsallocations.jpa.entity.UnallocatedCaseEntity
import java.time.LocalDate

class UpdateUnallocatedCaseOffenderEventListenerTests : IntegrationTestBase() {

  @Test
  fun `only contain one record in db if case remains the same after event emitted about it`() {
    val crn = "J678910"
    val convictionId = 123456789L
    repository.save(
      UnallocatedCaseEntity(
        crn = crn,
        sentenceDate = LocalDate.parse("2019-11-17"),
        initialAppointment = LocalDate.parse("2021-11-30"),
        name = "Tester TestSurname",
        tier = "B3",
        status = "New to probation",
        convictionId = convictionId
      )
    )

    unallocatedConvictionResponse(crn, convictionId)
    singleActiveInductionResponse(crn)
    tierCalculationResponse(crn)
    offenderSummaryResponse(crn)
    singleActiveConvictionResponse(crn)
    singleActiveConvictionResponseForAllConvictions(crn)

    hmppsOffenderSnsClient.publish(
      PublishRequest(hmppsOffenderTopicArn, jsonString(offenderEvent(crn, convictionId))).withMessageAttributes(
        mapOf("eventType" to MessageAttributeValue().withDataType("String").withStringValue("CONVICTION_CHANGED"))
      )
    )

    await untilCallTo { countMessagesOnOffenderEventQueue() } matches { it == 0 }

    assertThat(repository.count()).isEqualTo(1)
    val case = repository.findAll().first()

    assertThat(case.sentenceDate).isEqualTo(LocalDate.parse("2019-11-17"))
    assertThat(case.initialAppointment).isEqualTo(LocalDate.parse("2021-11-30"))
    assertThat(case.name).isEqualTo("Tester TestSurname")
    assertThat(case.tier).isEqualTo("B3")
    assertThat(case.status).isEqualTo("New to probation")
  }
}
