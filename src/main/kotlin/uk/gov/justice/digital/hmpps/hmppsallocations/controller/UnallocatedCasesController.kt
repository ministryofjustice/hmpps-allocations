package uk.gov.justice.digital.hmpps.hmppsallocations.controller

import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.hmppsallocations.service.UnallocatedCasesService

@RestController
@RequestMapping(produces = [MediaType.APPLICATION_JSON_VALUE])
class UnallocatedCasesController(
  private val unallocatedCasesService: UnallocatedCasesService
) {

  // TODO add role
  @PreAuthorize("hasRole('ROLE_MANAGE_A_WORKFORCE_ALLOCATE')")
  @GetMapping("/cases/unallocated")
  fun getUnallocatedCases(): ResponseEntity<List<UnallocatedCase>> {
    return ResponseEntity.ok(
      unallocatedCasesService.getAll()
    )
  }
}
