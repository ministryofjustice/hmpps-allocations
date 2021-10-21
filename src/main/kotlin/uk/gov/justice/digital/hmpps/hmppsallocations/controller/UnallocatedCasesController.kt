package uk.gov.justice.digital.hmpps.hmppsallocations.controller

import com.fasterxml.jackson.annotation.JsonCreator
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
      UnallocatedCase("Dylan Adam Armstrong", "J678910", "C1", "17 October 2021", "22 October 2021",	"Currently managed")
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
