package uk.gov.justice.digital.hmpps.hmppsallocations.controller

import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping(produces = [MediaType.APPLICATION_JSON_VALUE])
class UnallocatedCasesController {

  @GetMapping("/cases/unallocated")
  fun unallocatedCases(): ResponseEntity<UnallocatedCases> = ResponseEntity.ok(UnallocatedCases())

  class UnallocatedCases
}
