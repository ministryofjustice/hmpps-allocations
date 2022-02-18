package uk.gov.justice.digital.hmpps.hmppsallocations.domain

import com.fasterxml.jackson.annotation.JsonCreator

data class PotentialCaseRequest @JsonCreator constructor(
  val tier: String,
  val type: String,
  val isT2A: Boolean = false
)
