package uk.gov.justice.digital.hmpps.hmppsallocations.service

import uk.gov.justice.digital.hmpps.hmppsallocations.domain.CaseTypes

interface CaseTypeRule {
  fun isCaseType(sentenceTypeCode: String, custodialStatusCode: String?): Boolean
  fun getCaseType(): CaseTypes
}
