package uk.gov.justice.digital.hmpps.hmppsallocations.client.dto

import uk.gov.justice.digital.hmpps.hmppsallocations.client.Name
import java.math.BigDecimal
import java.math.BigInteger
import java.time.LocalDate

data class DeliusRisk constructor(
  val name: Name,
  val crn: String,
  val activeRegistrations: List<ActiveRegistration>,
  val inactiveRegistrations: List<InactiveRegistration>,
  val ogrs: Ogrs?,
  val rosh: Rosh?,
  val rsr: Rsr?
)

data class ActiveRegistration constructor(
  val description: String,
  val startDate: LocalDate,
  val notes: String
)

data class InactiveRegistration constructor(
  val description: String,
  val startDate: LocalDate,
  val endDate: LocalDate,
  val notes: String
)

data class Ogrs constructor(
  val lastUpdatedDate: LocalDate,
  val score: BigInteger
)

data class Rosh constructor(
  val overallRisk: String,
  val assessmentDate: LocalDate,
  val riskInCommunity: Map<String, String?>
)

data class Rsr constructor(
  val percentageScore: BigDecimal,
  val levelScore: String,
  val completedDate: LocalDate
)
