package uk.gov.justice.digital.hmpps.hmppsallocations.client.dto

import uk.gov.justice.digital.hmpps.hmppsallocations.client.Name

data class StaffMember constructor(
  val code: String,
  val name: Name,
  val email: String?
)
