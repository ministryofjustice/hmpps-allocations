package uk.gov.justice.digital.hmpps.hmppsallocations.integration.unallocatedcases

import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.hmppsallocations.integration.IntegrationTestBase

class GetCaseAllocateOverviewByCrnTest : IntegrationTestBase() {

  @Test
  fun `can get Overview of Offender Manager when allocate by crn`() {
    val crn = "J678910"
    val convictionId = 123456789L
    val offenderManagerCode = "OM1"
    insertCases()
    getOffenderManagerOverviewWhenAllocatingForCrn(crn, offenderManagerCode)
    webTestClient.get()
      .uri("/cases/$crn/convictions/$convictionId/allocate/$offenderManagerCode/overview")
      .headers { it.authToken(roles = listOf("ROLE_MANAGE_A_WORKFORCE_ALLOCATE")) }
      .exchange()
      .expectStatus()
      .isOk
      .expectBody()
      .jsonPath("$.name")
      .isEqualTo("Dylan Adam Armstrong")
      .jsonPath("$.crn")
      .isEqualTo("J678910")
      .jsonPath("$.tier")
      .isEqualTo("C1")
      .jsonPath("$.offenderManagerForename")
      .isEqualTo("John")
      .jsonPath("$.offenderManagerSurname")
      .isEqualTo("Smith")
      .jsonPath("$.offenderManagerGrade")
      .isEqualTo("PO")
      .jsonPath("$.offenderManagerCurrentCapacity")
      .isEqualTo(67.4)
      .jsonPath("$.offenderManagerCode")
      .isEqualTo("OM1")
      .jsonPath("$.offenderManagerTotalCases")
      .isEqualTo(22)
      .jsonPath("$.convictionId")
      .isEqualTo(convictionId)
      .jsonPath("$.teamName")
      .isEqualTo("Test Team")
      .jsonPath("$.offenderManagerWeeklyHours")
      .isEqualTo(22.5)
      .jsonPath("$.offenderManagerTotalReductionHours")
      .isEqualTo(10)
      .jsonPath("$.offenderManagerPointsAvailable")
      .isEqualTo(1265)
      .jsonPath("$.offenderManagerPointsUsed")
      .isEqualTo(1580)
      .jsonPath("$.offenderManagerPointsRemaining")
      .isEqualTo(-315)
  }
}
