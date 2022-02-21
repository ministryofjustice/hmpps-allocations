package uk.gov.justice.digital.hmpps.hmppsallocations.service

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppsallocations.client.CommunityApiClient
import uk.gov.justice.digital.hmpps.hmppsallocations.domain.Conviction

@Service
class CaseTypeEngine(
  private val caseTypeRules: List<CaseTypeRule>,
  private val communityApiClient: CommunityApiClient
) {
  private val caseTypePrecedence = mapOf("COMMUNITY" to 3, "CUSTODY" to 2, "LICENSE" to 1)
  fun getCaseType(crn: String, convictionId: Long): String {
    return communityApiClient.getActiveConvictions(crn)
      .map { convictions ->
        val allConvictionTypes = convictions.mapNotNull { conviction ->
          convictionToCaseType(conviction)?.let { caseType ->
            {
              conviction.convictionId to caseType
            }
          }
        }.associate { it.invoke() }
        var caseType = allConvictionTypes.getValue(convictionId)
        if (caseType == "LICENSE" && allConvictionTypes.containsValue("CUSTODY")) {
          caseType = "CUSTODY"
        }
        caseType
      }.block()!!
  }

  fun convictionToCaseType(conviction: Conviction): String? {
    var caseType: String? = null
    for (caseTypeRule in caseTypeRules) {
      if (caseTypeRule.isCaseType(conviction.sentence!!.sentenceType.code, conviction.custody?.status?.code)) {
        caseType = caseTypeRule.getCaseType()
        break
      }
    }
    return caseType
  }
}
