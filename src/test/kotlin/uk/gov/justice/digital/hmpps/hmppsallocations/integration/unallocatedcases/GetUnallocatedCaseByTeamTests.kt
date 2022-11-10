package uk.gov.justice.digital.hmpps.hmppsallocations.integration.unallocatedcases

import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus
import uk.gov.justice.digital.hmpps.hmppsallocations.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.hmppsallocations.integration.domain.CaseDetailsInitialAppointment
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class GetUnallocatedCaseByTeamTests : IntegrationTestBase() {
  @Test
  fun `Get unallocated cases by team`() {
    insertCases()
    val initialAppointment = LocalDate.of(2022, 10, 11)
    val firstSentenceDate = LocalDate.of(2022, 11, 5)
    deliusCaseDetailsResponse(
      CaseDetailsInitialAppointment("J678910", "1", initialAppointment),
      CaseDetailsInitialAppointment("J680648", "2", LocalDate.now()),
      CaseDetailsInitialAppointment("X4565764", "3", LocalDate.now()),
      CaseDetailsInitialAppointment("J680660", "4", LocalDate.now()),
      CaseDetailsInitialAppointment("J678910", "5", LocalDate.now()),
      CaseDetailsInitialAppointment("C3333333", "6", LocalDate.now())
    )
    webTestClient.get()
      .uri("/team/TEAM1/cases/unallocated")
      .headers { it.authToken(roles = listOf("ROLE_MANAGE_A_WORKFORCE_ALLOCATE")) }
      .exchange()
      .expectStatus()
      .isOk
      .expectBody()
      .jsonPath("$.length()")
      .isEqualTo(4)
      .jsonPath("$.[0].sentenceDate")
      .isEqualTo(firstSentenceDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")))
      .jsonPath("$.[0].initialAppointment")
      .isEqualTo(initialAppointment.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")))
      .jsonPath("$.[0].name")
      .isEqualTo("Dylan Adam Armstrong")
      .jsonPath("$.[0].crn")
      .isEqualTo("J678910")
      .jsonPath("$.[0].tier")
      .isEqualTo("C1")
      .jsonPath("$.[0].status")
      .isEqualTo("Currently managed")
      .jsonPath("$.[0].offenderManager.forenames")
      .isEqualTo("Antonio")
      .jsonPath("$.[0].offenderManager.surname")
      .isEqualTo("LoSardo")
      .jsonPath("$.[0].offenderManager.grade")
      .isEqualTo("PO")
      .jsonPath("$.[0].convictionId")
      .isEqualTo(123456789)
      .jsonPath("$.[0].caseType")
      .isEqualTo("CUSTODY")
  }

  @Test
  fun `must get initial appointments for all required`() {
    insertCases()
    val initialAppointment = LocalDate.of(2021, 11, 30)
    deliusCaseDetailsResponse(
      CaseDetailsInitialAppointment("J678910", "1", LocalDate.now()),
      CaseDetailsInitialAppointment("J680648", "2", null),
      CaseDetailsInitialAppointment("X4565764", "3", initialAppointment),
      CaseDetailsInitialAppointment("J680660", "4", LocalDate.now()),
      CaseDetailsInitialAppointment("J678910", "5", LocalDate.now()),
      CaseDetailsInitialAppointment("C3333333", "6", LocalDate.now())
    )
    webTestClient.get()
      .uri("/team/TEAM1/cases/unallocated")
      .headers { it.authToken(roles = listOf("ROLE_MANAGE_A_WORKFORCE_ALLOCATE")) }
      .exchange()
      .expectStatus()
      .isOk
      .expectBody()
      .jsonPath("$.[?(@.convictionId == 68793954)].initialAppointment")
      .isEqualTo(initialAppointment.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")))
      .jsonPath("$.[?(@.convictionId == 23456789)].initialAppointment")
      .isEqualTo(null)
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
  fun `can get previous conviction end date`() {
    repository.save(previouslyManagedCase)
    deliusCaseDetailsResponse(CaseDetailsInitialAppointment("J680660", "4", LocalDate.now()))
    webTestClient.get()
      .uri("/team/TEAM1/cases/unallocated")
      .headers { it.authToken(roles = listOf("ROLE_MANAGE_A_WORKFORCE_ALLOCATE")) }
      .exchange()
      .expectStatus()
      .isOk
      .expectBody()
      .jsonPath("$.[0].status")
      .isEqualTo(previouslyManagedCase.status)
      .jsonPath("$.[0].previousConvictionEndDate")
      .isEqualTo((previouslyManagedCase.previousConvictionDate!!.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))))
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
    deliusCaseDetailsResponse(
      CaseDetailsInitialAppointment("J678910", "1", LocalDate.now()),
      CaseDetailsInitialAppointment("J680648", "2", LocalDate.now()),
      CaseDetailsInitialAppointment("X4565764", "3", LocalDate.now()),
      CaseDetailsInitialAppointment("J680660", "4", LocalDate.now()),
      CaseDetailsInitialAppointment("J678910", "5", LocalDate.now()),
      CaseDetailsInitialAppointment("C3333333", "6", LocalDate.now())
    )

    insertCases()
    webTestClient.get()
      .uri("/team/TEAM1/cases/unallocated")
      .headers { it.authToken(roles = listOf("ROLE_MANAGE_A_WORKFORCE_ALLOCATE")) }
      .exchange()
      .expectStatus()
      .isOk
      .expectBody()
      .jsonPath("$.[?(@.convictionId == 123456789)].sentenceLength")
      .isEqualTo("5 Weeks")
  }
}
