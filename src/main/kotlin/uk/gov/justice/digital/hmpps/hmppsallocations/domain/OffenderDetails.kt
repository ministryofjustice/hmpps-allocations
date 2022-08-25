package uk.gov.justice.digital.hmpps.hmppsallocations.domain

import com.fasterxml.jackson.annotation.JsonCreator
import java.time.LocalDate

data class OffenderDetails @JsonCreator constructor(
  val firstName: String,
  val surname: String,
  val gender: String,
  val dateOfBirth: LocalDate,
  val otherIds: OffenderDetailsOtherIds?
)

data class OffenderDetailsOtherIds @JsonCreator constructor(
  val pncNumber: String?
)
