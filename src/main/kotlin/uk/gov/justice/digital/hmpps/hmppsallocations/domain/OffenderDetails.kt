package uk.gov.justice.digital.hmpps.hmppsallocations.domain

import com.fasterxml.jackson.annotation.JsonCreator

data class OffenderDetails @JsonCreator constructor(
  val firstName: String,
  val surname: String
)
