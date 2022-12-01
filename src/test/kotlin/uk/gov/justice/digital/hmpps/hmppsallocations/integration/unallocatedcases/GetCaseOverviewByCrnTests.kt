package uk.gov.justice.digital.hmpps.hmppsallocations.integration.unallocatedcases

import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.hmppsallocations.integration.IntegrationTestBase
import java.time.format.DateTimeFormatter

class GetCaseOverviewByCrnTests : IntegrationTestBase() {

  @Test
  fun `can get case overview by crn and convictionId`() {
    val crn = "J678910"
    val convictionId = 123456789L
    insertCases()

    webTestClient.get()
      .uri("/cases/unallocated/$crn/convictions/$convictionId/overview")
      .headers { it.authToken(roles = listOf("ROLE_MANAGE_A_WORKFORCE_ALLOCATE")) }
      .exchange()
      .expectStatus()
      .isOk
      .expectBody()
      .jsonPath("$.sentenceDate")
      .isEqualTo(firstSentenceDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")))
      .jsonPath("$.initialAppointment")
      .isEqualTo(firstInitialAppointment.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")))
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
      .jsonPath("$.convictionId")
      .isEqualTo(convictionId)
      .jsonPath("$.caseType")
      .isEqualTo("CUSTODY")
      .jsonPath("$.convictionNumber")
      .isEqualTo(1)
  }

  @Test
  fun `can get case overview by crn and convictionNumber`() {
    val crn = "J678910"
    val convictionId = 123456789L
    insertCases()

    webTestClient.get()
      .uri("/cases/unallocated/$crn/convictions/1/overview")
      .headers { it.authToken(roles = listOf("ROLE_MANAGE_A_WORKFORCE_ALLOCATE")) }
      .exchange()
      .expectStatus()
      .isOk
      .expectBody()
      .jsonPath("$.sentenceDate")
      .isEqualTo(firstSentenceDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")))
      .jsonPath("$.initialAppointment")
      .isEqualTo(firstInitialAppointment.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")))
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
      .jsonPath("$.convictionId")
      .isEqualTo(convictionId)
      .jsonPath("$.caseType")
      .isEqualTo("CUSTODY")
      .jsonPath("$.convictionNumber")
      .isEqualTo(1)
  }

  @Test
  fun `get 404 if crn not found`() {
    webTestClient.get()
      .uri("/cases/unallocated/J678912/convictions/51245325/overview")
      .headers { it.authToken(roles = listOf("ROLE_MANAGE_A_WORKFORCE_ALLOCATE")) }
      .exchange()
      .expectStatus()
      .isNotFound
  }
}
