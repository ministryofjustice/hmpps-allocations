package uk.gov.justice.digital.hmpps.hmppsallocations.domain

import com.fasterxml.jackson.annotation.JsonCreator
import java.math.BigDecimal

data class PotentialOffenderManagerWorkload @JsonCreator constructor(
  val forename: String,
  val surname: String,
  var grade: String,
  val capacity: BigDecimal,
  val code: String,
  val potentialCapacity: BigDecimal,
)
