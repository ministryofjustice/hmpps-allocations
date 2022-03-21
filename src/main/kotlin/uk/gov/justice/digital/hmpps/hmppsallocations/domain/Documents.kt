package uk.gov.justice.digital.hmpps.hmppsallocations.domain

data class Documents(
  val preSentenceReport: UnallocatedCaseDocument?,
  val cpsPack: UnallocatedCaseDocument?,
  val preConvictionReport: UnallocatedCaseDocument?
)
