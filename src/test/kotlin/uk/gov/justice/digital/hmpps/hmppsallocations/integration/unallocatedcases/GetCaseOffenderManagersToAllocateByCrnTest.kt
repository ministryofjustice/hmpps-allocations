package uk.gov.justice.digital.hmpps.hmppsallocations.integration.unallocatedcases

import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.hmppsallocations.integration.IntegrationTestBase

class GetCaseOffenderManagersToAllocateByCrnTest : IntegrationTestBase() {

  @Test
  fun `can get Offender Managers to allocate by crn`() {
    val crn = "J678910"
    val convictionId = 123456789L
    insertCases()
    getOffenderManagersToAllocateForCrn()
    webTestClient.get()
      .uri("/cases/$crn/convictions/$convictionId/allocate/offenderManagers")
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
      .jsonPath("$.status")
      .isEqualTo("Currently managed")
      .jsonPath("$.offenderManager.forenames")
      .isEqualTo("Antonio")
      .jsonPath("$.offenderManager.surname")
      .isEqualTo("LoSardo")
      .jsonPath("$.offenderManager.grade")
      .isEqualTo("PO")
      .jsonPath("$.offenderManagersToAllocate[0].forename")
      .isEqualTo("Ben")
      .jsonPath("$.offenderManagersToAllocate[0].surname")
      .isEqualTo("Doe")
      .jsonPath("$.offenderManagersToAllocate[0].grade")
      .isEqualTo("PO")
      .jsonPath("$.offenderManagersToAllocate[0].totalCommunityCases")
      .isEqualTo(15)
      .jsonPath("$.offenderManagersToAllocate[0].totalCustodyCases")
      .isEqualTo(20)
      .jsonPath("$.offenderManagersToAllocate[0].capacity")
      .isEqualTo(0.5)
      .jsonPath("$.offenderManagersToAllocate[0].code")
      .isEqualTo("OM1")
      .jsonPath("$.convictionId")
      .isEqualTo(convictionId)
      .jsonPath("$.caseType")
      .isEqualTo("CUSTODY")
  }
}
