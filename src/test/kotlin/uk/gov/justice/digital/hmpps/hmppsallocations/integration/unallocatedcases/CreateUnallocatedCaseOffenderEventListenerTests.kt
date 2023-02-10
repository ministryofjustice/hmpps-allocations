package uk.gov.justice.digital.hmpps.hmppsallocations.integration.unallocatedcases

import io.mockk.Called
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.awaitility.kotlin.await
import org.awaitility.kotlin.matches
import org.awaitility.kotlin.untilCallTo
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.hmppsallocations.domain.CaseTypes
import uk.gov.justice.digital.hmpps.hmppsallocations.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.hmppsallocations.integration.mockserver.CommunityApiExtension.Companion.communityApi
import uk.gov.justice.digital.hmpps.hmppsallocations.integration.mockserver.TierApiExtension.Companion.hmppsTier
import java.time.LocalDate

class CreateUnallocatedCaseOffenderEventListenerTests : IntegrationTestBase() {

  @Test
  fun `retrieve event with minimum required data will save`() {
    val crn = "J678910"
    val convictionId = 123456789L
    communityApi.singleActiveConvictionResponseForAllConvictions(crn)
    communityApi.unallocatedConvictionResponse(crn, convictionId)
    communityApi.singleActiveInductionResponse(crn)
    hmppsTier.tierCalculationResponse(crn)
    communityApi.offenderDetailsResponse(crn)
    communityApi.singleActiveConvictionResponse(crn)

    publishConvictionChangedMessage(crn)

    await untilCallTo { repository.count() } matches { it!! > 0 }

    val case = repository.findAll().first()

    assertThat(case.initialAppointment).isEqualTo(LocalDate.parse("2021-11-30"))
    assertThat(case.name).isEqualTo("Tester TestSurname")
    assertThat(case.tier).isEqualTo("B3")
    assertThat(case.caseType).isEqualTo(CaseTypes.CUSTODY)
    assertThat(case.teamCode).isEqualTo("TM1")
    assertThat(case.providerCode).isEqualTo("PAC1")
    assertThat(case.convictionNumber).isEqualTo(1)

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
    communityApi.singleActiveConvictionResponseForAllConvictions(crn)
    communityApi.allocatedConvictionResponse(crn, convictionId)
    communityApi.singleActiveInductionResponse(crn)
    hmppsTier.tierCalculationResponse(crn)
    communityApi.offenderDetailsResponse(crn)
    communityApi.singleActiveConvictionResponse(crn)

    publishConvictionChangedMessage(crn)

    await untilCallTo { countMessagesOnOffenderEventQueue() } matches { it == 0 }

    assertThat(countMessagesOnOffenderEventDeadLetterQueue()).isEqualTo(0)
    assertThat(repository.count()).isEqualTo(0)

    verify { telemetryClient wasNot Called }
  }

  @Test
  fun `do not save when crn is not found`() {
    val crn = "J678910"
    communityApi.notFoundActiveConvictionsResponse(crn)

    publishConvictionChangedMessage(crn)

    await untilCallTo { countMessagesOnOffenderEventQueue() } matches { it == 0 }

    assertThat(countMessagesOnOffenderEventDeadLetterQueue()).isEqualTo(0)
    assertThat(repository.count()).isEqualTo(0)
  }

  @Test
  fun `do not save when restricted case`() {
    val crn = "J678910"
    val convictionId = 123456789L
    communityApi.singleActiveConvictionResponseForAllConvictions(crn)
    communityApi.unallocatedConvictionResponse(crn, convictionId)
    communityApi.singleActiveInductionResponse(crn)
    hmppsTier.tierCalculationResponse(crn)
    communityApi.offenderDetailsForbiddenResponse(crn)
    communityApi.singleActiveConvictionResponse(crn)

    publishConvictionChangedMessage(crn)

    await untilCallTo { countMessagesOnOffenderEventQueue() } matches { it == 0 }
    await untilCallTo { countMessagesOnOffenderEventDeadLetterQueue() } matches { it == 0 }
  }

  @Test
  fun `do not save when conviction is not sentenced yet`() {
    val crn = "J678910"
    val convictionId = 123456789L
    communityApi.singleActiveConvictionResponseForAllConvictions(crn)
    communityApi.convictionWithNoSentenceResponse(crn, convictionId)
    communityApi.singleActiveInductionResponse(crn)
    hmppsTier.tierCalculationResponse(crn)
    communityApi.offenderDetailsResponse(crn)
    communityApi.singleActiveConvictionResponse(crn)

    publishConvictionChangedMessage(crn)

    await untilCallTo { countMessagesOnOffenderEventQueue() } matches { it == 0 }

    assertThat(countMessagesOnOffenderEventDeadLetterQueue()).isEqualTo(0)
    assertThat(repository.count()).isEqualTo(0)
  }

  @Test
  fun `do not save when conviction is not active`() {
    val crn = "J678910"
    val convictionId = 123456789L
    communityApi.singleActiveConvictionResponseForAllConvictions(crn)
    communityApi.inactiveConvictionResponse(crn, convictionId)
    communityApi.singleActiveInductionResponse(crn)
    hmppsTier.tierCalculationResponse(crn)
    communityApi.offenderDetailsResponse(crn)
    communityApi.singleActiveConvictionResponse(crn)

    publishConvictionChangedMessage(crn)

    await untilCallTo { countMessagesOnOffenderEventQueue() } matches { it == 0 }

    assertThat(countMessagesOnOffenderEventDeadLetterQueue()).isEqualTo(0)
    assertThat(repository.count()).isEqualTo(0)
  }

  @Test
  fun `do not save when conviction is not found`() {
    val crn = "J678910"
    val convictionId = 123456789L
    communityApi.singleActiveConvictionResponseForAllConvictions(crn)
    communityApi.notFoundConvictionResponse(crn, convictionId)
    communityApi.singleActiveInductionResponse(crn)
    hmppsTier.tierCalculationResponse(crn)
    communityApi.offenderDetailsResponse(crn)
    communityApi.singleActiveConvictionResponse(crn)

    publishConvictionChangedMessage(crn)

    await untilCallTo { countMessagesOnOffenderEventQueue() } matches { it == 0 }

    assertThat(countMessagesOnOffenderEventDeadLetterQueue()).isEqualTo(0)
    assertThat(repository.count()).isEqualTo(0)
  }

  @Test
  fun `create case when one conviction is sentenced and other is still being sentenced`() {
    val crn = "J678910"
    val convictionId = 123456789L
    communityApi.singleActiveConvictionResponseForAllConvictions(crn)
    communityApi.unallocatedConvictionResponse(crn, convictionId)
    communityApi.singleActiveInductionResponse(crn)
    hmppsTier.tierCalculationResponse(crn)
    communityApi.offenderDetailsResponse(crn)
    communityApi.activeSentenacedAndPreConvictionResponse(crn)

    publishConvictionChangedMessage(crn)

    await untilCallTo { repository.count() } matches { it!! > 0 }

    val case = repository.findAll().first()

    assertThat(case.initialAppointment).isEqualTo(LocalDate.parse("2021-11-30"))
    assertThat(case.name).isEqualTo("Tester TestSurname")
    assertThat(case.tier).isEqualTo("B3")
    assertThat(case.caseType).isEqualTo(CaseTypes.CUSTODY)
  }
}
