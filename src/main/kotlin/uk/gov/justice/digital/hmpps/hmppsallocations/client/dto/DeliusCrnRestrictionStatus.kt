package uk.gov.justice.digital.hmpps.hmppsallocations.client.dto

data class DeliusCrnRestrictionStatus(
  val crn: String,
  val isRestricted: Boolean,
  val isRedacted: Boolean,
)
