package uk.gov.justice.digital.hmpps.hmppsallocations.service

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppsallocations.client.CommunityApiClient
import uk.gov.justice.digital.hmpps.hmppsallocations.service.exception.CaseTypeNotDeterminedException
import java.util.Optional

@Service
class CaseTypeEngine(
  private val caseTypeRules: List<CaseTypeRule>,
  private val communityApiClient: CommunityApiClient
) {
  private val caseTypePrecedence = mapOf("COMMUNITY" to 3, "CUSTODY" to 2, "LICENSE" to 1)
  fun getCaseType(crn: String): String {
    val caseType = communityApiClient.getActiveConvictions(crn)
      .map { convictions ->
        Optional.ofNullable(
          convictions.map { conviction ->
            var caseType: String? = null
            for (caseTypeRule in caseTypeRules) {
              if (caseTypeRule.isCaseType(conviction.sentence!!.sentenceType.code, conviction.custody.status.code)) {
                caseType = caseTypeRule.getCaseType()
                break
              }
            }
            caseType
          }.filterNotNull()
            .reduce { first, second ->
              if (caseTypePrecedence[first]!! >= caseTypePrecedence[second]!!) {
                first
              } else {
                second
              }
            }
        )
      }.block()!!
    return caseType.orElseThrow { CaseTypeNotDeterminedException("Cannot determine case type for $crn") }
  }
}
