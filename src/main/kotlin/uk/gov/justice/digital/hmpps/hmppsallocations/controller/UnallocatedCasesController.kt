package uk.gov.justice.digital.hmpps.hmppsallocations.controller

import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.Collections

@RestController
@RequestMapping(produces = [MediaType.APPLICATION_JSON_VALUE])
class UnallocatedCasesController {

  // TODO add role
  @GetMapping("/cases/unallocated")
  fun unallocatedCases(): ResponseEntity<List<UnallocatedCase>> = ResponseEntity.ok(
    Collections.singletonList(
      UnallocatedCase()
    )
  )
}

class UnallocatedCase
