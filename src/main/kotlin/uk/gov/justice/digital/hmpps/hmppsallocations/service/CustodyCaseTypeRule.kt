package uk.gov.justice.digital.hmpps.hmppsallocations.service

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppsallocations.domain.CaseTypes

@Service
class CustodyCaseTypeRule : CaseTypeRule {

  private val custodialStatusCodes = setOf("A", "C", "D", "R", "I", "AT")
  private val custodialSentenceCodes = setOf("SC", "NC")

  override fun isCaseType(sentenceTypeCode: String?, custodialStatusCode: String?): Boolean = custodialSentenceCodes.contains(sentenceTypeCode) && custodialStatusCodes.contains(custodialStatusCode)

  override fun getCaseType(): CaseTypes = CaseTypes.CUSTODY
}
