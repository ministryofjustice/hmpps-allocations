package uk.gov.justice.digital.hmpps.hmppsallocations.controller

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonInclude
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import org.springframework.http.MediaType.APPLICATION_JSON_VALUE
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.hmppsallocations.domain.OffenderManagerDetails
import uk.gov.justice.digital.hmpps.hmppsallocations.jpa.entity.UnallocatedCaseEntity
import uk.gov.justice.digital.hmpps.hmppsallocations.service.GetUnallocatedCaseService
import uk.gov.justice.digital.hmpps.hmppsallocations.service.exception.EntityNotFoundException

@RestController
@RequestMapping(produces = [APPLICATION_JSON_VALUE])
class ChooseOffenderManagerCaseController(
  private val getUnallocatedCaseService: GetUnallocatedCaseService
) {
  @Operation(summary = "Retrieve unallocated case for choose practitioner page")
  @ApiResponses(
    value = [
      ApiResponse(responseCode = "200", description = "OK"),
      ApiResponse(responseCode = "404", description = "Result Not Found")
    ]
  )
  @PreAuthorize("hasRole('ROLE_MANAGE_A_WORKFORCE_ALLOCATE')")
  @GetMapping("/cases/unallocated/{crn}/convictions/{convictionId}/practitionerCase")
  fun getOffenderManagerCase(
    @PathVariable(required = true) crn: String,
    @PathVariable(required = true) convictionId: Long
  ): ResponseEntity<ChooseOffenderManagerCase> =
    ResponseEntity.ok(
      getUnallocatedCaseService.getChooseOffenderManagerCase(crn, convictionId)
        ?: throw EntityNotFoundException("OffenderManager Case Not Found for $crn")
    )
}

data class ChooseOffenderManagerCase @JsonCreator constructor(
  @Schema(description = "name", example = "John William")
  val name: String,
  @Schema(description = "crn", example = "J678910")
  val crn: String,
  @Schema(description = "tier", example = "C1")
  val tier: String,
  @Schema(description = "status", example = "Currently managed")
  val status: String,
  @JsonInclude(JsonInclude.Include.NON_NULL)
  val offenderManager: OffenderManagerDetails?,
  @Schema(description = "convictionId", example = "123456789")
  val convictionId: Long
) {
  companion object {
    fun from(e: UnallocatedCaseEntity): ChooseOffenderManagerCase {
      var offenderManager: OffenderManagerDetails? = null
      e.offenderManagerSurname?.let {
        offenderManager = OffenderManagerDetails(
          e.offenderManagerForename,
          e.offenderManagerSurname,
          e.offenderManagerGrade
        )
      }
      return ChooseOffenderManagerCase(
        e.name,
        e.crn,
        e.tier,
        e.status,
        offenderManager,
        e.convictionId
      )
    }
  }
}
