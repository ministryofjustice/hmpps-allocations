package uk.gov.justice.digital.hmpps.hmppsallocations.integration.unallocatedcases

import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.hmppsallocations.integration.IntegrationTestBase

class GetCaseAllocateImpactByCrnTest : IntegrationTestBase() {

  @Test
  fun `can get impact to Offender Manager when allocate by crn`() {
    val crn = "J678910"
    val convictionId = 123456789L
    val offenderManagerCode = "OM1"
    insertCases()
    singleActiveConvictionResponse(crn)
    getImpactToOffenderManagerWhenAllocatingForCrn(crn, offenderManagerCode)
    webTestClient.get()
      .uri("/cases/$crn/convictions/$convictionId/allocate/$offenderManagerCode/impact")
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
      .jsonPath("$.offenderManagerForename")
      .isEqualTo("John")
      .jsonPath("$.offenderManagerSurname")
      .isEqualTo("Smith")
      .jsonPath("$.offenderManagerGrade")
      .isEqualTo("PO")
      .jsonPath("$.offenderManagerCurrentCapacity")
      .isEqualTo(56.7)
      .jsonPath("$.offenderManagerCode")
      .isEqualTo("OM1")
      .jsonPath("$.offenderManagerPotentialCapacity")
      .isEqualTo(87.2)
      .jsonPath("$.convictionId")
      .isEqualTo(convictionId)
  }
}
