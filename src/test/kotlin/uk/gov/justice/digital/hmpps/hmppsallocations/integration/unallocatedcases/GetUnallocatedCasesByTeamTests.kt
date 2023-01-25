package uk.gov.justice.digital.hmpps.hmppsallocations.integration.unallocatedcases

import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus
import uk.gov.justice.digital.hmpps.hmppsallocations.client.CommunityPersonManager
import uk.gov.justice.digital.hmpps.hmppsallocations.client.Name
import uk.gov.justice.digital.hmpps.hmppsallocations.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.hmppsallocations.integration.domain.CaseDetailsIntegration
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class GetUnallocatedCasesByTeamTests : IntegrationTestBase() {
  @Test
  fun `Get unallocated cases by team`() {
    insertCases()
    val initialAppointment = LocalDate.of(2022, 10, 11)
    val firstSentenceDate = LocalDate.of(2022, 11, 5)
    setupTeam1CaseDetails()
    webTestClient.get()
      .uri("/team/TEAM1/cases/unallocated")
      .headers { it.authToken(roles = listOf("ROLE_MANAGE_A_WORKFORCE_ALLOCATE")) }
      .exchange()
      .expectStatus()
      .isOk
      .expectBody()
      .jsonPath("$.length()")
      .isEqualTo(5)
      .jsonPath("$.[?(@.convictionNumber == 1 && @.crn == 'J678910')].sentenceDate")
      .isEqualTo(firstSentenceDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")))
      .jsonPath("$.[?(@.convictionNumber == 1 && @.crn == 'J678910')].initialAppointment")
      .isEqualTo(initialAppointment.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")))
      .jsonPath("$.[?(@.convictionNumber == 1 && @.crn == 'J678910')].name")
      .isEqualTo("Dylan Adam Armstrong")
      .jsonPath("$.[?(@.convictionNumber == 1 && @.crn == 'J678910')].crn")
      .isEqualTo("J678910")
      .jsonPath("$.[?(@.convictionNumber == 1 && @.crn == 'J678910')].tier")
      .isEqualTo("C1")
      .jsonPath("$.[?(@.convictionNumber == 1 && @.crn == 'J678910')].status")
      .isEqualTo("Currently managed")
      .jsonPath("$.[?(@.convictionNumber == 1 && @.crn == 'J678910')].offenderManager.forenames")
      .isEqualTo("Beverley")
      .jsonPath("$.[?(@.convictionNumber == 1 && @.crn == 'J678910')].offenderManager.surname")
      .isEqualTo("Smith")
      .jsonPath("$.[?(@.convictionNumber == 1 && @.crn == 'J678910')].offenderManager.grade")
      .isEqualTo("SPO")
      .jsonPath("$.[?(@.convictionNumber == 1 && @.crn == 'J678910')].caseType")
      .isEqualTo("CUSTODY")
      .jsonPath("$.[?(@.convictionNumber == 2 && @.crn == 'J680648')].status")
      .isEqualTo("Previously managed")
      .jsonPath("$.[?(@.convictionNumber == 2 && @.crn == 'J680648')].initialAppointment")
      .isEqualTo(null)
      .jsonPath("$.[?(@.convictionNumber == 2 && @.crn == 'J680648')].offenderManager.forenames")
      .isEqualTo("Janie")
      .jsonPath("$.[?(@.convictionNumber == 2 && @.crn == 'J680648')].offenderManager.surname")
      .isEqualTo("Jones")
      .jsonPath("$.[?(@.convictionNumber == 2 && @.crn == 'J680648')].offenderManager.grade")
      .doesNotExist()
      .jsonPath("$.[?(@.convictionNumber == 6 && @.crn == 'C3333333')].status")
      .isEqualTo("Currently managed")
      .jsonPath("$.[?(@.convictionNumber == 6 && @.crn == 'C3333333')].offenderManager.forenames")
      .isEqualTo("John")
      .jsonPath("$.[?(@.convictionNumber == 6 && @.crn == 'C3333333')].offenderManager.surname")
      .isEqualTo("Brown")
      .jsonPath("$.[?(@.convictionNumber == 6 && @.crn == 'C3333333')].offenderManager.grade")
      .doesNotExist()
      .jsonPath("$.[?(@.convictionNumber == 4 && @.crn == 'J680660')].offenderManager")
      .doesNotExist()
      .jsonPath("$.[?(@.convictionNumber == 4 && @.crn == 'J680660')].status")
      .isEqualTo("Previously managed")
      .jsonPath("$.[?(@.convictionNumber == 3 && @.crn == 'X4565764')].status")
      .isEqualTo("New to probation")
      .jsonPath("$.[?(@.convictionNumber == 3 && @.crn == 'X4565764')].offenderManager")
      .doesNotExist()
  }

  @Test
  fun `return error when error on API call`() {
    insertCases()
    errorDeliusCaseDetailsResponse()
    webTestClient.get()
      .uri("/team/TEAM1/cases/unallocated")
      .headers { it.authToken(roles = listOf("ROLE_MANAGE_A_WORKFORCE_ALLOCATE")) }
      .exchange()
      .expectStatus()
      .is5xxServerError
  }

  @Test
  fun `cannot get unallocated cases by team when no auth token supplied`() {
    webTestClient.get()
      .uri("/team/TEAM1/cases/unallocated")
      .exchange()
      .expectStatus()
      .isUnauthorized
  }

  @Test
  fun `not found`() {
    webTestClient.post()
      .uri("/cases/someotherurl")
      .headers { it.authToken(roles = listOf("ROLE_QUEUE_WORKLOAD_ADMIN")) }
      .exchange()
      .expectStatus()
      .isEqualTo(HttpStatus.NOT_FOUND)
  }

  @Test
  fun `method not allowed`() {
    webTestClient.post()
      .uri("/team/TEAM1/cases/unallocated")
      .headers { it.authToken(roles = listOf("ROLE_QUEUE_WORKLOAD_ADMIN")) }
      .exchange()
      .expectStatus()
      .isEqualTo(HttpStatus.METHOD_NOT_ALLOWED)
  }

  @Test
  fun `cannot get unallocated cases when no auth token supplied`() {
    webTestClient.get()
      .uri("/team/TEAM1/cases/unallocated")
      .exchange()
      .expectStatus()
      .isUnauthorized
  }

  @Test
  fun `must return sentence length`() {
    setupTeam1CaseDetails()

    insertCases()
    webTestClient.get()
      .uri("/team/TEAM1/cases/unallocated")
      .headers { it.authToken(roles = listOf("ROLE_MANAGE_A_WORKFORCE_ALLOCATE")) }
      .exchange()
      .expectStatus()
      .isOk
      .expectBody()
      .jsonPath("$.[?(@.convictionNumber == 1 && @.crn == 'J678910')].sentenceLength")
      .isEqualTo("5 Weeks")
  }

  fun setupTeam1CaseDetails() = deliusCaseDetailsResponse(
    CaseDetailsIntegration(
      "J678910",
      "1",
      LocalDate.of(2022, 10, 11),
      "Currently managed",
      CommunityPersonManager(Name("Beverley", null, "Smith"), "SPO")
    ),
    CaseDetailsIntegration(
      "J680648", "2", null, "Previously managed", CommunityPersonManager(Name("Janie", null, "Jones"), "PO")
    ),
    CaseDetailsIntegration(
      "X4565764", "3", LocalDate.now(), "New to probation", CommunityPersonManager(Name("Beverley", null, "Smith"), "SPO")
    ),
    CaseDetailsIntegration(
      "J680660", "4", LocalDate.now(), "Previously managed", null
    ),
    CaseDetailsIntegration(
      "C3333333",
      "6",
      LocalDate.of(2022, 10, 11),
      "Currently managed",
      CommunityPersonManager(Name("John", null, "Brown"), null)
    ),
  )
}
