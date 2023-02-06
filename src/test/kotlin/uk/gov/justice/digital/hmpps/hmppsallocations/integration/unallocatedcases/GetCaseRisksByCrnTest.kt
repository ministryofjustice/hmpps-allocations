package uk.gov.justice.digital.hmpps.hmppsallocations.integration.unallocatedcases

import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.hmppsallocations.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.hmppsallocations.integration.mockserver.AssessRisksNeedsApiExtension.Companion.assessRisksNeedsApi
import uk.gov.justice.digital.hmpps.hmppsallocations.integration.mockserver.CommunityApiExtension.Companion.communityApi

class GetCaseRisksByCrnTest : IntegrationTestBase() {

  @Test
  fun `can get case risks by crn and convictionNUmber`() {
    val crn = "J678910"
    val convictionNumber = 1
    insertCases()
    communityApi.getRegistrationsFromDelius(crn)
    assessRisksNeedsApi.getRoshForCrn(crn)
    assessRisksNeedsApi.getRiskPredictorsForCrn(crn)
    communityApi.getOgrsForCrn(crn)
    webTestClient.get()
      .uri("/cases/unallocated/$crn/convictions/$convictionNumber/risks")
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
      .jsonPath("$.activeRegistrations[0].notes")
      .doesNotExist()
      .jsonPath("$.activeRegistrations[0].endDate")
      .doesNotExist()
      .jsonPath("$.inactiveRegistrations[0].type")
      .isEqualTo("Child Protection")
      .jsonPath("$.inactiveRegistrations[0].registered")
      .isEqualTo("2021-05-20")
      .jsonPath("$.inactiveRegistrations[0].notes")
      .isEqualTo("Some Notes.")
      .jsonPath("$.inactiveRegistrations[0].endDate")
      .isEqualTo("2021-08-30")
      .jsonPath("$.roshRisk.overallRisk")
      .isEqualTo("VERY_HIGH")
      .jsonPath("$.roshRisk.assessedOn")
      .isEqualTo("2022-10-07")
      .jsonPath("$.roshRisk.riskInCommunity.Children")
      .isEqualTo("LOW")
      .jsonPath("$.roshRisk.riskInCommunity.Public")
      .isEqualTo("HIGH")
      .jsonPath("$.roshRisk.riskInCommunity.['Known Adult']")
      .isEqualTo("MEDIUM")
      .jsonPath("$.roshRisk.riskInCommunity.['Staff']")
      .isEqualTo("VERY_HIGH")
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
      .jsonPath("$.convictionNumber")
      .isEqualTo(1)
  }

  @Test
  fun `get case risks with no registrations`() {
    val crn = "J678910"
    val convictionNumber = 1
    insertCases()
    communityApi.noRegistrationsFromDelius(crn)
    assessRisksNeedsApi.getRoshForCrn(crn)
    assessRisksNeedsApi.getRiskPredictorsForCrn(crn)
    communityApi.getOgrsForCrn(crn)
    webTestClient.get()
      .uri("/cases/unallocated/$crn/convictions/$convictionNumber/risks")
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
    val convictionNumber = 1
    insertCases()
    communityApi.noRegistrationsFromDelius(crn)
    assessRisksNeedsApi.getRoshNotFoundForCrn(crn)
    assessRisksNeedsApi.getRiskPredictorsForCrn(crn)
    communityApi.getOgrsForCrn(crn)
    webTestClient.get()
      .uri("/cases/unallocated/$crn/convictions/$convictionNumber/risks")
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
    val convictionNumber = 1
    insertCases()
    assessRisksNeedsApi.getRoshForCrn(crn)
    communityApi.noRegistrationsFromDelius(crn)
    assessRisksNeedsApi.notFoundOgrsForCrn(crn)
    assessRisksNeedsApi.getRiskPredictorsForCrn(crn)
    webTestClient.get()
      .uri("/cases/unallocated/$crn/convictions/$convictionNumber/risks")
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
    val convictionNumber = 1
    insertCases()
    communityApi.noRegistrationsFromDelius(crn)
    assessRisksNeedsApi.getRoshNoLevelForCrn(crn)
    assessRisksNeedsApi.getRiskPredictorsForCrn(crn)
    communityApi.getOgrsForCrn(crn)
    webTestClient.get()
      .uri("/cases/unallocated/$crn/convictions/$convictionNumber/risks")
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
    val convictionNumber = 1
    insertCases()
    communityApi.noRegistrationsFromDelius(crn)
    assessRisksNeedsApi.getRoshForCrn(crn)
    communityApi.getOgrsForCrn(crn)
    webTestClient.get()
      .uri("/cases/unallocated/$crn/convictions/$convictionNumber/risks")
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
  fun `can get case risks when no Rosh nor Rsr are returned`() {
    val crn = "J678910"
    val convictionNumber = 1
    insertCases()
    communityApi.noRegistrationsFromDelius(crn)
    assessRisksNeedsApi.getRoshForCrn(crn)
    communityApi.getOgrsForCrn(crn)
    workforceAllocationsToDelius.riskResponseNoRoshNoRsr(crn)
    webTestClient.get()
      .uri("/cases/unallocated/$crn/convictions/$convictionNumber/risks")
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
