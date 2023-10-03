package uk.gov.justice.digital.hmpps.hmppsallocations.integration.unallocatedcases

import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.hmppsallocations.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.hmppsallocations.integration.mockserver.AssessRisksNeedsApiExtension
import uk.gov.justice.digital.hmpps.hmppsallocations.integration.mockserver.OffenderAssessmentApiExtension.Companion.offenderAssessmentApi
import uk.gov.justice.digital.hmpps.hmppsallocations.integration.mockserver.WorkforceAllocationsToDeliusApiExtension.Companion.workforceAllocationsToDelius

class GetCaseByCrnTests : IntegrationTestBase() {

  @Test
  fun `can get case by crn and convictionNumber`() {
    val crn = "J678910"
    val convictionNumber = 1
    workforceAllocationsToDelius.userHasAccess("J678910")
    insertCases()
    AssessRisksNeedsApiExtension.assessRisksNeedsApi.getRoshForCrn(crn)
    AssessRisksNeedsApiExtension.assessRisksNeedsApi.getRiskPredictorsForCrn(crn)
    workforceAllocationsToDelius.riskResponse(crn)
    workforceAllocationsToDelius.caseViewResponse(crn, convictionNumber)
    offenderAssessmentApi.getAssessmentsForCrn(crn)
    workforceAllocationsToDelius.userHasAccess("J678910")
    webTestClient.get()
      .uri("/cases/unallocated/$crn/convictions/$convictionNumber")
      .headers { it.authToken(roles = listOf("ROLE_MANAGE_A_WORKFORCE_ALLOCATE")) }
      .exchange()
      .expectStatus()
      .isOk
      .expectBody()
      .jsonPath("$.sentenceDate")
      .isEqualTo("2023-01-04")
      .jsonPath("$.name")
      .isEqualTo("Dylan Adam Armstrong")
      .jsonPath("$.crn")
      .isEqualTo("J678910")
      .jsonPath("$.tier")
      .isEqualTo("C1")
      .jsonPath("$.gender")
      .isEqualTo("Male")
      .jsonPath("$.dateOfBirth")
      .isEqualTo("2001-11-17")
      .jsonPath("$.age")
      .isEqualTo(21)
      .jsonPath("$.expectedSentenceEndDate")
      .isEqualTo("2024-01-03")
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
      .isEqualTo("100 Hours")
      .jsonPath("$.pncNumber")
      .isEqualTo("9999/1234567A")
      .jsonPath("$.courtReport.description")
      .isEqualTo("Pre-Sentence Report - Fast")
      .jsonPath("$.courtReport.completedDate")
      .isEqualTo("2021-12-07")
      .jsonPath("$.courtReport.documentId")
      .isEqualTo("6c50048a-c647-4598-8fae-0b84c69ef31a")
      .jsonPath("$.courtReport.name")
      .isEqualTo("doc.pdf")
      .jsonPath("$.cpsPack.completedDate")
      .isEqualTo("2021-10-16")
      .jsonPath("$.cpsPack.documentId")
      .isEqualTo("efb7a4e8-3f4a-449c-bf6f-b1fc8def3410")
      .jsonPath("$.cpsPack.name")
      .isEqualTo("cps.pdf")
      .jsonPath("$.assessment.lastAssessedOn")
      .isEqualTo("2014-03-28")
      .jsonPath("$.assessment.type")
      .isEqualTo("LAYER_3")
      .jsonPath("$.convictionNumber")
      .isEqualTo(convictionNumber)
      .jsonPath("$.preConvictionDocument.completedDate")
      .doesNotExist()
      .jsonPath("$.preConvictionDocument.documentId")
      .isEqualTo("626aa1d1-71c6-4b76-92a1-bf2f9250c143")
      .jsonPath("$.preConvictionDocument.name")
      .isEqualTo("Pre Cons.pdf")
      .jsonPath("$.roshLevel")
      .isEqualTo("VERY_HIGH")
      .jsonPath("$.rsrLevel")
      .isEqualTo("MEDIUM")
      .jsonPath("$.ogrsScore")
      .isEqualTo(85)
      .jsonPath("$.activeRiskRegistration")
      .isEqualTo("ALT Under MAPPA Arrangements, Suicide/self-harm")
  }

  @Test
  fun `can get case by crn missing court report`() {
    val crn = "J678910"
    val convictionNumber = 1
    workforceAllocationsToDelius.userHasAccess("J678910")
    insertCases()
    workforceAllocationsToDelius.riskResponse(crn)
    workforceAllocationsToDelius.caseViewNoCourtReportResponse(crn, convictionNumber)
    offenderAssessmentApi.getAssessmentsForCrn(crn)
    workforceAllocationsToDelius.userHasAccess("J678910")

    webTestClient.get()
      .uri("/cases/unallocated/$crn/convictions/$convictionNumber")
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
    val convictionNumber = 1
    workforceAllocationsToDelius.userHasAccess("J678910")
    insertCases()
    workforceAllocationsToDelius.riskResponse(crn)
    workforceAllocationsToDelius.caseViewResponse(crn, convictionNumber)
    offenderAssessmentApi.notFoundAssessmentForCrn(crn)
    workforceAllocationsToDelius.userHasAccess("J678910")

    webTestClient.get()
      .uri("/cases/unallocated/$crn/convictions/$convictionNumber")
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
    workforceAllocationsToDelius.userHasAccess("J678912")
    webTestClient.get()
      .uri("/cases/unallocated/J678912/convictions/9")
      .headers { it.authToken(roles = listOf("ROLE_MANAGE_A_WORKFORCE_ALLOCATE")) }
      .exchange()
      .expectStatus()
      .isNotFound
  }

  @Test
  fun `get 404 if crn is restricted or limited`() {
    webTestClient.get()
      .uri("/cases/unallocated/J678912/convictions/9")
      .headers { it.authToken(roles = listOf("ROLE_MANAGE_A_WORKFORCE_ALLOCATE")) }
      .exchange()
      .expectStatus()
      .isNotFound
  }

  @Test
  fun `retrieve main address`() {
    val crn = "J678910"
    val convictionNumber = 1
    workforceAllocationsToDelius.userHasAccess("J678910")
    insertCases()
    workforceAllocationsToDelius.riskResponse(crn)
    workforceAllocationsToDelius.caseViewWithMainAddressResponse(crn, convictionNumber)
    offenderAssessmentApi.getAssessmentsForCrn(crn)
    workforceAllocationsToDelius.userHasAccess("J678910")

    webTestClient.get()
      .uri("/cases/unallocated/$crn/convictions/$convictionNumber")
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
      .jsonPath("$.address.typeDescription")
      .isEqualTo("Supported Housing")
      .jsonPath("$.address.startDate")
      .isEqualTo("2022-08-25")
      .jsonPath("$.address.noFixedAbode")
      .isEqualTo(false)
  }

  @Test
  fun `return a no fixed abode address`() {
    val crn = "J678910"
    val convictionNumber = 1
    workforceAllocationsToDelius.userHasAccess("J678910")
    insertCases()
    workforceAllocationsToDelius.riskResponse(crn)
    workforceAllocationsToDelius.caseViewWithNoFixedAbodeResponse(crn, convictionNumber)
    offenderAssessmentApi.getAssessmentsForCrn(crn)
    workforceAllocationsToDelius.userHasAccess("J678910")

    webTestClient.get()
      .uri("/cases/unallocated/$crn/convictions/$convictionNumber")
      .headers { it.authToken(roles = listOf("ROLE_MANAGE_A_WORKFORCE_ALLOCATE")) }
      .exchange()
      .expectStatus()
      .isOk
      .expectBody()
      .jsonPath("$.address.typeDescription")
      .isEqualTo("Homeless - rough sleeping")
      .jsonPath("$.address.startDate")
      .isEqualTo("2022-08-25")
      .jsonPath("$.address.noFixedAbode")
      .isEqualTo(true)
  }

  @Test
  fun `must return sentence length`() {
    val crn = "J678910"
    val convictionNumber = 1
    workforceAllocationsToDelius.userHasAccess("J678910")
    insertCases()
    workforceAllocationsToDelius.riskResponse(crn)
    workforceAllocationsToDelius.caseViewResponse(crn, convictionNumber)
    offenderAssessmentApi.getAssessmentsForCrn(crn)
    workforceAllocationsToDelius.userHasAccess("J678910")
    webTestClient.get()
      .uri("/cases/unallocated/$crn/convictions/$convictionNumber")
      .headers { it.authToken(roles = listOf("ROLE_MANAGE_A_WORKFORCE_ALLOCATE")) }
      .exchange()
      .expectStatus()
      .isOk
      .expectBody()
      .jsonPath("$.sentenceLength")
      .isEqualTo("12 Months")
  }

  @Test
  fun `can get case by crn and not found rosh and not found rsr and no regist`() {
    val crn = "J678910"
    val convictionNumber = 1
    workforceAllocationsToDelius.userHasAccess(crn)
    insertCases()
    AssessRisksNeedsApiExtension.assessRisksNeedsApi.getRoshNoLevelForCrn(crn)
    AssessRisksNeedsApiExtension.assessRisksNeedsApi.getRiskPredictorsNotFoundForCrn(crn)
    workforceAllocationsToDelius.riskResponseNoRegistrationsNoOgrs(crn)
    workforceAllocationsToDelius.caseViewResponse(crn, convictionNumber)
    offenderAssessmentApi.getAssessmentsForCrn(crn)
    workforceAllocationsToDelius.userHasAccess(crn)
    webTestClient.get()
      .uri("/cases/unallocated/$crn/convictions/$convictionNumber")
      .headers { it.authToken(roles = listOf("ROLE_MANAGE_A_WORKFORCE_ALLOCATE")) }
      .exchange()
      .expectStatus()
      .isOk
      .expectBody()
      .jsonPath("$.roshLevel")
      .isEqualTo("NOT_FOUND")
      .jsonPath("$.rsrLevel")
      .isEqualTo("NOT_FOUND")
      .jsonPath("$.ogrsScore")
      .isEmpty
      .jsonPath("$.activeRiskRegistration")
      .isEmpty
  }

  @Test
  fun `can get case by crn and unavailable rosh and unavailable rsr and no regist`() {
    val crn = "J678910"
    val convictionNumber = 1
    workforceAllocationsToDelius.userHasAccess("J678910")
    insertCases()
    AssessRisksNeedsApiExtension.assessRisksNeedsApi.getRoshUnavailableForCrn(crn)
    AssessRisksNeedsApiExtension.assessRisksNeedsApi.getRiskPredictorsUnavailableForCrn(crn)
    workforceAllocationsToDelius.riskResponseNoRegistrationsNoOgrs(crn)
    workforceAllocationsToDelius.caseViewResponse(crn, convictionNumber)
    offenderAssessmentApi.getAssessmentsForCrn(crn)
    workforceAllocationsToDelius.userHasAccess("J678910")
    webTestClient.get()
      .uri("/cases/unallocated/$crn/convictions/$convictionNumber")
      .headers { it.authToken(roles = listOf("ROLE_MANAGE_A_WORKFORCE_ALLOCATE")) }
      .exchange()
      .expectStatus()
      .isOk
      .expectBody()
      .jsonPath("$.roshLevel")
      .isEqualTo("UNAVAILABLE")
      .jsonPath("$.rsrLevel")
      .isEqualTo("UNAVAILABLE")
      .jsonPath("$.ogrsScore")
      .isEmpty
      .jsonPath("$.activeRiskRegistration")
      .isEmpty
  }

  @Test
  fun `return 404 when case is LAO`() {
    val crn = "J678910"
    val convictionNumber = 1
    insertCases()
    AssessRisksNeedsApiExtension.assessRisksNeedsApi.getRoshUnavailableForCrn(crn)
    AssessRisksNeedsApiExtension.assessRisksNeedsApi.getRiskPredictorsUnavailableForCrn(crn)
    workforceAllocationsToDelius.riskResponseNoRegistrationsNoOgrs(crn)
    workforceAllocationsToDelius.caseViewResponse(crn, convictionNumber)
    offenderAssessmentApi.getAssessmentsForCrn(crn)
    webTestClient.get()
      .uri("/cases/unallocated/$crn/convictions/$convictionNumber")
      .headers { it.authToken(roles = listOf("ROLE_MANAGE_A_WORKFORCE_ALLOCATE")) }
      .exchange()
      .expectStatus()
      .isNotFound
  }
}
