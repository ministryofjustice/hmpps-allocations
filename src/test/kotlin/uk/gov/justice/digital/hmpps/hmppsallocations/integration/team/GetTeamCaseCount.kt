package uk.gov.justice.digital.hmpps.hmppsallocations.integration.team

import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.hmppsallocations.integration.IntegrationTestBase

class GetTeamCaseCount : IntegrationTestBase() {

  @Test
  fun `must get team count of unallocated cases`() {
    val teamCode = "TEAM1"
    insertCases()

    webTestClient.get()
      .uri("/cases/unallocated/teamCount?teams=$teamCode")
      .headers { it.authToken(roles = listOf("ROLE_MANAGE_A_WORKFORCE_ALLOCATE")) }
      .exchange()
      .expectStatus()
      .isOk
      .expectBody()
      .jsonPath("$[0].teamCode")
      .isEqualTo(teamCode)
      .jsonPath("$.[0].caseCount")
      .isEqualTo(3)
  }

  @Test
  fun `must be able to handle multiple team codes`() {
    val firstTeamCode = "TEAM1"
    val secondTeamCode = "TEAM2"
    insertCases()
    webTestClient.get()
      .uri("/cases/unallocated/teamCount?teams=$firstTeamCode,$secondTeamCode")
      .headers { it.authToken(roles = listOf("ROLE_MANAGE_A_WORKFORCE_ALLOCATE")) }
      .exchange()
      .expectStatus()
      .isOk
      .expectBody()
      .jsonPath("$.[?(@.teamCode=='$firstTeamCode')].caseCount")
      .isEqualTo(3)
      .jsonPath("$.[?(@.teamCode=='$secondTeamCode')].caseCount")
      .isEqualTo(1)
  }

  @Test
  fun `must return forbidden when auth token does not contain correct role`() {
    webTestClient.get()
      .uri("/cases/unallocated/teamCount?teams=TEAM1")
      .headers { it.authToken(roles = listOf("ROLE_RANDOM_ROLE")) }
      .exchange()
      .expectStatus()
      .isForbidden
  }
}
