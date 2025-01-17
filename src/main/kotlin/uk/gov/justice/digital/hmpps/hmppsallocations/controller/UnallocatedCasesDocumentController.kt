package uk.gov.justice.digital.hmpps.hmppsallocations.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import kotlinx.coroutines.flow.Flow
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.core.io.Resource
import org.springframework.http.MediaType.APPLICATION_JSON_VALUE
import org.springframework.http.MediaType.APPLICATION_OCTET_STREAM_VALUE
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Mono
import uk.gov.justice.digital.hmpps.hmppsallocations.client.Document
import uk.gov.justice.digital.hmpps.hmppsallocations.client.WorkforceAllocationsToDeliusApiClient

@RestController
class UnallocatedCasesDocumentController(@Qualifier("workforceAllocationsToDeliusApiClientUserEnhanced") private val workforceAllocationsToDeliusApiClient: WorkforceAllocationsToDeliusApiClient) {

  @Operation(summary = "Download Document by ID")
  @ApiResponses(
    value = [
      ApiResponse(responseCode = "200", description = "OK"),
      ApiResponse(responseCode = "404", description = "Result Not Found"),
    ],
  )
  @PreAuthorize("hasRole('ROLE_MANAGE_A_WORKFORCE_ALLOCATE')")
  @GetMapping(path = ["/cases/unallocated/{crn}/documents/{documentId}"], produces = [APPLICATION_OCTET_STREAM_VALUE])
  fun getUnallocatedCaseDocument(
    @PathVariable(required = true) crn: String,
    @PathVariable(required = true) documentId: String,
  ): Mono<ResponseEntity<Resource>> = workforceAllocationsToDeliusApiClient.getDocumentById(crn, documentId)

  @Operation(summary = "Retrieve all documents by CRN")
  @ApiResponses(
    value = [
      ApiResponse(responseCode = "200", description = "OK"),
    ],
  )
  @PreAuthorize("hasRole('ROLE_MANAGE_A_WORKFORCE_ALLOCATE')")
  @GetMapping(path = ["/cases/unallocated/{crn}/documents"], produces = [APPLICATION_JSON_VALUE])
  suspend fun getUnallocatedCaseDocuments(@PathVariable(required = true) crn: String): Flow<Document> = workforceAllocationsToDeliusApiClient.getDocuments(crn)
}
