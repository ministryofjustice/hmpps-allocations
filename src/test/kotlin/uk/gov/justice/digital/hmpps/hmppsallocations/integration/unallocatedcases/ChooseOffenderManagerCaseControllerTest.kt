package uk.gov.justice.digital.hmpps.hmppsallocations.integration.unallocatedcases

import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.hmppsallocations.integration.IntegrationTestBase

class ChooseOffenderManagerCaseControllerTest : IntegrationTestBase() {

  @Test
  fun `get previously managed case with no offender manager`() {
    val crn = "J680660"
    val convictionNumber = 4
    insertCases()

    webTestClient.get()
      .uri("/cases/unallocated/$crn/convictions/$convictionNumber/practitionerCase")
      .headers { it.authToken(roles = listOf("ROLE_MANAGE_A_WORKFORCE_ALLOCATE")) }
      .exchange()
      .expectStatus()
      .isOk
      .expectBody()
      .jsonPath("$.name")
      .isEqualTo("Hannah Francis")
      .jsonPath("$.crn")
      .isEqualTo(crn)
      .jsonPath("$.tier")
      .isEqualTo("C2")
      .jsonPath("$.status")
      .isEqualTo("Previously managed")
      .jsonPath("$.offenderManager")
      .doesNotExist()
      .jsonPath("$.convictionNumber")
      .isEqualTo(convictionNumber)
  }

  @Test
  fun `get case with offender manager`() {
    val crn = "J678910"
    val convictionNumber = 1
    insertCases()

    webTestClient.get()
      .uri("/cases/unallocated/$crn/convictions/$convictionNumber/practitionerCase")
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
  }

  @Test
  fun `get 404 if crn not found`() {
    webTestClient.get()
      .uri("/cases/unallocated/J678912/convictions/1/practitionerCase")
      .headers { it.authToken(roles = listOf("ROLE_MANAGE_A_WORKFORCE_ALLOCATE")) }
      .exchange()
      .expectStatus()
      .isNotFound
  }
}
