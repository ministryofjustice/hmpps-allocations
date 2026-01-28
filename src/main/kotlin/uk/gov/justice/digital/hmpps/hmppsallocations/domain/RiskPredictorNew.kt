package uk.gov.justice.digital.hmpps.hmppsallocations.domain

import com.fasterxml.jackson.annotation.JsonCreator
import java.time.LocalDateTime

data class RiskPredictorNew @JsonCreator constructor(
  val completedDate: LocalDateTime?,
  val outputVersion: String,
  val source: String,
  val status: String,
  val output: Any,
)
