package uk.gov.justice.digital.hmpps.hmppsallocations.integration.unallocatedcases

import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.hmppsallocations.integration.IntegrationTestBase

class GetCaseCount : IntegrationTestBase() {

  @Test
  fun `can get case count of unallocated cases`() {
    insertCases()
    webTestClient.get()
      .uri("/cases/unallocated/count")
      .headers { it.authToken(roles = listOf("ROLE_MANAGE_A_WORKFORCE_ALLOCATE")) }
      .exchange()
      .expectStatus()
      .isOk
      .expectBody()
      .jsonPath("$.count")
      .isEqualTo(5)
  }
}
