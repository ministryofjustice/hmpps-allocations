package uk.gov.justice.digital.hmpps.hmppsallocations.domain

import com.fasterxml.jackson.annotation.JsonCreator
import java.time.LocalDateTime

data class RiskPredictorOutputV1 @JsonCreator constructor(
  val groupReconvictionScore: GroupReconvictionScore?,
  val violencePredictorScore: ViolencePredictorScore?,
  val generalPredictorScore: GeneralPredictorScore?,
  val riskOfSeriousRecidivismScore: RiskOfSeriousRecidivismScore?,
  val sexualPredictorScore: SexualPredictorScore?,
)

data class GroupReconvictionScore @JsonCreator constructor(
  val oneYear: Int?,
  val twoYears: Int?,
  val scoreLevel: String?,
)

data class ViolencePredictorScore @JsonCreator constructor(
  val ovpStaticWeightedScore: Int?,
  val ovpDynamicWeightedScore: Int?,
  val ovpTotalWeightedScore: Int?,
  val oneYear: Int?,
  val twoYears: Int?,
  val ovpRisk: String?,
)

data class GeneralPredictorScore @JsonCreator constructor(
  val ogpStaticWeightedScore: Int?,
  val ogpDynamicWeightedScore: Int?,
  val ogpTotalWeightedScore: Int?,
  val ogp1Year: Int?,
  val ogp2Year: Int?,
  val ogpRisk: String?,
)

data class RiskOfSeriousRecidivismScore @JsonCreator constructor(
  val percentageScore: Int?,
  val staticOrDynamic: String?,
  val source: String?,
  val algorithmVersion: String?,
  val scoreLevel: String?,
)

//Double check types
data class SexualPredictorScore @JsonCreator constructor(
  val ospIndecentPercentageScore: Int?,
  val ospContactPercentageScore: Int?,
  val ospIndecentScoreLevel: String?,
  val ospContactScoreLevel: String?,
  val ospIndirectImagePercentageScore: Int?,
  val ospDirectContactPercentageScore: Int?,
  val ospIndirectImageScoreLevel: String?,
  val ospDirectContactScoreLevel: String?,
)


