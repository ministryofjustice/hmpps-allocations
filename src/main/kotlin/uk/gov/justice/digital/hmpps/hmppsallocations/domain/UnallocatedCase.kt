package uk.gov.justice.digital.hmpps.hmppsallocations.controller

import com.fasterxml.jackson.annotation.JsonCreator
import uk.gov.justice.digital.hmpps.hmppsallocations.jpa.entity.UnallocatedCaseEntity
import java.time.LocalDateTime

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
