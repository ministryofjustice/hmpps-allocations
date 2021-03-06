package uk.gov.justice.digital.hmpps.hmppsallocations.domain

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonFormat
import com.fasterxml.jackson.annotation.JsonFormat.Shape.STRING
import uk.gov.justice.digital.hmpps.hmppsallocations.mapper.deliusToStaffGrade
import java.time.LocalDate
import java.time.LocalDateTime

data class Conviction @JsonCreator constructor(
  @JsonFormat(pattern = "yyyy-MM-dd", shape = STRING)
  val convictionDate: LocalDate?,
  val sentence: Sentence?,
  val active: Boolean,
  val offences: List<Offence>,
  val convictionId: Long,
  val orderManagers: List<OrderManager>,
  val custody: Custody?,
)

data class OrderManager @JsonCreator constructor (
  val dateStartOfAllocation: LocalDateTime?,
  val name: String?,
  val staffCode: String?,
  val gradeCode: String?,
  val teamCode: String?,
  val probationAreaCode: String
) {
  val staffGrade: String? = deliusToStaffGrade(this.gradeCode)
}

data class Custody @JsonCreator constructor(
  val status: CustodyStatus,
)

data class CustodyStatus @JsonCreator constructor(
  val code: String,
)
