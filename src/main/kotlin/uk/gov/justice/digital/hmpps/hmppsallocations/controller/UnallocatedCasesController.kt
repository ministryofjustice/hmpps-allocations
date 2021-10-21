package uk.gov.justice.digital.hmpps.hmppsallocations.controller

import com.fasterxml.jackson.annotation.JsonCreator
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

import uk.gov.justice.digital.hmpps.hmppsallocations.jpa.entity.repository.UnallocatedCaseEntity
import uk.gov.justice.digital.hmpps.hmppsallocations.jpa.repository.UnallocatedCasesRepository
import java.time.LocalDateTime

@RestController
@RequestMapping(produces = [MediaType.APPLICATION_JSON_VALUE])
class UnallocatedCasesController(
  private val unallocatedCasesRepository: UnallocatedCasesRepository
) {

  // TODO add role
  @GetMapping("/cases/unallocated")
  fun unallocatedCases(): ResponseEntity<List<UnallocatedCase>> {
    return ResponseEntity.ok(
      unallocatedCasesRepository.findAll().map {
        UnallocatedCase.from(it)
      }
    )
  }
}

data class UnallocatedCase @JsonCreator constructor(
  val name: String,
  val crn: String,
  val tier: String,

  val sentence_date: LocalDateTime,
  val initial_appointment: LocalDateTime?,
  val status: String
) {

  companion object {
    fun from(case: UnallocatedCaseEntity): UnallocatedCase {
      return UnallocatedCase(
        case.name,
        case.crn, case.tier, case.sentence_date, case.initial_appointment, case.status
      )
    }
  }
}

