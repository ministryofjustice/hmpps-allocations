package uk.gov.justice.digital.hmpps.hmppsallocations.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import org.springframework.http.MediaType
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.hmppsallocations.client.dto.CrnDetails
import uk.gov.justice.digital.hmpps.hmppsallocations.service.CrnLookupService

@RestController
@RequestMapping(produces = [MediaType.APPLICATION_JSON_VALUE])
class CrnController(
  private val crnLookupService: CrnLookupService,
) {
  @Operation(summary = "Information about person on probation by crn")
  @ApiResponses(
    value = [
      ApiResponse(responseCode = "200", description = "OK"),
      ApiResponse(responseCode = "403", description = "Unauthorized"),
      ApiResponse(responseCode = "404", description = "Result Not Found"),
    ],
  )
  @PreAuthorize("hasRole('ROLE_MANAGE_A_WORKFORCE_ALLOCATE')")
  @GetMapping("/allocated/crn/{crn}")
  suspend fun getCrn(@PathVariable crn: String): CrnDetails = crnLookupService.getCrnDetails(crn)
}
