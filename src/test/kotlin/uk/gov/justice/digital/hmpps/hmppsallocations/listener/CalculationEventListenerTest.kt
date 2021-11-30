package uk.gov.justice.digital.hmpps.hmppsallocations.listener

import com.amazonaws.services.sns.model.MessageAttributeValue
import com.amazonaws.services.sns.model.PublishRequest
import org.assertj.core.api.Assertions
import org.awaitility.kotlin.await
import org.awaitility.kotlin.matches
import org.awaitility.kotlin.untilCallTo
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.hmppsallocations.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.hmppsallocations.jpa.entity.UnallocatedCaseEntity
import java.time.LocalDate

internal class CalculationEventListenerTest : IntegrationTestBase() {

  @Test
  fun `retrieve sentenceDate from delius`() {
    // Given
    val crn = "J678910"
    tierCalculationResponse(crn)
    // Ad record in the DB for that CRN in order to check exist.

    repository.save(UnallocatedCaseEntity(crn = "J678910", tier = "D0", name = "foo", status = "active", sentence_date = LocalDate.now()))

    hmppsDomainSnsClient.publish(
      PublishRequest(hmppsDomainTopicArn, tierUpdateMessage())
        .withMessageAttributes(
          mapOf("eventType" to MessageAttributeValue().withDataType("String").withStringValue("TIER_CALCULATION_COMPLETE"))
        )
    )

    // Then check entry is in the DB.
    await untilCallTo { repository.findByCrn(crn) } matches { it!!.tier.equals("B3") }

    val case = repository.findAll().first()

    Assertions.assertThat(case.tier).isEqualTo("B3")
  }

  fun tierUpdateMessage() = """
{
  "Type": "Notification",
  "MessageId": "f39059e7-a62d-4157-929a-fb049015c993",
  "Token": null,
  "TopicArn": "arn:aws:sns:eu-west-2:000000000000:hmpps-domain",
  "Message": "{\"crn\":\"J678910\",\"calculationId\":\"e45559d1-3460-4a0e-8281-c736de57c562\"}",
  "SubscribeURL": null,
  "Timestamp": "2021-10-21T06:27:57.028Z",
  "SignatureVersion": "1",
  "Signature": "EXAMPLEpH+..",
  "SigningCertURL": "https://sns.us-east-1.amazonaws.com/SimpleNotificationService-0000000000000000000000.pem",
  "MessageAttributes": {
    "eventType": {
      "Type": "String",
      "Value": "TIER_CALCULATION_COMPLETE"
    }
  }
}   
  """.trimIndent()
}
