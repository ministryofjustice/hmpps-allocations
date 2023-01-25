package uk.gov.justice.digital.hmpps.hmppsallocations.integration.unallocatedcases

import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.hmppsallocations.integration.IntegrationTestBase

class GetCaseProbationRecordByCrnTest : IntegrationTestBase() {

  @Test
  fun `can get case probation record excluding conviction number`() {
    val crn = "J678910"
    val convictionNumber = 1
    insertCases()
    probationRecordSingleInactiveEventReponse(crn, convictionNumber)
    makeTestRequest(crn, convictionNumber)
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
      .isEqualTo("0")
      .jsonPath("$.previous[0].offenderManager.name")
      .isEqualTo("A Staff Name")
      .jsonPath("$.previous[0].offenderManager.grade")
      .isEqualTo("PQiP")
      .jsonPath("$.previous[0].endDate")
      .isEqualTo("2009-10-12")
      .jsonPath("$.previous[0].offences[0].description")
      .isEqualTo("Abstracting electricity - 04300")
      .jsonPath("$.previous[0].offences[0].mainOffence")
      .isEqualTo(true)
      .jsonPath("$.convictionNumber")
      .isEqualTo(convictionNumber)
  }

  @Test
  fun `active probation record has probation practitioner`() {
    val crn = "J678910"
    val convictionNumber = 1
    insertCases()
    probationRecordSingleActiveEventReponse(crn, convictionNumber)

    makeTestRequest(crn, convictionNumber)
      .jsonPath("$.active[0].description")
      .isEqualTo("Adult Custody < 12m")
      .jsonPath("$.active[0].length")
      .isEqualTo("6 Months")
      .jsonPath("$.active[0].startDate")
      .isEqualTo("2021-11-22")
      .jsonPath("$.active[0].offenderManager.name")
      .isEqualTo("A Staff Name")
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
    val convictionNumber = 1
    insertCases()
    probationRecordNoEventsResponse(crn, convictionNumber)
    makeTestRequest(crn, convictionNumber)
      .jsonPath("$.active")
      .isEmpty
      .jsonPath("$.previous")
      .isEmpty
  }

  private fun makeTestRequest(
    crn: String,
    convictionNumber: Int
  ) = webTestClient.get()
    .uri("/cases/unallocated/$crn/record/exclude-conviction/$convictionNumber")
    .headers { it.authToken(roles = listOf("ROLE_MANAGE_A_WORKFORCE_ALLOCATE")) }
    .exchange()
    .expectStatus()
    .isOk
    .expectBody()
}
