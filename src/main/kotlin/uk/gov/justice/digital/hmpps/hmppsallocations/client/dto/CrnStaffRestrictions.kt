package uk.gov.justice.digital.hmpps.hmppsallocations.client.dto

data class CrnStaffRestrictions(
  val crn: String,
  val staffRestrictions: List<CrnStaffRestrictionDetail>,
)
