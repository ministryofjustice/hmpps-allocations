package uk.gov.justice.digital.hmpps.hmppsallocations.service

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppsallocations.domain.CaseTypes
import uk.gov.justice.digital.hmpps.hmppsallocations.domain.Conviction

@Service
class CaseTypeEngine(
  private val caseTypeRules: List<CaseTypeRule>
) {
  fun getCaseType(activeConvictions: List<Conviction>, convictionId: Long): CaseTypes {
    val allConvictionTypes = activeConvictions.mapNotNull { conviction ->
      convictionToCaseType(conviction)?.let { caseType ->
        {
          conviction.convictionId to caseType
        }
      }
    }.associate { it.invoke() }
    var caseType = allConvictionTypes.getValue(convictionId)
    if (caseType == CaseTypes.LICENSE && allConvictionTypes.containsValue(CaseTypes.CUSTODY)) {
      caseType = CaseTypes.CUSTODY
    }
    return caseType
  }

  fun convictionToCaseType(conviction: Conviction): CaseTypes? {
    var caseType: CaseTypes = CaseTypes.UNKNOWN
    for (caseTypeRule in caseTypeRules) {
      if (caseTypeRule.isCaseType(conviction.sentence?.sentenceType?.code, conviction.custody?.status?.code)) {
        caseType = caseTypeRule.getCaseType()
        break
      }
    }
    return caseType
  }
}
