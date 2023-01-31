package uk.gov.justice.digital.hmpps.hmppsallocations.integration.unallocatedcases

import org.junit.jupiter.api.Test
import org.springframework.http.ContentDisposition
import uk.gov.justice.digital.hmpps.hmppsallocations.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.hmppsallocations.integration.mockserver.WorkforceAllocationsToDeliusApiExtension.Companion.workforceAllocationsToDelius
import java.util.UUID

class GetCaseDocumentTest : IntegrationTestBase() {

  @Test
  fun `can get a document`() {
    val crn = "J678910"
    val documentId = UUID.randomUUID().toString()
    workforceAllocationsToDelius.getDocument(crn, documentId)
    webTestClient.get()
      .uri("/cases/unallocated/$crn/documents/$documentId")
      .headers { it.authToken(roles = listOf("ROLE_MANAGE_A_WORKFORCE_ALLOCATE")) }
      .exchange()
      .expectStatus()
      .isOk
      .expectHeader()
      .contentDisposition(ContentDisposition.parse("attachment; filename=\"sample_word_doc.doc\""))
      .expectHeader()
      .contentType("application/msword;charset=UTF-8")
  }

  @Test
  fun `can get a document without conviction id`() {
    val crn = "J678910"
    val documentId = UUID.randomUUID().toString()
    workforceAllocationsToDelius.getDocument(crn, documentId)
    webTestClient.get()
      .uri("/cases/unallocated/$crn/documents/$documentId")
      .headers { it.authToken(roles = listOf("ROLE_MANAGE_A_WORKFORCE_ALLOCATE")) }
      .exchange()
      .expectStatus()
      .isOk
      .expectHeader()
      .contentDisposition(ContentDisposition.parse("attachment; filename=\"sample_word_doc.doc\""))
      .expectHeader()
      .contentType("application/msword;charset=UTF-8")
  }
}
