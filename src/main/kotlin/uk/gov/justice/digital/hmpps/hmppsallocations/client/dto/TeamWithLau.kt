package uk.gov.justice.digital.hmpps.hmppsallocations.client.dto

data class TeamWithLau constructor(
  val code: String = "",
  val description: String = "",
  val localAdminUnit: Lau = Lau("","", Pdu("", "", Provider("", ""))),
)
