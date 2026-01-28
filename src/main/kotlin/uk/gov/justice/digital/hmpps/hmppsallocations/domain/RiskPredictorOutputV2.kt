package uk.gov.justice.digital.hmpps.hmppsallocations.domain

import com.fasterxml.jackson.annotation.JsonCreator
import java.time.LocalDateTime

data class RiskPredictorOutputV2 @JsonCreator constructor(
  val allReoffendingPredictor: AllReoffendingPredictor?,
  val violentReoffendingPredictor: ViolentReoffendingPredictor?,
  val seriousViolentReoffendingPredictor: SeriousViolentReoffendingPredictor?,
  val directContactSexualReoffendingPredictor: DirectContactSexualReoffendingPredictor?,
  val indirectImageContactSexualReoffendingPredictor: IndirectImageContactSexualReoffendingPredictor?,
  val combinedSeriousReoffendingPredictor: CombinedSeriousReoffendingPredictor?,
)

data class AllReoffendingPredictor @JsonCreator constructor(
  val staticOrDynamic: String?,
  val score: Int?,
  val band: String?,
)

data class ViolentReoffendingPredictor @JsonCreator constructor(
  val staticOrDynamic: String?,
  val score: Int?,
  val band: String?,
)

data class SeriousViolentReoffendingPredictor @JsonCreator constructor(
  val staticOrDynamic: String?,
  val score: Int?,
  val band: String?,
)

data class DirectContactSexualReoffendingPredictor @JsonCreator constructor(
  val score: Int?,
  val band: String?,
)

data class IndirectImageContactSexualReoffendingPredictor @JsonCreator constructor(
  val score: Int?,
  val band: String?,
)

data class CombinedSeriousReoffendingPredictor @JsonCreator constructor(
  val algorithmVersion: String?,
  val staticOrDynamic: String?,
  val score: Int?,
  val band: String?,
)
