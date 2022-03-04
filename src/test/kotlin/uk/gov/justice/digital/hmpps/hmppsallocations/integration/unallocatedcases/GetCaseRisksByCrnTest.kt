package uk.gov.justice.digital.hmpps.hmppsallocations.integration.unallocatedcases

import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.hmppsallocations.integration.IntegrationTestBase

class GetCaseRisksByCrnTest : IntegrationTestBase() {

  @Test
  fun `can get case risks`() {
    val crn = "J678910"
    val convictionId = 123456789L
    insertCases()
    getRegistrationsFromDelius(crn)
    getRiskSummaryForCrn(crn)
    getRiskPredictorsForCrn(crn)
    getOgrsForCrn(crn)
    webTestClient.get()
      .uri("/cases/unallocated/$crn/convictions/$convictionId/risks")
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
      .jsonPath("$.rsr.level")
      .isEqualTo("MEDIUM")
      .jsonPath("$.rsr.lastUpdatedOn")
      .isEqualTo("2019-02-12")
      .jsonPath("$.rsr.percentage")
      .isEqualTo(3.8)
      .jsonPath("$.ogrs.lastUpdatedOn")
      .isEqualTo("2018-11-17")
      .jsonPath("$.ogrs.score")
      .isEqualTo(85)
      .jsonPath("$.convictionId")
      .isEqualTo(convictionId)
      .jsonPath("$.caseType")
      .isEqualTo("CUSTODY")
  }

  @Test
  fun `get case risks with no registrations`() {
    val crn = "J678910"
    val convictionId = 123456789L
    insertCases()
    noRegistrationsFromDelius(crn)
    getRiskSummaryForCrn(crn)
    getRiskPredictorsForCrn(crn)
    getOgrsForCrn(crn)
    webTestClient.get()
      .uri("/cases/unallocated/$crn/convictions/$convictionId/risks")
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
    val convictionId = 123456789L
    insertCases()
    noRegistrationsFromDelius(crn)
    notFoundRiskSummaryForCrn(crn)
    getRiskPredictorsForCrn(crn)
    getOgrsForCrn(crn)
    webTestClient.get()
      .uri("/cases/unallocated/$crn/convictions/$convictionId/risks")
      .headers { it.authToken(roles = listOf("ROLE_MANAGE_A_WORKFORCE_ALLOCATE")) }
      .exchange()
      .expectStatus()
      .isOk
      .expectBody()
      .jsonPath("$.rosh")
      .doesNotExist()
  }

  @Test
  fun `get case risks with no ogrs`() {
    val crn = "J678910"
    val convictionId = 123456789L
    insertCases()
    noRegistrationsFromDelius(crn)
    notFoundOgrsForCrn(crn)
    getRiskPredictorsForCrn(crn)
    webTestClient.get()
      .uri("/cases/unallocated/$crn/convictions/$convictionId/risks")
      .headers { it.authToken(roles = listOf("ROLE_MANAGE_A_WORKFORCE_ALLOCATE")) }
      .exchange()
      .expectStatus()
      .isOk
      .expectBody()
      .jsonPath("$.ogrs")
      .doesNotExist()
  }

  @Test
  fun `get case risks with no ROSH level`() {
    val crn = "J678910"
    val convictionId = 123456789L
    insertCases()
    noRegistrationsFromDelius(crn)
    getRiskSummaryNoLevelForCrn(crn)
    getRiskPredictorsForCrn(crn)
    getOgrsForCrn(crn)
    webTestClient.get()
      .uri("/cases/unallocated/$crn/convictions/$convictionId/risks")
      .headers { it.authToken(roles = listOf("ROLE_MANAGE_A_WORKFORCE_ALLOCATE")) }
      .exchange()
      .expectStatus()
      .isOk
      .expectBody()
      .jsonPath("$.rosh")
      .doesNotExist()
  }

  @Test
  fun `can get case risks when no registrations are returned`() {
    val crn = "J678910"
    val convictionId = 123456789L
    insertCases()
    noRegistrationsFromDelius(crn)
    getOgrsForCrn(crn)
    webTestClient.get()
      .uri("/cases/unallocated/$crn/convictions/$convictionId/risks")
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
}
