package uk.gov.justice.digital.hmpps.hmppsallocations.domain

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonFormat
import com.fasterxml.jackson.annotation.JsonFormat.Shape.STRING
import java.time.LocalDateTime

data class Sentence @JsonCreator constructor(

  @JsonFormat(pattern = "yyyyMMdd HH:mm:ss", shape = STRING)
  val startDate: LocalDateTime,
)
