package uk.gov.justice.digital.hmpps.hmppsallocations.domain

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonFormat
import com.fasterxml.jackson.annotation.JsonFormat.Shape.STRING
import java.time.LocalDate

data class Conviction @JsonCreator constructor(
  @JsonFormat(pattern = "yyyy-MM-dd", shape = STRING)
  val convictionDate: LocalDate?,
  val sentence: Sentence?,
  val active: Boolean,
  val offences: List<Offence>,
  val convictionId: Long,
)
