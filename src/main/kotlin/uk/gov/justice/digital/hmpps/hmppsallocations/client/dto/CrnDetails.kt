package uk.gov.justice.digital.hmpps.hmppsallocations.client.dto

import uk.gov.justice.digital.hmpps.hmppsallocations.client.Name
import java.time.LocalDate

data class CrnDetails(
  val crn: String,
  val name: Name,
  val dateOfBirth: LocalDate,
  val manager: Manager,
  val hasActiveOrder: Boolean,
)

data class Manager(
  val code: String,
  val name: Name,
  val teamCode: String,
  val grade: String?,
  val allocated: Boolean,
)
