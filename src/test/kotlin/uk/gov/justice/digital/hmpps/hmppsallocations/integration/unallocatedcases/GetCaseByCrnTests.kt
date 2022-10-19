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
    val convictionId = 123456789L
    insertCases()
    val dateOfBirth = LocalDate.of(2001, 11, 17)
    val expectedAge = Period.between(dateOfBirth, LocalDate.now()).years

    offenderDetailsResponse(crn)
    unallocatedConvictionResponse(crn, convictionId)
    singleActiveRequirementResponse(crn, convictionId)
    documentsResponse(crn, convictionId)
    getAssessmentsForCrn(crn)
    webTestClient.get()
      .uri("/cases/unallocated/$crn/convictions/$convictionId")
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
      .isEqualTo("2021-12-03")
      .jsonPath("$.courtReport.documentId")
      .isEqualTo("6c50048a-c647-4598-8fae-0b84c69ef31a")
      .jsonPath("$.cpsPack.completedDate")
      .isEqualTo("2021-10-17")
      .jsonPath("$.cpsPack.documentId")
      .isEqualTo("efb7a4e8-3f4a-449c-bf6f-b1fc8def3410")
      .jsonPath("$.assessment.lastAssessedOn")
      .isEqualTo("2014-03-28")
      .jsonPath("$.assessment.type")
      .isEqualTo("LAYER_3")
      .jsonPath("$.convictionId")
      .isEqualTo(convictionId)
      .jsonPath("$.caseType")
      .isEqualTo("CUSTODY")
      .jsonPath("$.preConvictionDocument.completedDate")
      .isEqualTo("2021-11-17")
      .jsonPath("$.preConvictionDocument.documentId")
      .isEqualTo("626aa1d1-71c6-4b76-92a1-bf2f9250c143")
  }

  @Test
  fun `can get case by crn missing court report`() {
    val crn = "J678910"
    val convictionId = 123456789L
    insertCases()
    offenderDetailsResponse(crn)
    unallocatedConvictionResponse(crn, convictionId)
    singleActiveRequirementResponse(crn, convictionId)
    noDocumentsResponse(crn, convictionId)
    getAssessmentsForCrn(crn)

    webTestClient.get()
      .uri("/cases/unallocated/$crn/convictions/$convictionId")
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
    val convictionId = 123456789L
    insertCases()
    offenderDetailsResponse(crn)
    unallocatedConvictionResponse(crn, convictionId)
    singleActiveRequirementResponse(crn, convictionId)
    documentsResponse(crn, convictionId)
    notFoundAssessmentForCrn(crn)

    webTestClient.get()
      .uri("/cases/unallocated/$crn/convictions/$convictionId")
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
      .uri("/cases/unallocated/J678912/convictions/51245325")
      .headers { it.authToken(roles = listOf("ROLE_MANAGE_A_WORKFORCE_ALLOCATE")) }
      .exchange()
      .expectStatus()
      .isNotFound
  }

  @Test
  fun `retrieve main address`() {
    val crn = "J678910"
    val convictionId = 123456789L
    insertCases()

    offenderDetailsResponse(crn)
    unallocatedConvictionResponse(crn, convictionId)
    singleActiveRequirementResponse(crn, convictionId)
    documentsResponse(crn, convictionId)
    getAssessmentsForCrn(crn)
    webTestClient.get()
      .uri("/cases/unallocated/$crn/convictions/$convictionId")
      .headers { it.authToken(roles = listOf("ROLE_MANAGE_A_WORKFORCE_ALLOCATE")) }
      .exchange()
      .expectStatus()
      .isOk
      .expectBody()
      .jsonPath("$.address.addressNumber")
      .isEqualTo("22")
      .jsonPath("$.address.buildingName")
      .isEqualTo("Sheffield Towers")
      .jsonPath("$.address.streetName")
      .isEqualTo("Sheffield Street")
      .jsonPath("$.address.town")
      .isEqualTo("Sheffield")
      .jsonPath("$.address.county")
      .isEqualTo("Yorkshire")
      .jsonPath("$.address.postcode")
      .isEqualTo("S2 4SU")
      .jsonPath("$.address.type.description")
      .isEqualTo("Supported Housing")
      .jsonPath("$.address.from")
      .isEqualTo("2022-08-25")
      .jsonPath("$.address.noFixedAbode")
      .isEqualTo(false)
  }

  @Test
  fun `return a no fixed abode address`() {
    val crn = "J678910"
    val convictionId = 123456789L
    insertCases()

    offenderDetailsNoFixedAbodeResponse(crn)
    unallocatedConvictionResponse(crn, convictionId)
    singleActiveRequirementResponse(crn, convictionId)
    documentsResponse(crn, convictionId)
    getAssessmentsForCrn(crn)
    webTestClient.get()
      .uri("/cases/unallocated/$crn/convictions/$convictionId")
      .headers { it.authToken(roles = listOf("ROLE_MANAGE_A_WORKFORCE_ALLOCATE")) }
      .exchange()
      .expectStatus()
      .isOk
      .expectBody()
      .jsonPath("$.address.type.description")
      .isEqualTo("Homeless - rough sleeping")
      .jsonPath("$.address.from")
      .isEqualTo("2022-08-22")
      .jsonPath("$.address.noFixedAbode")
      .isEqualTo(true)
  }

  @Test
  fun `must return sentence length`() {
    val crn = "J678910"
    val convictionId = 123456789L
    insertCases()

    offenderDetailsNoFixedAbodeResponse(crn)
    unallocatedConvictionResponse(crn, convictionId)
    singleActiveRequirementResponse(crn, convictionId)
    documentsResponse(crn, convictionId)
    getAssessmentsForCrn(crn)
    webTestClient.get()
      .uri("/cases/unallocated/$crn/convictions/$convictionId")
      .headers { it.authToken(roles = listOf("ROLE_MANAGE_A_WORKFORCE_ALLOCATE")) }
      .exchange()
      .expectStatus()
      .isOk
      .expectBody()
      .jsonPath("$.sentenceLength")
      .isEqualTo("5 Weeks")
  }
}
