package uk.gov.justice.digital.hmpps.hmppsallocations.client.dto

data class DeliusAccessRestrictionDetails constructor(
  val crn: String,
  val excludedFrom: List<DeliusApopUser>,
  val restrictedTo: List<DeliusApopUser>,
  val exclusionMessage: String,
  val restrictionMessage: String,
)
