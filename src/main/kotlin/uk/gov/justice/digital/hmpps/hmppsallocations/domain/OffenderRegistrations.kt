package uk.gov.justice.digital.hmpps.hmppsallocations.domain

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonFormat
import java.time.LocalDate

data class OffenderRegistrations @JsonCreator constructor(
  val registrations: List<OffenderRegistration>?
)

data class OffenderRegistration @JsonCreator constructor(
  val active: Boolean,
  val type: OffenderRegistrationType,
  @JsonFormat(pattern = "yyyy-MM-dd", shape = JsonFormat.Shape.STRING)
  val startDate: LocalDate,
  @JsonFormat(pattern = "yyyy-MM-dd", shape = JsonFormat.Shape.STRING)
  val nextReviewDate: LocalDate?,
  val notes: String?,
  @JsonFormat(pattern = "yyyy-MM-dd", shape = JsonFormat.Shape.STRING)
  val endDate: LocalDate?
)

data class OffenderRegistrationType @JsonCreator constructor(
  val description: String
)
