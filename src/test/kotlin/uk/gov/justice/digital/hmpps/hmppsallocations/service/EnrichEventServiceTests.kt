package uk.gov.justice.digital.hmpps.hmppsallocations.service

import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import reactor.core.publisher.Mono
import uk.gov.justice.digital.hmpps.hmppsallocations.client.CommunityApiClient
import uk.gov.justice.digital.hmpps.hmppsallocations.domain.Conviction
import uk.gov.justice.digital.hmpps.hmppsallocations.domain.OrderManager
import java.time.LocalDate
import java.time.LocalDateTime

class EnrichEventServiceTests {

  private val communityApiClient = mockk<CommunityApiClient>()
  private val enrichEventService = EnrichEventService(communityApiClient, mockk(), mockk(), mockk())

  @BeforeEach
  fun setupMocks() {

    every { communityApiClient.getInactiveConvictions(any()) } returns Mono.just(emptyList())
  }

  @Test
  fun `must be new to probation if multiple convictions and currently allocated offender manager is unallocated`() {
    every { communityApiClient.getOffenderManagerName(any()) } returns Mono.just(
      CommunityApiClient.OffenderManager(
        CommunityApiClient.Staff("Forename", "Surname"), CommunityApiClient.Grade("Code"), true
      )
    )

    val crn = "CRN1111"
    val activeConvictions = listOf(generateUnallocatedConviction(), generateUnallocatedConviction())
    val result = enrichEventService.getProbationStatus(crn, activeConvictions)
    Assertions.assertEquals(ProbationStatusType.NEW_TO_PROBATION, result.status)
  }

  private fun generateUnallocatedConviction(): Conviction = Conviction(
    LocalDate.now(), null, true, emptyList(), 1L,
    orderManagers = listOf(
      OrderManager(LocalDateTime.now(), "Unallocated", "STAFFU", "GRADE", "TEAM", "REGION1")
    ),
    null, 1
  )
}
