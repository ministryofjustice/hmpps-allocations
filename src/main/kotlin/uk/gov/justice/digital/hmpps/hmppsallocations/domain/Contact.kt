package uk.gov.justice.digital.hmpps.hmppsallocations.domain

import com.fasterxml.jackson.annotation.JsonCreator
import java.time.ZonedDateTime

data class Contact @JsonCreator constructor(
  val contactStart: ZonedDateTime
)
