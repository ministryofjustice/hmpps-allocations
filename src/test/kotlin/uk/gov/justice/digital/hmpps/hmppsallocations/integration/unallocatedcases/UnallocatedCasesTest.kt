package uk.gov.justice.digital.hmpps.hmppsallocations.integration.unallocatedcases

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.hmppsallocations.controller.UnallocatedCase
import uk.gov.justice.digital.hmpps.hmppsallocations.integration.IntegrationTestBase

class UnallocatedCasesTest : IntegrationTestBase() {
  @Test
  fun `can get unallocated cases`() {
    val unallocatedCases = webTestClient.get()
      .uri("/cases/unallocated")
      .exchange()
      .expectStatus()
      .isOk
      .expectBodyList(UnallocatedCase::class.java)
      .returnResult().responseBody
    assertThat(unallocatedCases.size).isEqualTo(1)
    val firstCase = unallocatedCases.get(0)
    assertThat(firstCase).isEqualTo(
      UnallocatedCase("Dylan Adam Armstrong", "J678910", "C1", "17 October 2021", "22 October 2021",	"Currently managed")
    )
  }
}
