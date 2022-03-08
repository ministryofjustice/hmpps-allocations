package uk.gov.justice.digital.hmpps.hmppsallocations.domain

import com.fasterxml.jackson.annotation.JsonCreator
import java.math.BigDecimal
import java.math.BigInteger

data class OffenderManagerOverview @JsonCreator constructor(
  val forename: String,
  val surname: String,
  var grade: String,
  val capacity: BigDecimal,
  val code: String,
  val teamName: String,
  val totalCases: BigDecimal,
  val weeklyHours: BigDecimal,
  val totalReductionHours: BigDecimal,
  val pointsAvailable: BigInteger,
  val pointsUsed: BigInteger,
  val pointsRemaining: BigInteger
)
