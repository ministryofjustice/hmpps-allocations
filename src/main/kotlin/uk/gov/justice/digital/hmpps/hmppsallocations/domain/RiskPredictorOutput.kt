package uk.gov.justice.digital.hmpps.hmppsallocations.domain

import java.math.BigDecimal

interface RiskPredictorOutput {
  fun getRSRScoreLevel(): String?

  fun getRSRPercentageScore(): BigDecimal?

  fun getRiskPredictorOutputV1(): RiskPredictorOutputV1?

  fun getRiskPredictorOutputV2(): RiskPredictorOutputV2?
}


