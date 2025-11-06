package uk.gov.justice.digital.hmpps.hmppsallocations.controller

import org.springframework.http.MediaType
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.hmppsallocations.config.Principal
import uk.gov.justice.digital.hmpps.hmppsallocations.domain.AllocatedCaseDetails
import uk.gov.justice.digital.hmpps.hmppsallocations.service.GetAllocatedCaseService
import uk.gov.justice.digital.hmpps.hmppsallocations.service.exception.EntityNotFoundException

@Suppress("StringLiteralDuplication")
@RestController
@RequestMapping(produces = [MediaType.APPLICATION_JSON_VALUE])
class ReallocationCasesController(private val getAllocatedCaseService: GetAllocatedCaseService) {

  // @PreAuthorize("hasRole('ROLE_MANAGE_A_WORKFORCE_ALLOCATE')")
  @GetMapping("/cases/allocated/{crn}")
  suspend fun getAllocatedCase(
    @AuthenticationPrincipal principal: Principal,
    @PathVariable(required = true) crn: String,
  ): AllocatedCaseDetails = getAllocatedCaseService.getCase(principal.userName, crn)
    ?: throw EntityNotFoundException("ALLOCATED_CASE_NOT_FOUND_FOR $crn")
}
