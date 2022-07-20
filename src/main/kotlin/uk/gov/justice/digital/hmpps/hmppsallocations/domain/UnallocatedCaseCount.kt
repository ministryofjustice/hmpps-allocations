package uk.gov.justice.digital.hmpps.hmppsallocations.domain

import com.fasterxml.jackson.annotation.JsonCreator
import io.swagger.v3.oas.annotations.media.Schema

data class UnallocatedCaseCount @JsonCreator constructor(
  @Schema(description = "Count", example = "6")
  val count: Long
)
