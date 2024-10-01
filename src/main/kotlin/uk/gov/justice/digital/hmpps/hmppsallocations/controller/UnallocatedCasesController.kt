package uk.gov.justice.digital.hmpps.hmppsallocations.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import org.springframework.http.MediaType
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Flux
import uk.gov.justice.digital.hmpps.hmppsallocations.domain.CaseCountByTeam
import uk.gov.justice.digital.hmpps.hmppsallocations.domain.CaseOverview
import uk.gov.justice.digital.hmpps.hmppsallocations.domain.UnallocatedCaseConfirmInstructions
import uk.gov.justice.digital.hmpps.hmppsallocations.domain.UnallocatedCaseConvictions
import uk.gov.justice.digital.hmpps.hmppsallocations.domain.UnallocatedCaseDetails
import uk.gov.justice.digital.hmpps.hmppsallocations.domain.UnallocatedCaseRisks
import uk.gov.justice.digital.hmpps.hmppsallocations.service.GetUnallocatedCaseService
import uk.gov.justice.digital.hmpps.hmppsallocations.service.auditing.AuditObject
import uk.gov.justice.digital.hmpps.hmppsallocations.service.auditing.AuditService
import uk.gov.justice.digital.hmpps.hmppsallocations.service.exception.EntityNotFoundException

@RestController
@RequestMapping(produces = [MediaType.APPLICATION_JSON_VALUE])
class UnallocatedCasesController(
  private val getUnallocatedCaseService: GetUnallocatedCaseService,
  private val auditService: AuditService,
) {
  @Operation(summary = "Retrieve count of all unallocated cases by team")
  @ApiResponses(
    value = [
      ApiResponse(responseCode = "200", description = "OK"),
    ],
  )
  @PreAuthorize("hasRole('ROLE_MANAGE_A_WORKFORCE_ALLOCATE')")
  @GetMapping("/cases/unallocated/teamCount")
  fun getCaseCountByTeam(@RequestParam(required = true) teams: List<String>): Flux<CaseCountByTeam> {
    return getUnallocatedCaseService.getCaseCountByTeam(teams)
  }

  @Operation(summary = "Retrieve unallocated case by crn and conviction id")
  @ApiResponses(
    value = [
      ApiResponse(responseCode = "200", description = "OK"),
      ApiResponse(responseCode = "404", description = "Result Not Found"),
    ],
  )
  @PreAuthorize("hasRole('ROLE_MANAGE_A_WORKFORCE_ALLOCATE')")
  @GetMapping("/cases/unallocated/{crn}/convictions/{convictionNumber}")
  suspend fun getUnallocatedCase(
    @PathVariable(required = true) crn: String,
    @PathVariable(required = true) convictionNumber: Long,
  ): UnallocatedCaseDetails {
    val user = getLoggedInUser()
    auditService.sendAuditMessage(CaseAuditObject(crn, convictionNumber.toString()), user, crn, "Get Unallocated Case")
    return getUnallocatedCaseService.getCase(crn, convictionNumber) ?: throw EntityNotFoundException("Unallocated case Not Found for $crn")
  }

  @Operation(summary = "Retrieve unallocated case overview by crn and conviction id")
  @ApiResponses(
    value = [
      ApiResponse(responseCode = "200", description = "OK"),
      ApiResponse(responseCode = "404", description = "Result Not Found"),
    ],
  )
  @PreAuthorize("hasRole('ROLE_MANAGE_A_WORKFORCE_ALLOCATE')")
  @GetMapping("/cases/unallocated/{crn}/convictions/{convictionNumber}/overview")
  suspend fun getUnallocatedCaseOverview(
    @PathVariable(required = true) crn: String,
    @PathVariable(required = true) convictionNumber: Long,
  ): CaseOverview {
    val user = getLoggedInUser()
    auditService.sendAuditMessage(CaseAuditObject(crn, convictionNumber.toString()), user, crn, "Get Unallocated Case Overview")
    return getUnallocatedCaseService.getCaseOverview(crn, convictionNumber)
      ?: throw EntityNotFoundException("Unallocated case Not Found for $crn")
  }

  @Operation(summary = "Retrieve probation record")
  @ApiResponses(
    value = [
      ApiResponse(responseCode = "200", description = "OK"),
      ApiResponse(responseCode = "404", description = "Result Not Found"),
    ],
  )
  @PreAuthorize("hasRole('ROLE_MANAGE_A_WORKFORCE_ALLOCATE')")
  @GetMapping("/cases/unallocated/{crn}/record/exclude-conviction/{excludeConvictionNumber}")
  suspend fun getUnallocatedCaseProbationRecord(
    @PathVariable(required = true) crn: String,
    @PathVariable(required = true) excludeConvictionNumber: Long,
  ): UnallocatedCaseConvictions {
    val user = getLoggedInUser()
    auditService.sendAuditMessage(CaseAuditObject(crn, excludeConvictionNumber.toString()), user, crn, "Get Unallocated Case Probation Record")
    return getUnallocatedCaseService.getCaseConvictions(crn, excludeConvictionNumber)
      ?: throw EntityNotFoundException("Unallocated case Not Found for $crn")
  }

  @Operation(summary = "Retrieve unallocated case risks by crn")
  @ApiResponses(
    value = [
      ApiResponse(responseCode = "200", description = "OK"),
      ApiResponse(responseCode = "404", description = "Result Not Found"),
    ],
  )
  @PreAuthorize("hasRole('ROLE_MANAGE_A_WORKFORCE_ALLOCATE')")
  @GetMapping("/cases/unallocated/{crn}/convictions/{convictionNumber}/risks")
  suspend fun getUnallocatedCaseRisks(
    @PathVariable(required = true) crn: String,
    @PathVariable(required = true) convictionNumber: Long,
  ): UnallocatedCaseRisks {
    val user = getLoggedInUser()
    auditService.sendAuditMessage(CaseAuditObject(crn, convictionNumber.toString()), user, crn, "Get Unallocated Risks")
    return getUnallocatedCaseService.getCaseRisks(crn, convictionNumber) ?: throw EntityNotFoundException("Unallocated case risks Not Found for $crn")
  }

  @Operation(summary = "Retrieve unallocated case confirm instructions by crn")
  @ApiResponses(
    value = [
      ApiResponse(responseCode = "200", description = "OK"),
      ApiResponse(responseCode = "404", description = "Result Not Found"),
    ],
  )
  @PreAuthorize("hasRole('ROLE_MANAGE_A_WORKFORCE_ALLOCATE')")
  @GetMapping("/cases/unallocated/{crn}/convictions/{convictionNumber}/confirm-instructions")
  suspend fun getUnallocatedCaseConfirmInstructions(
    @PathVariable(required = true) crn: String,
    @PathVariable(required = true) convictionNumber: Long,
    @RequestParam(required = true) staffCode: String,
  ): UnallocatedCaseConfirmInstructions {
    val user = getLoggedInUser()
    auditService.sendAuditMessage(
      ConfirmInstructionsAuditObject(crn, convictionNumber.toString(), staffCode),
      user,
      crn,
      "Get Unallocated Confirm Instructions",
    )
    return getUnallocatedCaseService.getCaseConfirmInstructions(crn, convictionNumber, staffCode)
      ?: throw EntityNotFoundException("Unallocated case Not Found for $crn and conviction $convictionNumber")
  }

  private fun getLoggedInUser(): String {
    try {
      return SecurityContextHolder.getContext().authentication.name
    } catch (e: NullPointerException) {
      return "NoNameFound"
    }
  }

  private class CaseAuditObject(crn: String, convictionNumber: String) : AuditObject
  private class ConfirmInstructionsAuditObject(crn: String, convictionNumber: String, staffCode: String) : AuditObject
}
