package uk.gov.justice.digital.hmpps.hmppsallocations.client.dto

import uk.gov.justice.digital.hmpps.hmppsallocations.client.Name
import java.math.BigInteger
import java.time.LocalDate

data class DeliusRisk constructor(
  val name: Name,
  val crn: String,
  val activeRegistrations: List<Registrations>,
  val inactiveRegistrations: List<Registrations>,
  val ogrs: Ogrs?
)

data class Registrations constructor(
  val description: String?,
  val startDate: LocalDate?,
  val endDate: LocalDate?,
  val notes: String?
)

data class Ogrs constructor(
  val lastUpdatedDate: LocalDate,
  val score: BigInteger
)
