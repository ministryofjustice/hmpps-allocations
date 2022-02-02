package uk.gov.justice.digital.hmpps.hmppsallocations.integration.unallocatedcases

import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.hmppsallocations.integration.IntegrationTestBase

class GetCaseRisksByCrnTest : IntegrationTestBase() {

  @Test
  fun `can get case risks`() {
    val crn = "J678910"
    insertCases()

    webTestClient.get()
      .uri("/cases/unallocated/$crn/risks")
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
      .jsonPath("$.activeRegistrations")
      .isEmpty
      .jsonPath("$.inactiveRegistrations")
      .isEmpty
  }
}
