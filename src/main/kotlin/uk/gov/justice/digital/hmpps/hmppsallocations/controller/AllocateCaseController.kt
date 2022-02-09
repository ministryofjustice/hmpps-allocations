package uk.gov.justice.digital.hmpps.hmppsallocations.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.hmppsallocations.domain.AllocateCaseOffenderManagers
import uk.gov.justice.digital.hmpps.hmppsallocations.service.GetAllocateCaseService
import javax.persistence.EntityNotFoundException

@RestController
@RequestMapping(produces = [MediaType.APPLICATION_JSON_VALUE])
class AllocateCaseController(
  private val getAllocateCaseService: GetAllocateCaseService
) {

  @Operation(summary = "Retrieve unallocated cases by crn")
  @ApiResponses(
    value = [
      ApiResponse(responseCode = "200", description = "OK"),
      ApiResponse(responseCode = "404", description = "Result Not Found")
    ]
  )
  @PreAuthorize("hasRole('ROLE_MANAGE_A_WORKFORCE_ALLOCATE')")
  @GetMapping("/cases/{crn}/allocate/offenderManagers")
  fun getOffenderManagersToAllocate(@PathVariable(required = true) crn: String): ResponseEntity<AllocateCaseOffenderManagers> =
    ResponseEntity.ok(getAllocateCaseService.getOffenderManagers(crn) ?: throw EntityNotFoundException("Case offender managers Not Found for $crn"))
}
