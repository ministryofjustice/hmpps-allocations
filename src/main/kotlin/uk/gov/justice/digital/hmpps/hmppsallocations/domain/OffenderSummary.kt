package uk.gov.justice.digital.hmpps.hmppsallocations.domain

import com.fasterxml.jackson.annotation.JsonCreator
import java.time.LocalDate

data class OffenderSummary @JsonCreator constructor(
  val firstName: String,
  val surname: String,
  val gender: String,
  val dateOfBirth: LocalDate
)
