package uk.gov.justice.digital.hmpps.hmppsallocations.integration.tier

import com.amazonaws.services.sns.model.MessageAttributeValue
import com.amazonaws.services.sns.model.PublishRequest
import org.awaitility.kotlin.await
import org.awaitility.kotlin.matches
import org.awaitility.kotlin.untilCallTo
import org.junit.jupiter.api.Test
import org.mockserver.verify.VerificationTimes
import uk.gov.justice.digital.hmpps.hmppsallocations.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.hmppsallocations.jpa.entity.UnallocatedCaseEntity
import java.time.LocalDate

internal class CalculationEventListenerTest : IntegrationTestBase() {

  @Test
  fun `change tier after event calculation is consumed`() {

    val crn = "J678910"
    tierCalculationResponse(crn)
    // Add  unallocated case to the database with the original tier value that will be  changed based on consumed message.
    repository.save(UnallocatedCaseEntity(crn = crn, tier = "D0", name = "foo", status = "active", sentenceDate = LocalDate.now()))

    publishTierCalculationCompleteMessage(crn)

    // Then check entry is in the DB.
    await untilCallTo { repository.findByCrn(crn) } matches { it!!.tier.equals("B3") }
  }

  @Test
  fun `does not get tier calculation when the crn is not for an unallocated case`() {

    val crn = "J678910"
    val tierCalculationRequest = tierCalculationResponse(crn)
    // Add  unallocated case to the database with the original tier value
    repository.save(UnallocatedCaseEntity(crn = "A123456", tier = "D0", name = "foo", status = "active", sentenceDate = LocalDate.now()))

    publishTierCalculationCompleteMessage(crn)

    whenCalculationQueueIsEmpty()
    whenCalculationMessageHasBeenProcessed()
    hmppsTier.verify(tierCalculationRequest, VerificationTimes.exactly(0))
  }

  private fun publishTierCalculationCompleteMessage(crn: String) {
    hmppsDomainSnsClient.publish(
      PublishRequest(hmppsDomainTopicArn, jsonString(tierCalculationEvent(crn)))
        .withMessageAttributes(
          mapOf(
            "eventType" to MessageAttributeValue().withDataType("String").withStringValue("TIER_CALCULATION_COMPLETE")
          )
        )
    )
  }
}
