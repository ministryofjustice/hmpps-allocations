package uk.gov.justice.digital.hmpps.hmppsallocations.service

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppsallocations.domain.CaseTypes

@Service
class CommunityCaseTypeRule : CaseTypeRule {

  private val communitySentenceCodes = setOf("SP", "NP")

  override fun isCaseType(sentenceTypeCode: String, custodialStatusCode: String?): Boolean = communitySentenceCodes.contains(sentenceTypeCode)

  override fun getCaseType(): CaseTypes = CaseTypes.COMMUNITY
}
