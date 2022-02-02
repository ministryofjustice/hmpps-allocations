package uk.gov.justice.digital.hmpps.hmppsallocations.integration.unallocatedcases

import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.hmppsallocations.integration.IntegrationTestBase

class GetCaseRisksByCrnTest : IntegrationTestBase() {

  @Test
  fun `can get case risks`() {
    val crn = "J678910"
    insertCases()
    getRegistrationsFromDelius(crn)
    getRiskSummaryForCrn(crn)
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
      .jsonPath("$.activeRegistrations[0].type")
      .isEqualTo("ALT Under MAPPA Arrangements")
      .jsonPath("$.activeRegistrations[0].registered")
      .isEqualTo("2021-08-30")
      .jsonPath("$.activeRegistrations[0].nextReviewDate")
      .doesNotExist()
      .jsonPath("$.activeRegistrations[0].notes")
      .doesNotExist()
      .jsonPath("$.activeRegistrations[0].endDate")
      .doesNotExist()
      .jsonPath("$.inactiveRegistrations[0].type")
      .isEqualTo("Child Protection")
      .jsonPath("$.inactiveRegistrations[0].registered")
      .isEqualTo("2021-05-20")
      .jsonPath("$.inactiveRegistrations[0].nextReviewDate")
      .doesNotExist()
      .jsonPath("$.inactiveRegistrations[0].notes")
      .isEqualTo("Some Notes.")
      .jsonPath("$.inactiveRegistrations[0].endDate")
      .isEqualTo("2021-08-30")
      .jsonPath("$.rosh.level")
      .isEqualTo("HIGH")
      .jsonPath("$.rosh.lastUpdatedOn")
      .isEqualTo("2022-02-02")
  }

  @Test
  fun `get case risks with no registrations`() {
    val crn = "J678910"
    insertCases()
    getNoRegistrationsFromDelius(crn)
    getRiskSummaryForCrn(crn)
    webTestClient.get()
      .uri("/cases/unallocated/$crn/risks")
      .headers { it.authToken(roles = listOf("ROLE_MANAGE_A_WORKFORCE_ALLOCATE")) }
      .exchange()
      .expectStatus()
      .isOk
      .expectBody()
      .jsonPath("$.activeRegistrations")
      .isEmpty
      .jsonPath("$.inactiveRegistrations")
      .isEmpty
  }

  @Test
  fun `get case risks with no ROSH summary`() {
    val crn = "J678910"
    insertCases()
    getNoRegistrationsFromDelius(crn)
    notFoundRiskSummaryForCrn(crn)
    webTestClient.get()
      .uri("/cases/unallocated/$crn/risks")
      .headers { it.authToken(roles = listOf("ROLE_MANAGE_A_WORKFORCE_ALLOCATE")) }
      .exchange()
      .expectStatus()
      .isOk
      .expectBody()
      .jsonPath("$.rosh")
      .doesNotExist()
  }
}
