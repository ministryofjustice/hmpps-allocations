package uk.gov.justice.digital.hmpps.hmppsallocations.domain

import com.fasterxml.jackson.annotation.JsonCreator

data class Conviction @JsonCreator constructor(
  val sentence: Sentence
)
