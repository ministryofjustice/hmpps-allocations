package uk.gov.justice.digital.hmpps.hmppsallocations.integration.unallocatedcases

import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.hmppsallocations.integration.IntegrationTestBase

class GetCaseProbationRecordByCrnTest : IntegrationTestBase() {

  @Test
  fun `can get case probation record`() {
    val crn = "J678910"
    insertCases()
    singleActiveAndInactiveConvictionsResponse(crn)
    getStaffWithGradeFromDelius(crn)
    getRiskSummaryForCrn(crn)
    webTestClient.get()
      .uri("/cases/unallocated/$crn/convictions")
      .headers { it.authToken(roles = listOf("ROLE_MANAGE_A_WORKFORCE_ALLOCATE")) }
      .exchange()
      .expectStatus()
      .isOk
      .expectBody()
      .jsonPath("$.name")
      .isEqualTo("Dylan Adam Armstrong")
      .jsonPath("$.crn")
      .isEqualTo("J678910")
      .jsonPath("$.tier")
      .isEqualTo("C1")
      .jsonPath("$.active")
      .isEmpty
      .jsonPath("$.previous[0].description")
      .isEqualTo("Absolute/Conditional Discharge")
      .jsonPath("$.previous[0].length")
      .isEqualTo(0)
      .jsonPath("$.previous[0].lengthUnit")
      .doesNotExist()
      .jsonPath("$.previous[0].endDate")
      .isEqualTo("2009-10-12")
      .jsonPath("$.previous[0].offences[0].description")
      .isEqualTo("Abstracting electricity - 04300")
      .jsonPath("$.previous[0].offences[0].mainOffence")
      .isEqualTo(true)
      .jsonPath("$.roshLevel")
      .isEqualTo("HIGH")
      .jsonPath("$.roshLastUpdatedOn")
      .isEqualTo("2022-02-02")
  }

  @Test
  fun `active probation record has probation practitioner`() {
    val crn = "J678910"
    insertCases()
    twoActiveConvictionsResponse(crn)
    getStaffWithGradeFromDelius(crn)

    webTestClient.get()
      .uri("/cases/unallocated/$crn/convictions")
      .headers { it.authToken(roles = listOf("ROLE_MANAGE_A_WORKFORCE_ALLOCATE")) }
      .exchange()
      .expectStatus()
      .isOk
      .expectBody()
      .jsonPath("$.active[0].description")
      .isEqualTo("Adult Custody < 12m")
      .jsonPath("$.active[0].length")
      .isEqualTo(6)
      .jsonPath("$.active[0].lengthUnit")
      .isEqualTo("Months")
      .jsonPath("$.active[0].startDate")
      .isEqualTo("2019-11-17")
      .jsonPath("$.active[0].offenderManager.forenames")
      .isEqualTo("Sheila Linda")
      .jsonPath("$.active[0].offenderManager.surname")
      .isEqualTo("Hancock")
      .jsonPath("$.active[0].offenderManager.grade")
      .isEqualTo("PSO")
      .jsonPath("$.active[0].offences[0].description")
      .isEqualTo("Abstracting electricity - 04300")
      .jsonPath("$.active[0].offences[0].mainOffence")
      .isEqualTo(true)
  }

  @Test
  fun `can get probation record for no convictions`() {
    val crn = "J678910"
    insertCases()
    noConvictionsResponse(crn)
    getStaffWithGradeFromDelius(crn)
    webTestClient.get()
      .uri("/cases/unallocated/$crn/convictions")
      .headers { it.authToken(roles = listOf("ROLE_MANAGE_A_WORKFORCE_ALLOCATE")) }
      .exchange()
      .expectStatus()
      .isOk
      .expectBody()
      .jsonPath("$.active")
      .isEmpty
      .jsonPath("$.previous")
      .isEmpty
  }
}
