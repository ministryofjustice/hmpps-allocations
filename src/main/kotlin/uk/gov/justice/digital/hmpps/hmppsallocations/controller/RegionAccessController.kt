package uk.gov.justice.digital.hmpps.hmppsallocations.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import org.springframework.http.HttpStatus
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
import uk.gov.justice.digital.hmpps.hmppsallocations.service.exception.EntityNotFoundException
import uk.gov.justice.digital.hmpps.hmppsallocations.service.exception.NotAllowedForAccessException

@RestController
@RequestMapping(produces = [MediaType.APPLICATION_JSON_VALUE])
class RegionAccessController(
  private val getRegionsService: GetRegionsService,
  private val validateAccessService: ValidateAccessService,
) {

  @Operation(summary = "Retrieve all user regions")
  @ApiResponses(
    value = [
      ApiResponse(responseCode = "200", description = "OK"),
    ],
  )
  @PreAuthorize("hasRole('ROLE_MANAGE_A_WORKFORCE_ALLOCATE')")
  @GetMapping("/user/{userName}/regions")
  suspend fun getAccessibleRegionsByUser(@PathVariable userName: String): RegionList = getRegionsService.getRegionsByUser(userName)

  @Operation(summary = "Check for user access")
  @ApiResponses(
    value = [
      ApiResponse(responseCode = "200", description = "OK"),
      ApiResponse(responseCode = "403", description = "Forbidden"),
      ApiResponse(responseCode = "404", description = "Result Not Found"),
    ],
  )
  @PreAuthorize("hasRole('ROLE_MANAGE_A_WORKFORCE_ALLOCATE')")
  @GetMapping("/user/{userName}/crn/{crn}/conviction/{convictionNumber}/is-allowed")
  suspend fun getValidatedAccess(@PathVariable userName: String, @PathVariable crn: String, @PathVariable convictionNumber: String): ResponseEntity<String> = try {
    validateAccessService.validateUserAccess(userName, crn, convictionNumber)
    ResponseEntity<String>("Ok", HttpStatus.OK)
  } catch (e: NotAllowedForAccessException) {
    ResponseEntity<String>(e.message, HttpStatus.FORBIDDEN)
  } catch (e: EntityNotFoundException) {
    ResponseEntity<String>(e.message, HttpStatus.NOT_FOUND)
  }

  @Operation(summary = "Check for user access to PDU")
  @ApiResponses(
    value = [
      ApiResponse(responseCode = "200", description = "OK"),
      ApiResponse(responseCode = "403", description = "Forbidden"),
      ApiResponse(responseCode = "404", description = "Result Not Found"),
    ],
  )
  @PreAuthorize("hasRole('ROLE_MANAGE_A_WORKFORCE_ALLOCATE')")
  @GetMapping("/user/{userName}/pdu/{pdu}/is-allowed")
  suspend fun getValidatedPduAccess(@PathVariable userName: String, @PathVariable pdu: String): ResponseEntity<String> = try {
    validateAccessService.validateUserAccess(userName, pdu)
    ResponseEntity<String>("Ok", HttpStatus.OK)
  } catch (e: NotAllowedForAccessException) {
    ResponseEntity<String>(e.message, HttpStatus.FORBIDDEN)
  } catch (e: EntityNotFoundException) {
    ResponseEntity<String>(e.message, HttpStatus.NOT_FOUND)
  }

  @Operation(summary = "Check for user access for region")
  @ApiResponses(
    value = [
      ApiResponse(responseCode = "200", description = "OK"),
      ApiResponse(responseCode = "403", description = "Forbidden"),
      ApiResponse(responseCode = "404", description = "Result Not Found"),
    ],
  )
  @PreAuthorize("hasRole('ROLE_MANAGE_A_WORKFORCE_ALLOCATE')")
  @GetMapping("/user/{userName}/region/{region}/is-allowed")
  suspend fun getValidatedRegionAccess(@PathVariable userName: String, @PathVariable region: String): ResponseEntity<String> = try {
    validateAccessService.validateUserRegionAccess(userName, region)
    ResponseEntity<String>("Ok", HttpStatus.OK)
  } catch (e: NotAllowedForAccessException) {
    ResponseEntity<String>(e.message, HttpStatus.FORBIDDEN)
  } catch (e: EntityNotFoundException) {
    ResponseEntity<String>(e.message, HttpStatus.NOT_FOUND)
  }

  @Operation(summary = "Check staff member can access CRN")
  @ApiResponses(
    value = [
      ApiResponse(responseCode = "200", description = "OK"),
      ApiResponse(responseCode = "403", description = "Forbidden"),
      ApiResponse(responseCode = "404", description = "Result Not Found"),
    ],
  )
  @PreAuthorize("hasRole('ROLE_MANAGE_A_WORKFORCE_ALLOCATE')")
  @GetMapping("/user/{userName}/crn/{crn}/is-allowed")
  suspend fun getValidatedCrnAccess(@PathVariable userName: String, @PathVariable crn: String): ResponseEntity<String> = try {
    validateAccessService.validateUserAccessForCrnAndStaff(userName, crn)
    ResponseEntity<String>("Ok", HttpStatus.OK)
  } catch (e: NotAllowedForAccessException) {
    ResponseEntity<String>(e.message, HttpStatus.FORBIDDEN)
  } catch (e: EntityNotFoundException) {
    ResponseEntity<String>(e.message, HttpStatus.NOT_FOUND)
  }
}
