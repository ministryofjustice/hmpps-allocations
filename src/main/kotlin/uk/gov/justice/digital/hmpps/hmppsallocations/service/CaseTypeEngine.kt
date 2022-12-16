package uk.gov.justice.digital.hmpps.hmppsallocations.service

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppsallocations.domain.CaseTypes
import uk.gov.justice.digital.hmpps.hmppsallocations.domain.CaseTypes.CUSTODY
import uk.gov.justice.digital.hmpps.hmppsallocations.domain.CaseTypes.LICENSE
import uk.gov.justice.digital.hmpps.hmppsallocations.domain.CaseTypes.UNKNOWN
import uk.gov.justice.digital.hmpps.hmppsallocations.domain.Conviction

@Service
class CaseTypeEngine(
  private val caseTypeRules: List<CaseTypeRule>
) {
  fun getCaseType(activeConvictions: List<Conviction>, convictionNumber: Int): CaseTypes {
    val allConvictionTypes = activeConvictions.mapNotNull { conviction ->
      convictionToCaseType(conviction)?.let { caseType ->
        {
          conviction.convictionNumber to caseType
        }
      }
    }.associate { it.invoke() }
    var caseType = allConvictionTypes.getValue(convictionNumber)
    if (caseType == LICENSE && allConvictionTypes.containsValue(CUSTODY)) {
      caseType = CUSTODY
    }
    return caseType
  }

  fun convictionToCaseType(conviction: Conviction): CaseTypes? {
    var caseType: CaseTypes = UNKNOWN
    for (caseTypeRule in caseTypeRules) {
      if (caseTypeRule.isCaseType(conviction.sentence?.sentenceType?.code, conviction.custody?.status?.code)) {
        caseType = caseTypeRule.getCaseType()
        break
      }
    }
    return caseType
  }
}
