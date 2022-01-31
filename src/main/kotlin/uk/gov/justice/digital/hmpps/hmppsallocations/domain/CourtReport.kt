package uk.gov.justice.digital.hmpps.hmppsallocations.domain

import com.fasterxml.jackson.annotation.JsonCreator
import java.time.LocalDateTime

data class CourtReport @JsonCreator constructor(
  val courtReportId: Long,
  val completedDate: LocalDateTime?,
  val courtReportType: CourtReportType
)

data class CourtReportType @JsonCreator constructor(
  val code: String,
  var description: String
)
