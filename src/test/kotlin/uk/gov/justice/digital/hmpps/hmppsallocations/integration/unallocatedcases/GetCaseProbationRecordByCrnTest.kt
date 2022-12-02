package uk.gov.justice.digital.hmpps.hmppsallocations.integration.unallocatedcases

import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.hmppsallocations.integration.IntegrationTestBase

class GetCaseProbationRecordByCrnTest : IntegrationTestBase() {

  @Test
  fun `can get case probation record excluding conviction ID`() {
    val crn = "J678910"
    val convictionId = 123456789L
    insertCases()
    singleActiveAndInactiveConvictionsResponse(crn, "STAFF1")
    webTestClient.get()
      .uri("/cases/unallocated/$crn/convictions?excludeConvictionId=$convictionId")
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
      .jsonPath("$.convictionId")
      .isEqualTo(convictionId)
      .jsonPath("$.convictionNumber")
      .isEqualTo(1)
  }

  @Test
  fun `can get case probation record excluding conviction number`() {
    val crn = "J678910"
    val convictionNumber = 1
    insertCases()
    singleActiveAndInactiveConvictionsResponse(crn, "STAFF1")
    webTestClient.get()
      .uri("/cases/unallocated/$crn/convictions?excludeConvictionId=$convictionNumber")
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
      .jsonPath("$.convictionId")
      .isEqualTo(123456789L)
      .jsonPath("$.convictionNumber")
      .isEqualTo(convictionNumber)
  }

  @Test
  fun `active probation record has probation practitioner`() {
    val crn = "J678910"
    val convictionId = 123456789L
    insertCases()
    twoActiveConvictionsResponse(crn)

    webTestClient.get()
      .uri("/cases/unallocated/$crn/convictions?excludeConvictionId=$convictionId")
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
    val convictionId = 123456789L
    insertCases()
    noConvictionsResponse(crn)
    webTestClient.get()
      .uri("/cases/unallocated/$crn/convictions?excludeConvictionId=$convictionId")
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

  @Test
  fun `do not return unallocated order managers`() {
    val crn = "J678910"
    val convictionId = 123456789L
    insertCases()
    singleActiveAndInactiveConvictionsResponse(crn, "STAFFU")
    webTestClient.get()
      .uri("/cases/unallocated/$crn/convictions?excludeConvictionId=$convictionId")
      .headers { it.authToken(roles = listOf("ROLE_MANAGE_A_WORKFORCE_ALLOCATE")) }
      .exchange()
      .expectStatus()
      .isOk
      .expectBody()
      .jsonPath("$.previous[0].offenderManager")
      .doesNotExist()
  }
}
