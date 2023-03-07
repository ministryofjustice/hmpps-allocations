package uk.gov.justice.digital.hmpps.hmppsallocations.client.dto

import com.fasterxml.jackson.annotation.JsonCreator
import uk.gov.justice.digital.hmpps.hmppsallocations.client.Name

data class PersonOnProbationStaffDetailsResponse @JsonCreator constructor(
  val crn: String,
  val name: Name,
  val staff: StaffMember
)
