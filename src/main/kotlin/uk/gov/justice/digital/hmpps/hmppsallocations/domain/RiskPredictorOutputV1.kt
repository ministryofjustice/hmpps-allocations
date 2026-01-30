package uk.gov.justice.digital.hmpps.hmppsallocations.domain

import com.fasterxml.jackson.annotation.JsonCreator
import java.math.BigDecimal

data class RiskPredictorOutputV1 @JsonCreator constructor(
  val groupReconvictionScore: GroupReconvictionScore?,
  val violencePredictorScore: ViolencePredictorScore?,
  val generalPredictorScore: GeneralPredictorScore?,
  val riskOfSeriousRecidivismScore: RiskOfSeriousRecidivismScore?,
  val sexualPredictorScore: SexualPredictorScore?,
) : RiskPredictorOutput{
  override fun getRSRScoreLevel(): String? {
    return riskOfSeriousRecidivismScore?.scoreLevel
  }
  override fun getRSRPercentageScore(): BigDecimal? {
    return riskOfSeriousRecidivismScore?.percentageScore
  }
  override fun getRiskPredictorOutputV1(): RiskPredictorOutputV1 {
    return this
  }
  override fun getRiskPredictorOutputV2(): RiskPredictorOutputV2? {
    return null
  }
}

data class GroupReconvictionScore @JsonCreator constructor(
  val oneYear: BigDecimal?,
  val twoYears: BigDecimal?,
  val scoreLevel: String?,
)

data class ViolencePredictorScore @JsonCreator constructor(
  val ovpStaticWeightedScore: BigDecimal?,
  val ovpDynamicWeightedScore: BigDecimal?,
  val ovpTotalWeightedScore: BigDecimal?,
  val oneYear: BigDecimal?,
  val twoYears: BigDecimal?,
  val ovpRisk: String?,
)

data class GeneralPredictorScore @JsonCreator constructor(
  val ogpStaticWeightedScore: BigDecimal?,
  val ogpDynamicWeightedScore: BigDecimal?,
  val ogpTotalWeightedScore: BigDecimal?,
  val ogp1Year: BigDecimal?,
  val ogp2Year: BigDecimal?,
  val ogpRisk: String?,
)

data class RiskOfSeriousRecidivismScore @JsonCreator constructor(
  val percentageScore: BigDecimal?,
  val staticOrDynamic: String?,
  val source: String?,
  val algorithmVersion: String?,
  val scoreLevel: String?,
)

data class SexualPredictorScore @JsonCreator constructor(
  val ospIndecentPercentageScore: BigDecimal?,
  val ospContactPercentageScore: BigDecimal?,
  val ospIndecentScoreLevel: String?,
  val ospContactScoreLevel: String?,
  val ospIndirectImagePercentageScore: BigDecimal?,
  val ospDirectContactPercentageScore: BigDecimal?,
  val ospIndirectImageScoreLevel: String?,
  val ospDirectContactScoreLevel: String?,
)


