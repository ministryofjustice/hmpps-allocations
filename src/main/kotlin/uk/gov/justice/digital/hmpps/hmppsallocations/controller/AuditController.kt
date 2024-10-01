package uk.gov.justice.digital.hmpps.hmppsallocations.controller

import com.fasterxml.jackson.databind.ObjectMapper
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.hmppsallocations.service.auditing.AuditMessage
import uk.gov.justice.digital.hmpps.hmppsallocations.service.auditing.AuditService

@RestController
class AuditController(private val auditService: AuditService, private val objectMapper: ObjectMapper) {
  @Operation(summary = "Send Audit Details")
  @ApiResponses(
    value = [
      ApiResponse(responseCode = "200", description = "OK"),
      ApiResponse(responseCode = "500", description = "Internal server error"),
    ],
  )
  @PreAuthorize("hasRole('ROLE_MANAGE_A_WORKFORCE_ALLOCATE')")
  @PostMapping("allocations/contact/audit")
  fun sendAuditDetails(@RequestBody(required = true) auditMessage: AuditMessage) {
    auditService.sendAuditMessage(
      auditMessage.operationId,
      auditMessage.what,
      auditMessage.who,
      auditMessage.service,
      auditMessage.details,
    )
  }
}
