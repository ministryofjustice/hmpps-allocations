package uk.gov.justice.digital.hmpps.hmppsallocations.domain

import com.fasterxml.jackson.annotation.JsonCreator
import java.time.LocalDate

data class RoshSummary @JsonCreator constructor(
  private val overallRisk: String?,
  val assessedOn: LocalDate?,
  val riskInCommunity: Map<String, String?>,
) {
  fun getOverallRisk(): String = overallRisk ?: "NOT_FOUND"
}
