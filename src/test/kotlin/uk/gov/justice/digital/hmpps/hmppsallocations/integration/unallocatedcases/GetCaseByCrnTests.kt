package uk.gov.justice.digital.hmpps.hmppsallocations.integration.unallocatedcases

import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.hmppsallocations.integration.IntegrationTestBase
import java.time.LocalDate
import java.time.Period
import java.time.format.DateTimeFormatter

class GetCaseByCrnTests : IntegrationTestBase() {

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
    getAssessmentsForCrn(crn)
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
      .jsonPath("$.sentenceDescription")
      .isEqualTo("Adult Custody < 12m")
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
      .isEqualTo("2014-03-28")
      .jsonPath("$.assessment.type")
      .isEqualTo("LAYER_3")
  }

  @Test
  fun `can get case by crn missing court report`() {
    val crn = "J678910"
    insertCases()
    offenderSummaryResponse(crn)
    singleActiveConvictionResponse(crn)
    singleActiveRequirementResponse(crn, 2500292515)
    noCourtReportResponse(crn, 2500292515)
    getAssessmentsForCrn(crn)

    webTestClient.get()
      .uri("/cases/unallocated/$crn")
      .headers { it.authToken(roles = listOf("ROLE_MANAGE_A_WORKFORCE_ALLOCATE")) }
      .exchange()
      .expectStatus()
      .isOk
      .expectBody()
      .jsonPath("$.courtReport")
      .doesNotExist()
  }

  @Test
  fun `can get case by crn missing assessment`() {
    val crn = "J678910"
    insertCases()
    offenderSummaryResponse(crn)
    singleActiveConvictionResponse(crn)
    singleActiveRequirementResponse(crn, 2500292515)
    singleCourtReportResponse(crn, 2500292515)
    notFoundAssessmentForCrn(crn)

    webTestClient.get()
      .uri("/cases/unallocated/$crn")
      .headers { it.authToken(roles = listOf("ROLE_MANAGE_A_WORKFORCE_ALLOCATE")) }
      .exchange()
      .expectStatus()
      .isOk
      .expectBody()
      .jsonPath("$.assessment")
      .doesNotExist()
  }

  @Test
  fun `get 404 if crn not found`() {
    webTestClient.get()
      .uri("/cases/unallocated/J678912")
      .headers { it.authToken(roles = listOf("ROLE_MANAGE_A_WORKFORCE_ALLOCATE")) }
      .exchange()
      .expectStatus()
      .isNotFound
  }
}
