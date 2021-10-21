package uk.gov.justice.digital.hmpps.hmppsallocations.integration.unallocatedcases

import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.hmppsallocations.controller.UnallocatedCase
import uk.gov.justice.digital.hmpps.hmppsallocations.integration.IntegrationTestBase

class UnallocatedCasesTest : IntegrationTestBase() {
  @Test
  fun `can get unallocated cases`() {
    webTestClient.get()
      .uri("/cases/unallocated")
      .exchange()
      .expectStatus()
      .isOk
      .expectBodyList(UnallocatedCase::class.java)
  }
}
