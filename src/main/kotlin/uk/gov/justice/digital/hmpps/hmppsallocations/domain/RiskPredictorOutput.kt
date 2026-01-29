package uk.gov.justice.digital.hmpps.hmppsallocations.domain

import com.fasterxml.jackson.annotation.JsonCreator
import java.math.BigDecimal

open class RiskPredictorOutput @JsonCreator constructor(){
  open fun getRSRScoreLevel(): String?{
    return null
  }

  open fun getRSRPercentageScore(): BigDecimal? {
    return null
  }

  open fun getRiskPredictorOutputV1(): RiskPredictorOutputV1? {
    return null
  }

  open fun getRiskPredictorOutputV2(): RiskPredictorOutputV2? {
    return null
  }
}


