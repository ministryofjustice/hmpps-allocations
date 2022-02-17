package uk.gov.justice.digital.hmpps.hmppsallocations.service

interface CaseTypeRule {
  fun isCaseType(sentenceTypeCode: String, custodialStatusCode: String): Boolean
  fun getCaseType(): String
}
