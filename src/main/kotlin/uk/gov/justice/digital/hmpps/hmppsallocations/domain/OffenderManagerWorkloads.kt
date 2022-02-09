package uk.gov.justice.digital.hmpps.hmppsallocations.domain

import com.fasterxml.jackson.annotation.JsonCreator
import java.math.BigDecimal
import java.math.BigInteger

data class OffenderManagerWorkloads @JsonCreator constructor(
  val offenderManagers: List<OffenderManagerWorkload>
)

data class OffenderManagerWorkload @JsonCreator constructor(
  val forename: String,
  val surname: String,
  var grade: String,
  val totalCommunityCases: BigInteger,
  val totalCustodyCases: BigInteger,
  val capacity: BigDecimal,
  val code: String
)
