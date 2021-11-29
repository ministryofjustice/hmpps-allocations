package uk.gov.justice.digital.hmpps.hmppsallocations.integration.unallocatedcases

import com.opencsv.CSVWriter
import com.opencsv.bean.StatefulBeanToCsv
import com.opencsv.bean.StatefulBeanToCsvBuilder
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
import uk.gov.justice.digital.hmpps.hmppsallocations.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.hmppsallocations.jpa.entity.UnallocatedCaseEntity
import java.io.File
import java.io.FileWriter
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class UnallocatedCasesTest : IntegrationTestBase() {

  val firstSentenceDate = LocalDate.now().minusDays(4)
  val firstInitialAppointment = LocalDate.now().plusDays(1)

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
          LocalDate.now().minusDays(3),
          LocalDate.now().plusDays(2),
          "New to probation"
        ),
        UnallocatedCaseEntity(
          null,
          "Hannah Francis",
          "J680660",
          "C2",
          LocalDate.now().minusDays(1),
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
    webTestClient.get()
      .uri("/cases/unallocated")
      .headers { it.authToken(roles = listOf("ROLE_MANAGE_A_WORKFORCE_ALLOCATE")) }
      .exchange()
      .expectStatus()
      .isOk
      .expectBody()
      .jsonPath("$.length()")
      .isEqualTo(3)
      .jsonPath("$.[0].sentenceDate")
      .isEqualTo(firstSentenceDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")))
      .jsonPath("$.[0].initialAppointment")
      .isEqualTo(firstInitialAppointment.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")))
      .jsonPath("$.[0].name")
      .isEqualTo("Dylan Adam Armstrong")
      .jsonPath("$.[0].crn")
      .isEqualTo("J678910")
      .jsonPath("$.[0].tier")
      .isEqualTo("C1")
      .jsonPath("$.[0].status")
      .isEqualTo("Currently managed")
  }

  @Test
  fun `populate database from csv upload of unallocated cases`() {
    allDeliusResponses("J678910")
    allDeliusResponses("J680648")
    allDeliusResponses("J680660")

    val unallocatedCases = listOf(
      UnallocatedCaseCsv(
        "J678910",
        "Currently managed"
      ),
      UnallocatedCaseCsv(
        "J680648",
        "New to probation"
      ),
      UnallocatedCaseCsv(
        "J680660",
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
