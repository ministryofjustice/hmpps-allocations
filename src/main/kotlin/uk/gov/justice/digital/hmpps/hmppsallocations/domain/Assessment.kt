package uk.gov.justice.digital.hmpps.hmppsallocations.domain

import com.fasterxml.jackson.annotation.JsonAlias
import com.fasterxml.jackson.annotation.JsonCreator
import java.time.LocalDateTime

data class Assessment @JsonCreator constructor(
  @JsonAlias("completedDate") val completed: LocalDateTime?,
  val assessmentType: String,
  val status: String,
)

data class Timeline(val timeline: List<Assessment>)
