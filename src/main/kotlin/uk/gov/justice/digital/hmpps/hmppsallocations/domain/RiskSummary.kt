package uk.gov.justice.digital.hmpps.hmppsallocations.domain

import com.fasterxml.jackson.annotation.JsonCreator
import java.time.LocalDateTime

data class RiskSummary @JsonCreator constructor(
  val overallRiskLevel: String?,
  val assessedOn: LocalDateTime?
)
