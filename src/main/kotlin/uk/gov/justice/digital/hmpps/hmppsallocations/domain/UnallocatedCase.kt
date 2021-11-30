package uk.gov.justice.digital.hmpps.hmppsallocations.domain

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonFormat
import com.fasterxml.jackson.annotation.JsonFormat.Shape.STRING
import uk.gov.justice.digital.hmpps.hmppsallocations.jpa.entity.UnallocatedCaseEntity
import java.time.LocalDate

data class UnallocatedCase @JsonCreator constructor(
  val name: String,
  val crn: String,
  val tier: String,
  @JsonFormat(pattern = "yyyy-MM-dd", shape = STRING)
  val sentenceDate: LocalDate,
  @JsonFormat(pattern = "yyyy-MM-dd", shape = STRING)
  val initialAppointment: LocalDate?,
  val status: String,
  @JsonFormat(pattern = "yyyy-MM-dd", shape = STRING)
  val previousConvictionEndDate: LocalDate?
) {

  companion object {
    fun from(case: UnallocatedCaseEntity): UnallocatedCase {
      return UnallocatedCase(
        case.name,
        case.crn, case.tier, case.sentence_date, case.initial_appointment, case.status,
        case.previous_conviction_date
      )
    }
  }
}
