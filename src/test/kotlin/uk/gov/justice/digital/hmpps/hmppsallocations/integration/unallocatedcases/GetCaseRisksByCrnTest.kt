package uk.gov.justice.digital.hmpps.hmppsallocations.integration.unallocatedcases

import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.hmppsallocations.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.hmppsallocations.integration.mockserver.AssessRisksNeedsApiExtension.Companion.assessRisksNeedsApi
import uk.gov.justice.digital.hmpps.hmppsallocations.integration.mockserver.WorkforceAllocationsToDeliusApiExtension.Companion.workforceAllocationsToDelius
import java.math.BigDecimal

class GetCaseRisksByCrnTest : IntegrationTestBase() {

  @Test
  fun `can get case risks by crn and convictionNUmber`() {
    val crn = "J678910"
    val convictionNumber = 1
    workforceAllocationsToDelius.userHasAccess("J678910")
    insertCases()
    assessRisksNeedsApi.getRoshForCrn(crn)
    assessRisksNeedsApi.getRiskPredictorsForCrn(crn)
    workforceAllocationsToDelius.riskResponse(crn)
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
      .isEqualTo("Some Notes")
      .jsonPath("$.activeRegistrations[0].endDate")
      .doesNotExist()
      .jsonPath("$.activeRegistrations[0].flag.description")
      .isEqualTo("MAPPA Risk")
      .jsonPath("$.inactiveRegistrations[0].type")
      .isEqualTo("Child Protection")
      .jsonPath("$.inactiveRegistrations[0].registered")
      .isEqualTo("2021-05-20")
      .jsonPath("$.inactiveRegistrations[0].notes")
      .isEqualTo("Some Notes.")
      .jsonPath("$.inactiveRegistrations[0].endDate")
      .isEqualTo("2021-08-30")
      .jsonPath("$.inactiveRegistrations[0].flag.description")
      .isEqualTo("Child Protection Flag")
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
    workforceAllocationsToDelius.userHasAccess("J678910")
    insertCases()
    assessRisksNeedsApi.getRoshForCrn(crn)
    assessRisksNeedsApi.getRiskPredictorsForCrn(crn)
    workforceAllocationsToDelius.riskResponseNoRegistrationsNoOgrs(crn)
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
    workforceAllocationsToDelius.userHasAccess("J678910")
    insertCases()
    assessRisksNeedsApi.getRoshNotFoundForCrn(crn)
    assessRisksNeedsApi.getRiskPredictorsForCrn(crn)
    workforceAllocationsToDelius.riskResponse(crn)
    webTestClient.get()
      .uri("/cases/unallocated/$crn/convictions/$convictionNumber/risks")
      .headers { it.authToken(roles = listOf("ROLE_MANAGE_A_WORKFORCE_ALLOCATE")) }
      .exchange()
      .expectStatus()
      .isOk
      .expectBody()
      .jsonPath("$.roshRisk.overallRisk")
      .isEqualTo("NOT_FOUND")

    assessRisksNeedsApi.verifyRoshCalled(crn, 1)
    assessRisksNeedsApi.verifyRiskPredictorCalled(crn, 1)
  }

  @Test
  fun `get case risks with no ROSH summary after retry`() {
    val crn = "J678910"
    val convictionNumber = 1
    workforceAllocationsToDelius.userHasAccess("J678910")
    insertCases()
    assessRisksNeedsApi.getRoshNotFoundForCrnRetry(crn)
    assessRisksNeedsApi.getRiskPredictorsForCrn(crn)
    workforceAllocationsToDelius.riskResponse(crn)
    webTestClient.get()
      .uri("/cases/unallocated/$crn/convictions/$convictionNumber/risks")
      .headers { it.authToken(roles = listOf("ROLE_MANAGE_A_WORKFORCE_ALLOCATE")) }
      .exchange()
      .expectStatus()
      .isOk
      .expectBody()
      .jsonPath("$.roshRisk.overallRisk")
      .isEqualTo("NOT_FOUND")

    assessRisksNeedsApi.verifyRoshCalled(crn, 2)
    assessRisksNeedsApi.verifyRiskPredictorCalled(crn, 1)
  }

  @Test
  fun `get case risks with ROSH summary unavailable`() {
    val crn = "J678910"
    val convictionNumber = 1
    workforceAllocationsToDelius.userHasAccess("J678910")
    insertCases()
    assessRisksNeedsApi.getRoshUnavailableForCrn(crn)
    assessRisksNeedsApi.getRiskPredictorsForCrn(crn)
    workforceAllocationsToDelius.riskResponse(crn)
    webTestClient.get()
      .uri("/cases/unallocated/$crn/convictions/$convictionNumber/risks")
      .headers { it.authToken(roles = listOf("ROLE_MANAGE_A_WORKFORCE_ALLOCATE")) }
      .exchange()
      .expectStatus()
      .isOk
      .expectBody()
      .jsonPath("$.roshRisk.overallRisk")
      .isEqualTo("UNAVAILABLE")
    assessRisksNeedsApi.verifyRoshCalled(crn, 4)
    assessRisksNeedsApi.verifyRiskPredictorCalled(crn, 1)
  }

  @Test
  fun `get case risks with no RSR`() {
    val crn = "J678910"
    val convictionNumber = 1
    workforceAllocationsToDelius.userHasAccess("J678910")
    insertCases()
    assessRisksNeedsApi.getRoshForCrn(crn)
    assessRisksNeedsApi.getRiskPredictorsNotFoundForCrn(crn)
    workforceAllocationsToDelius.riskResponse(crn)
    webTestClient.get()
      .uri("/cases/unallocated/$crn/convictions/$convictionNumber/risks")
      .headers { it.authToken(roles = listOf("ROLE_MANAGE_A_WORKFORCE_ALLOCATE")) }
      .exchange()
      .expectStatus()
      .isOk
      .expectBody()
      .jsonPath("$.rsr.level")
      .isEqualTo("NOT_FOUND")
      .jsonPath("$.rsr.percentage")
      .isEqualTo(BigDecimal(Int.MIN_VALUE))
    assessRisksNeedsApi.verifyRoshCalled(crn, 1)
    assessRisksNeedsApi.verifyRiskPredictorCalled(crn, 1)
  }

  @Test
  fun `get case risks with unavailable RSR`() {
    val crn = "J678910"
    val convictionNumber = 1
    workforceAllocationsToDelius.userHasAccess("J678910")
    insertCases()
    assessRisksNeedsApi.getRoshForCrn(crn)
    assessRisksNeedsApi.getRiskPredictorsUnavailableForCrn(crn)
    workforceAllocationsToDelius.riskResponse(crn)
    webTestClient.get()
      .uri("/cases/unallocated/$crn/convictions/$convictionNumber/risks")
      .headers { it.authToken(roles = listOf("ROLE_MANAGE_A_WORKFORCE_ALLOCATE")) }
      .exchange()
      .expectStatus()
      .isOk
      .expectBody()
      .jsonPath("$.rsr.level")
      .isEqualTo("UNAVAILABLE")
      .jsonPath("$.rsr.percentage")
      .isEqualTo(BigDecimal(Int.MIN_VALUE))
    assessRisksNeedsApi.verifyRoshCalled(crn, 1)
    assessRisksNeedsApi.verifyRiskPredictorCalled(crn, 4)
  }

  @Test
  fun `get case risks with Empty List RSR`() {
    val crn = "J678910"
    val convictionNumber = 1
    workforceAllocationsToDelius.userHasAccess("J678910")
    insertCases()
    assessRisksNeedsApi.getRoshForCrn(crn)
    assessRisksNeedsApi.getRiskPredictorsForCrnEmptyList(crn)
    workforceAllocationsToDelius.riskResponse(crn)
    webTestClient.get()
      .uri("/cases/unallocated/$crn/convictions/$convictionNumber/risks")
      .headers { it.authToken(roles = listOf("ROLE_MANAGE_A_WORKFORCE_ALLOCATE")) }
      .exchange()
      .expectStatus()
      .isOk
      .expectBody()
      .jsonPath("$.rsr.level")
      .isEqualTo("NOT_FOUND")
      .jsonPath("$.rsr.percentage")
      .isEqualTo(BigDecimal(Int.MIN_VALUE))
    assessRisksNeedsApi.verifyRoshCalled(crn, 1)
  }

  @Test
  fun `get case risks with no ogrs`() {
    val crn = "J678910"
    val convictionNumber = 1
    workforceAllocationsToDelius.userHasAccess("J678910")
    insertCases()
    assessRisksNeedsApi.getRoshForCrn(crn)
    assessRisksNeedsApi.getRiskPredictorsForCrn(crn)
    workforceAllocationsToDelius.riskResponseNoRegistrationsNoOgrs(crn)
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
    workforceAllocationsToDelius.userHasAccess("J678910")
    insertCases()
    assessRisksNeedsApi.getRoshNoLevelForCrn(crn)
    assessRisksNeedsApi.getRiskPredictorsForCrn(crn)
    workforceAllocationsToDelius.riskResponse(crn)
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
    workforceAllocationsToDelius.userHasAccess("J678910")
    insertCases()
    assessRisksNeedsApi.getRoshForCrn(crn)
    workforceAllocationsToDelius.riskResponseNoRegistrationsNoOgrs(crn)
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
  fun `getting case risks when not in allocation demand returns not found`() {
    workforceAllocationsToDelius.userHasAccess("CRN12345")
    webTestClient.get()
      .uri("/cases/unallocated/CRN12345/convictions/6/risks")
      .headers { it.authToken(roles = listOf("ROLE_MANAGE_A_WORKFORCE_ALLOCATE")) }
      .exchange()
      .expectStatus()
      .isNotFound
  }

  @Test
  fun `getting case risks when crn is restricted or excluded`() {
    webTestClient.get()
      .uri("/cases/unallocated/CRN12345/convictions/6/risks")
      .headers { it.authToken(roles = listOf("ROLE_MANAGE_A_WORKFORCE_ALLOCATE")) }
      .exchange()
      .expectStatus()
      .isNotFound
  }
}
