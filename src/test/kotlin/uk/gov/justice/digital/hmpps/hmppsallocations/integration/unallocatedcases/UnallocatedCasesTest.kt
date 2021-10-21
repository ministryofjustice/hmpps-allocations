package uk.gov.justice.digital.hmpps.hmppsallocations.integration.unallocatedcases

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import uk.gov.justice.digital.hmpps.hmppsallocations.controller.UnallocatedCase
import uk.gov.justice.digital.hmpps.hmppsallocations.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.hmppsallocations.jpa.entity.repository.UnallocatedCaseEntity
import uk.gov.justice.digital.hmpps.hmppsallocations.jpa.repository.UnallocatedCasesRepository
import java.time.LocalDateTime

class UnallocatedCasesTest(@Autowired val repository: UnallocatedCasesRepository) : IntegrationTestBase() {

  val firstSentenceDate = LocalDateTime.now().minusDays(4)
  val firstInitialAppointment = LocalDateTime.now().plusDays(1)

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

  @AfterEach
  fun resetDatabase() {
    repository.deleteAll()
  }
}
