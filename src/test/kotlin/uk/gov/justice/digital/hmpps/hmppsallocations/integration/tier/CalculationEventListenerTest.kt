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
    val crn = "X123456"
    tierCalculationResponse(crn)
    writeUnallocatedCaseToDatabase(crn, "D0")
    publishTierCalculationCompleteMessage(crn)
    checkTierHasBeenUpdated(crn, "B3")
  }

  @Test
  fun `does not get tier calculation when the crn is not for an unallocated case`() {
    val crn = "J678910"
    val tierCalculationRequest = tierCalculationResponse(crn)
    publishTierCalculationCompleteMessage(crn)
    whenCalculationQueueIsEmpty()
    whenCalculationMessageHasBeenProcessed()
    hmppsTier.verify(tierCalculationRequest, VerificationTimes.exactly(0))
  }

  private fun writeUnallocatedCaseToDatabase(crn: String, tier: String) {
    repository.save(
      UnallocatedCaseEntity(
        crn = crn,
        tier = tier,
        name = "foo",
        status = "active",
        sentenceDate = LocalDate.now()
      )
    )
  }

  private fun checkTierHasBeenUpdated(crn: String, tier: String) {
    await untilCallTo { repository.findByCrn(crn) } matches { it!!.tier.equals(tier) }
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
