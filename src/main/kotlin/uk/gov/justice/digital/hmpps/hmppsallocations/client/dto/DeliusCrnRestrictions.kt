package uk.gov.justice.digital.hmpps.hmppsallocations.client.dto

data class DeliusCrnRestrictions(
  val hasExclusion: Boolean,
  val hasRestriction: Boolean,
  val apopUserExcluded: Boolean,
)
