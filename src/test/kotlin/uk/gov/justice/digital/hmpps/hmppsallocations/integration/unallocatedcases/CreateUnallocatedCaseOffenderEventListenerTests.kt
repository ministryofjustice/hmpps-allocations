package uk.gov.justice.digital.hmpps.hmppsallocations.integration.unallocatedcases

import com.amazonaws.services.sns.model.MessageAttributeValue
import com.amazonaws.services.sns.model.PublishRequest
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.awaitility.kotlin.await
import org.awaitility.kotlin.matches
import org.awaitility.kotlin.untilCallTo
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.hmppsallocations.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.hmppsallocations.integration.mockserver.CommunityApiExtension.Companion.communityApi
import uk.gov.justice.digital.hmpps.hmppsallocations.integration.mockserver.TierApiExtension.Companion.hmppsTier
import uk.gov.justice.digital.hmpps.hmppsallocations.integration.mockserver.WorkforceAllocationsToDeliusApiExtension.Companion.workforceAllocationsToDelius

class CreateUnallocatedCaseOffenderEventListenerTests : IntegrationTestBase() {

  @Test
  fun `retrieve event with minimum required data will save`() {
    val crn = "J678910"
    communityApi.getUserAccessForCrn(crn)
    workforceAllocationsToDelius.unallocatedEventsResponse(crn)
    hmppsTier.tierCalculationResponse(crn)

    hmppsOffenderSnsClient.publish(
      PublishRequest(hmppsOffenderTopicArn, jsonString(offenderEvent(crn))).withMessageAttributes(
        mapOf("eventType" to MessageAttributeValue().withDataType("String").withStringValue("CONVICTION_CHANGED")),
      ),
    )

    await untilCallTo { repository.count() } matches { it!! > 0 }

    val case = repository.findAll().first()

    assertThat(case.name).isEqualTo("Tester TestSurname")
    assertThat(case.tier).isEqualTo("B3")
    assertThat(case.teamCode).isEqualTo("TM1")
    assertThat(case.providerCode).isEqualTo("PAC1")
    assertThat(case.convictionNumber).isEqualTo(1)

    verify(exactly = 1) {
      telemetryClient.trackEvent(
        "AllocationDemandRaised",
        mapOf(
          "crn" to crn,
          "teamCode" to "TM1",
          "providerCode" to "PAC1",
        ),
        null,
      )
    }
  }

  @Test
  fun `do not save when crn is not found`() {
    val crn = "J678910"
    communityApi.getUserAccessForCrnNotFound(crn)
    workforceAllocationsToDelius.unallocatedEventsNotFoundResponse(crn)

    hmppsOffenderSnsClient.publish(
      PublishRequest(hmppsOffenderTopicArn, jsonString(offenderEvent(crn))).withMessageAttributes(
        mapOf("eventType" to MessageAttributeValue().withDataType("String").withStringValue("CONVICTION_CHANGED")),
      ),
    )

    await untilCallTo { countMessagesOnOffenderEventQueue() } matches { it == 0 }

    assertThat(countMessagesOnOffenderEventDeadLetterQueue()).isEqualTo(0)
    assertThat(repository.count()).isEqualTo(0)
  }

  @Test
  fun `do not save when restricted case`() {
    val crn = "J678910"
    communityApi.getUserAccessForCrnForbidden(crn)
    workforceAllocationsToDelius.unallocatedEventsResponse(crn)

    hmppsTier.tierCalculationResponse(crn)

    hmppsOffenderSnsClient.publish(
      PublishRequest(hmppsOffenderTopicArn, jsonString(offenderEvent(crn))).withMessageAttributes(
        mapOf("eventType" to MessageAttributeValue().withDataType("String").withStringValue("CONVICTION_CHANGED")),
      ),
    )

    await untilCallTo { countMessagesOnOffenderEventQueue() } matches { it == 0 }
    await untilCallTo { countMessagesOnOffenderEventDeadLetterQueue() } matches { it == 0 }

    Assertions.assertFalse(repository.existsByCrn(crn))
  }
}
