package uk.gov.justice.digital.hmpps.hmppsallocations.client.dto

import com.fasterxml.jackson.annotation.JsonCreator
import uk.gov.justice.digital.hmpps.hmppsallocations.client.Name

data class UnallocatedEvents @JsonCreator constructor(
  val crn: String,
  val name: Name,
  val activeEvents: List<ActiveEvent>
)

data class ActiveEvent(
  val eventNumber: String,
  val teamCode: String,
  val providerCode: String
)
