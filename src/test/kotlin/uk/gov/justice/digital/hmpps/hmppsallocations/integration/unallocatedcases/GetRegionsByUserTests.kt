package uk.gov.justice.digital.hmpps.hmppsallocations.integration.unallocatedcases

import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.hmppsallocations.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.hmppsallocations.integration.mockserver.WorkforceAllocationsToDeliusApiExtension.Companion.workforceAllocationsToDelius

class GetRegionsByUserTests : IntegrationTestBase() {

  @Test
  fun `Get teams for user is successful`() {

    val userId = "User1"
    workforceAllocationsToDelius.setuserTeams(userId)

    webTestClient.get()
      .uri("/user/" + userId + "/regions")
      .headers { it.authToken(roles = listOf("ROLE_MANAGE_A_WORKFORCE_ALLOCATE")) }
      .exchange()
      .expectStatus()
      .isOk
      .expectBody()
      .jsonPath("$.regions")
      .isEqualTo(listOf("S03F01", "S03F02"))
  }
}
