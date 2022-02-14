package uk.gov.justice.digital.hmpps.hmppsallocations.domain

import com.fasterxml.jackson.annotation.JsonCreator
import java.math.BigInteger

data class OffenderAssessment @JsonCreator constructor(
  val ogrsScore: BigInteger?
)
