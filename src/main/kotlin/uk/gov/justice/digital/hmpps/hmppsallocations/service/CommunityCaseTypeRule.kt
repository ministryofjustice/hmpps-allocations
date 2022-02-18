package uk.gov.justice.digital.hmpps.hmppsallocations.service

import org.springframework.stereotype.Service

@Service
class CommunityCaseTypeRule : CaseTypeRule {

  private val communitySentenceCodes = setOf("SP", "NP")

  override fun isCaseType(sentenceTypeCode: String, custodialStatusCode: String?): Boolean = communitySentenceCodes.contains(sentenceTypeCode)

  override fun getCaseType(): String = "COMMUNITY"
}
