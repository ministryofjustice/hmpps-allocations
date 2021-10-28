package uk.gov.justice.digital.hmpps.hmppsallocations.integration.unallocatedcases

import com.opencsv.CSVWriter
import com.opencsv.bean.StatefulBeanToCsv
import com.opencsv.bean.StatefulBeanToCsvBuilder
import org.assertj.core.api.Assertions.assertThat
import org.awaitility.kotlin.await
import org.awaitility.kotlin.matches
import org.awaitility.kotlin.untilCallTo
import org.junit.jupiter.api.Test
import org.springframework.core.io.FileSystemResource
import org.springframework.http.HttpStatus.METHOD_NOT_ALLOWED
import org.springframework.http.HttpStatus.NOT_FOUND
import org.springframework.http.MediaType
import org.springframework.http.client.MultipartBodyBuilder
import org.springframework.web.reactive.function.BodyInserters
import uk.gov.justice.digital.hmpps.hmppsallocations.controller.UnallocatedCaseCsv
import uk.gov.justice.digital.hmpps.hmppsallocations.domain.UnallocatedCase
import uk.gov.justice.digital.hmpps.hmppsallocations.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.hmppsallocations.jpa.entity.UnallocatedCaseEntity
import java.io.File
import java.io.FileWriter
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit.SECONDS

class UnallocatedCasesTest : IntegrationTestBase() {

  val firstSentenceDate = LocalDateTime.now().minusDays(4).truncatedTo(SECONDS)
  val firstInitialAppointment = LocalDateTime.now().plusDays(1).truncatedTo(SECONDS)

  fun insertCases() {

    repository.saveAll(
      listOf(
        UnallocatedCaseEntity(
          null, "Dylan Adam Armstrong", "J678910", "C1",
          firstSentenceDate, firstInitialAppointment, "Currently managed"
        ),
        UnallocatedCaseEntity(
          null,
          "Andrei Edwards",
          "J680648",
          "A1",
          LocalDateTime.now().minusDays(3),
          LocalDateTime.now().plusDays(2),
          "New to probation"
        ),
        UnallocatedCaseEntity(
          null,
          "Hannah Francis",
          "J680660",
          "C2",
          LocalDateTime.now().minusDays(1),
          null,
          "Previously managed"
        )

      )
    )
  }

  fun generateCsv(unallocatedCases: List<UnallocatedCaseCsv>): File {
    val tempFile = kotlin.io.path.createTempFile().toFile()
    val writer = FileWriter(tempFile)

    val sbc: StatefulBeanToCsv<UnallocatedCaseCsv> = StatefulBeanToCsvBuilder<UnallocatedCaseCsv>(writer)
      .withSeparator(CSVWriter.DEFAULT_SEPARATOR)
      .build()
    sbc.write(unallocatedCases)
    writer.close()
    return tempFile
  }

  @Test
  fun `can get unallocated cases`() {
    insertCases()
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

      UnallocatedCase(
        "Dylan Adam Armstrong",
        "J678910",
        "C1",
        firstSentenceDate,
        firstInitialAppointment,
        "Currently managed"
      )
    )
  }

  @Test
  fun `populate database from csv upload of unallocated cases`() {
    val unallocatedCases = listOf(
      UnallocatedCaseCsv(
        "Dylan Adam Armstrong",
        "J678910",
        "C1",
        firstSentenceDate,
        firstInitialAppointment,
        "Currently managed"
      ),
      UnallocatedCaseCsv(
        "Andrei Edwards",
        "J680648",
        "A1",
        LocalDateTime.now().minusDays(3),
        LocalDateTime.now().plusDays(2),
        "New to probation"
      ),
      UnallocatedCaseCsv(
        "Hannah Francis",
        "J680660",
        "C2",
        LocalDateTime.now().minusDays(1),
        null,
        "Previously managed"
      )
    )
    val csvFile = generateCsv(unallocatedCases)
    val multipartBodyBuilder = MultipartBodyBuilder()
    multipartBodyBuilder.part("file", FileSystemResource(csvFile))

    webTestClient.post()
      .uri("/cases/unallocated/upload")
      .contentType(MediaType.MULTIPART_FORM_DATA)
      .headers { it.authToken(roles = listOf("ROLE_MANAGE_A_WORKFORCE_ALLOCATE")) }
      .body(BodyInserters.fromMultipartData(multipartBodyBuilder.build()))
      .exchange()
      .expectStatus()
      .isOk

    await untilCallTo { repository.count() } matches { it == unallocatedCases.size.toLong() }
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
}
