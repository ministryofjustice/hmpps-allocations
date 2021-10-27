package uk.gov.justice.digital.hmpps.hmppsallocations.integration.unallocatedcases

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus.METHOD_NOT_ALLOWED
import org.springframework.http.HttpStatus.NOT_FOUND
import uk.gov.justice.digital.hmpps.hmppsallocations.controller.UnallocatedCase
import uk.gov.justice.digital.hmpps.hmppsallocations.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.hmppsallocations.jpa.entity.UnallocatedCaseEntity
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit.SECONDS

class UnallocatedCasesTest : IntegrationTestBase() {

  val firstSentenceDate = LocalDateTime.now().minusDays(4).truncatedTo(SECONDS)
  val firstInitialAppointment = LocalDateTime.now().plusDays(1).truncatedTo(SECONDS)

  @BeforeEach
  fun insertCases() {

    repository.saveAll(
      listOf(
        UnallocatedCaseEntity(
          null, "Dylan Adam Armstrong", "J678910", "C1",
          firstSentenceDate, firstInitialAppointment,	"Currently managed"
        ),
        UnallocatedCaseEntity(null, "Andrei Edwards", "J680648", "A1", LocalDateTime.now().minusDays(3), LocalDateTime.now().plusDays(2), "New to probation"),
        UnallocatedCaseEntity(null, "Hannah Francis", "J680660", "C2", LocalDateTime.now().minusDays(1), null, "Previously managed")

      )
    )
  }

  @Test
  fun `can get unallocated cases`() {
    val unallocatedCases = webTestClient.get()
      .uri("/cases/unallocated")
      .headers { it.authToken(roles = listOf("ROLE_MANAGE_A_WORKFORCE_ALLOCATE")) }
      .exchange()
      .expectStatus()
      .isOk
      .expectBodyList(UnallocatedCase::class.java)
      .returnResult().responseBody
    assertThat(unallocatedCases!!.size).isEqualTo(3)
    val firstCase = unallocatedCases[0]
    assertThat(firstCase).isEqualTo(

      UnallocatedCase("Dylan Adam Armstrong", "J678910", "C1", firstSentenceDate, firstInitialAppointment,	"Currently managed")
    )
  }

  @Test
  fun `cannot get unallocated cases when no auth token supplied`() {
    webTestClient.get()
      .uri("/cases/unallocated")
      .exchange()
      .expectStatus()
      .isUnauthorized
  }

  @Test
  fun `method not allowed`() {
    webTestClient.post()
      .uri("/cases/unallocated")
      .headers { it.authToken(roles = listOf("ROLE_QUEUE_WORKLOAD_ADMIN")) }
      .exchange()
      .expectStatus()
      .isEqualTo(METHOD_NOT_ALLOWED)
  }

  @Test
  fun `not found`() {
    webTestClient.post()
      .uri("/cases/someotherurl")
      .headers { it.authToken(roles = listOf("ROLE_QUEUE_WORKLOAD_ADMIN")) }
      .exchange()
      .expectStatus()
      .isEqualTo(NOT_FOUND)
  }

  @AfterEach
  fun resetDatabase() {
    repository.deleteAll()
  }
}
