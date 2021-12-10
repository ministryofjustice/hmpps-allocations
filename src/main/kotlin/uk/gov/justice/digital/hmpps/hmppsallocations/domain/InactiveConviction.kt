package uk.gov.justice.digital.hmpps.hmppsallocations.domain

import com.fasterxml.jackson.annotation.JsonCreator

data class InactiveConviction @JsonCreator constructor(
  val sentence: Sentence,
  val active: Boolean,
)
