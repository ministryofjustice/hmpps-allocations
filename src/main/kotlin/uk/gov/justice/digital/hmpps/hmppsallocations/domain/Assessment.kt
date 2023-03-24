package uk.gov.justice.digital.hmpps.hmppsallocations.domain

import com.fasterxml.jackson.annotation.JsonCreator
import java.time.LocalDateTime

data class Assessment @JsonCreator constructor(
  val completed: LocalDateTime,
  val assessmentType: String,
)
