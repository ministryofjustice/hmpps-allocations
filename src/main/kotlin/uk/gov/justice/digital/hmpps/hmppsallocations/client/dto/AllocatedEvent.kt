package uk.gov.justice.digital.hmpps.hmppsallocations.client.dto

import com.fasterxml.jackson.annotation.JsonCreator

class AllocatedEvent @JsonCreator constructor(
  val teamCode: String,
)
