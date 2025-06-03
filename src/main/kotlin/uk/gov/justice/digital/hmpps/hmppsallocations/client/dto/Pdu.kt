package uk.gov.justice.digital.hmpps.hmppsallocations.client.dto

data class Pdu constructor(
  val code: String = "",
  val description: String = "",
  val provider: Provider = Provider("", ""),
)
