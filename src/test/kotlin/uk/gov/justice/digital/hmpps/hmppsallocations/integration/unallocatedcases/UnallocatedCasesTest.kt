package uk.gov.justice.digital.hmpps.hmppsallocations.integration.unallocatedcases

import com.opencsv.CSVWriter.DEFAULT_SEPARATOR
import com.opencsv.bean.StatefulBeanToCsv
import com.opencsv.bean.StatefulBeanToCsvBuilder
import org.awaitility.kotlin.await
import org.awaitility.kotlin.matches
import org.awaitility.kotlin.untilCallTo
import org.junit.jupiter.api.Test
import org.springframework.core.io.FileSystemResource
import org.springframework.http.HttpStatus.METHOD_NOT_ALLOWED
import org.springframework.http.HttpStatus.NOT_FOUND
import org.springframework.http.MediaType.MULTIPART_FORM_DATA
import org.springframework.http.client.MultipartBodyBuilder
import org.springframework.web.reactive.function.BodyInserters
import uk.gov.justice.digital.hmpps.hmppsallocations.controller.UnallocatedCaseCsv
import uk.gov.justice.digital.hmpps.hmppsallocations.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.hmppsallocations.jpa.entity.UnallocatedCaseEntity
import java.io.File
import java.io.FileWriter
import java.time.LocalDate
import java.time.Period
import java.time.format.DateTimeFormatter

class UnallocatedCasesTest : IntegrationTestBase() {

  val firstSentenceDate = LocalDate.now().minusDays(4)
  val firstInitialAppointment = LocalDate.now().plusDays(1)
  val previousConvictionEndDate = LocalDate.now().minusDays(60)

  val previouslyManagedCase = UnallocatedCaseEntity(
    null,
    "Hannah Francis",
    "J680660",
    "C2",
    LocalDate.now().minusDays(1),
    null,
    "Previously managed",
    previousConvictionEndDate
  )

  fun insertCases() {
    repository.saveAll(
      listOf(
        UnallocatedCaseEntity(
          null, "Dylan Adam Armstrong", "J678910", "C1",
          firstSentenceDate, firstInitialAppointment, "Currently managed",
          null, "Antonio", "LoSardo", "PO"
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
        previouslyManagedCase

      )
    )
  }

  fun generateCsv(unallocatedCases: List<UnallocatedCaseCsv>): File {
    val tempFile = kotlin.io.path.createTempFile().toFile()
    val writer = FileWriter(tempFile)

    val sbc: StatefulBeanToCsv<UnallocatedCaseCsv> = StatefulBeanToCsvBuilder<UnallocatedCaseCsv>(writer)
      .withSeparator(DEFAULT_SEPARATOR)
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
      .jsonPath("$.[0].offenderManager.forenames")
      .isEqualTo("Antonio")
      .jsonPath("$.[0].offenderManager.surname")
      .isEqualTo("LoSardo")
      .jsonPath("$.[0].offenderManager.grade")
      .isEqualTo("PO")
  }

  @Test
  fun `can get previous conviction end date`() {
    repository.save(previouslyManagedCase)
    webTestClient.get()
      .uri("/cases/unallocated")
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
  fun `populate database from csv upload of unallocated cases`() {
    allDeliusResponses("J678910")
    allDeliusResponses("J680648")
    allDeliusResponses("J680660")

    val unallocatedCases = listOf(
      UnallocatedCaseCsv(
        "J678910"
      ),
      UnallocatedCaseCsv(
        "J680648"
      ),
      UnallocatedCaseCsv(
        "J680660"
      )
    )
    val csvFile = generateCsv(unallocatedCases)
    val multipartBodyBuilder = MultipartBodyBuilder()
    multipartBodyBuilder.part("file", FileSystemResource(csvFile))

    webTestClient.post()
      .uri("/cases/unallocated/upload")
      .contentType(MULTIPART_FORM_DATA)
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

  @Test
  fun `can get case by crn`() {
    val crn = "J678910"
    insertCases()
    val dateOfBirth = LocalDate.of(2001, 11, 17)
    val expectedAge = Period.between(dateOfBirth, LocalDate.now()).years
    offenderSummaryResponse(crn)
    singleActiveConvictionResponse(crn)
    singleActiveRequirementResponse(crn, 2500292515)
    singleCourtReportResponse(crn, 2500292515)
    getNeedsForCrn(crn)
    webTestClient.get()
      .uri("/cases/unallocated/$crn")
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
      .jsonPath("$.gender")
      .isEqualTo("Male")
      .jsonPath("$.dateOfBirth")
      .isEqualTo("2001-11-17")
      .jsonPath("$.age")
      .isEqualTo(expectedAge)
      .jsonPath("$.expectedSentenceEndDate")
      .isEqualTo("2020-05-16")
      .jsonPath("$.offences[0].mainOffence")
      .isEqualTo(true)
      .jsonPath("$.offences[0].mainCategory")
      .isEqualTo("Abstracting electricity")
      .jsonPath("$.offences[0].subCategory")
      .isEqualTo("Abstracting electricity")
      .jsonPath("$.requirements[0].mainCategory")
      .isEqualTo("Unpaid Work")
      .jsonPath("$.requirements[0].subCategory")
      .isEqualTo("Regular")
      .jsonPath("$.requirements[0].length")
      .isEqualTo(100)
      .jsonPath("$.requirements[0].lengthUnit")
      .isEqualTo("Hours")
      .jsonPath("$.pncNumber")
      .isEqualTo("9999/1234567A")
      .jsonPath("$.courtReport.code")
      .isEqualTo("CJF")
      .jsonPath("$.courtReport.description")
      .isEqualTo("Fast")
      .jsonPath("$.courtReport.completedDate")
      .isEqualTo("2019-11-11")
      .jsonPath("$.assessment.lastAssessedOn")
      .isEqualTo("2022-01-26")
  }

  @Test
  fun `get 404 if crn not found`() {
    val result = webTestClient.get()
      .uri("/cases/unallocated/J678912")
      .headers { it.authToken(roles = listOf("ROLE_MANAGE_A_WORKFORCE_ALLOCATE")) }
      .exchange()
      .expectStatus()
      .isNotFound
  }
}
