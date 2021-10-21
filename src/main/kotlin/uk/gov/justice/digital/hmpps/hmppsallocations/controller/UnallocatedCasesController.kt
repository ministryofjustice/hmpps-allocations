package uk.gov.justice.digital.hmpps.hmppsallocations.controller

import com.fasterxml.jackson.annotation.JsonCreator
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping(produces = [MediaType.APPLICATION_JSON_VALUE])
class UnallocatedCasesController {

  // TODO add role
  @GetMapping("/cases/unallocated")
  fun unallocatedCases(): ResponseEntity<List<UnallocatedCase>> = ResponseEntity.ok(
    listOf(
      UnallocatedCase("Dylan Adam Armstrong", "J678910", "C1", "17 October 2021", "22 October 2021",	"Currently managed"),
      UnallocatedCase("Andrei Edwards", "J680648", "A1", "18 October 2021", "23 October 2021", "New to probation"),
      UnallocatedCase("Hannah Francis", "J680660", "C2", "20 October 2021", "", "Previously managed")

    )
  )
}

data class UnallocatedCase @JsonCreator constructor(
  val name: String,
  val crn: String,
  val tier: String,
  val sentence_date: String,
  val initial_appointment: String,
  val status: String
)
