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
import uk.gov.justice.digital.hmpps.hmppsallocations.client.dto.RegionList
import uk.gov.justice.digital.hmpps.hmppsallocations.service.GetRegionsService
import uk.gov.justice.digital.hmpps.hmppsallocations.service.ValidateAccessService
import uk.gov.justice.digital.hmpps.hmppsallocations.service.exception.NotAllowedForAccessException

@RestController
@RequestMapping(produces = [MediaType.APPLICATION_JSON_VALUE])
class RegionAccessController(private val getRegionsService: GetRegionsService,
                             private val validateAccessService: ValidateAccessService) {

  @Operation(summary = "Retrieve all user regions")
  @ApiResponses(
    value = [
      ApiResponse(responseCode = "200", description = "OK"),
    ],
  )
  @PreAuthorize("hasRole('ROLE_MANAGE_A_WORKFORCE_ALLOCATE')")
  @GetMapping("/user/{staffId}/regions")
  suspend fun getAccessibleRegionsByUser(@PathVariable staffId: String): RegionList = getRegionsService.getRegionsByUser(staffId)

  @Operation(summary = "Check for user access")
  @ApiResponses(
    value = [
      ApiResponse(responseCode = "200", description = "OK"),
      ApiResponse(responseCode = "403", description = "Forbidden"),
    ],
  )
  @PreAuthorize("hasRole('ROLE_MANAGE_A_WORKFORCE_ALLOCATE')")
  @GetMapping("/user/{staffId}/crn/{crn}/is-allowed")
  suspend fun getValidatedAccess(@PathVariable staffId: String, @PathVariable crn: String): ResponseEntity<Void> {
    return try {
      validateAccessService.validateUserAccess(staffId, crn)
      ResponseEntity.ok().build()
    } catch (e: NotAllowedForAccessException) {
      ResponseEntity.status(403).build()
    }
  }
}
