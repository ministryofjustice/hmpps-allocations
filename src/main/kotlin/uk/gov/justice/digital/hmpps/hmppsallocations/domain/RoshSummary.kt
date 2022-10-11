package uk.gov.justice.digital.hmpps.hmppsallocations.domain

import com.fasterxml.jackson.annotation.JsonCreator
import java.time.LocalDate

data class RoshSummary @JsonCreator constructor(
  val overallRisk: String?,
  val assessedOn: LocalDate?,
  val riskInCommunity: Map<String, String?>
)
