package uk.gov.justice.digital.hmpps.hmppsallocations.integration.unallocatedcases

import com.fasterxml.jackson.core.type.TypeReference
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.hmppsallocations.client.Document
import uk.gov.justice.digital.hmpps.hmppsallocations.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.hmppsallocations.integration.mockserver.WorkforceAllocationsToDeliusApiExtension.Companion.workforceAllocationsToDelius
import uk.gov.justice.digital.hmpps.hmppsallocations.integration.responses.workforceallocationstodelius.documentsResponse

class GetCaseDocumentsTest : IntegrationTestBase() {

  @Test
  fun `must return all documents associated with CRN`() {
    val crn = "X123456"
    workforceAllocationsToDelius.documentsResponse(crn)
    val expectedResponse = objectMapper.readValue(documentsResponse(), object : TypeReference<List<Document>>() {})
    webTestClient.get()
      .uri("/cases/unallocated/$crn/documents")
      .headers { it.authToken(roles = listOf("ROLE_MANAGE_A_WORKFORCE_ALLOCATE")) }
      .exchange()
      .expectStatus()
      .isOk
      .expectBody()
      .json(objectMapper.writeValueAsString(expectedResponse))
  }

  @Test
  fun `must error when retrieving documents errors`() {
    val crn = "X123456"
    workforceAllocationsToDelius.documentsErrorResponse(crn)
    webTestClient.get()
      .uri("/cases/unallocated/$crn/documents")
      .headers { it.authToken(roles = listOf("ROLE_MANAGE_A_WORKFORCE_ALLOCATE")) }
      .exchange()
      .expectStatus()
      .is5xxServerError
  }

  @Test
  fun `cannot get all documents when no auth token supplied`() {
    webTestClient.get()
      .uri("/cases/unallocated/X123456/documents")
      .exchange()
      .expectStatus()
      .isUnauthorized
  }
}
