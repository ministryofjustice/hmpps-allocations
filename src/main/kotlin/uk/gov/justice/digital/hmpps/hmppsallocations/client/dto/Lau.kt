package uk.gov.justice.digital.hmpps.hmppsallocations.client.dto

data class Lau constructor(
  val code: String,
  val description: String,
  val probationDeliveryUnit: Pdu,
)
