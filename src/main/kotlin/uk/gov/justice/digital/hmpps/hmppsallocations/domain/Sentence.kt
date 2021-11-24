package uk.gov.justice.digital.hmpps.hmppsallocations.domain

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonFormat
import com.fasterxml.jackson.annotation.JsonFormat.Shape.STRING
import java.time.LocalDate

data class Sentence @JsonCreator constructor(

  @JsonFormat(pattern = "yyyy-MM-dd", shape = STRING)
  val startDate: LocalDate,
)