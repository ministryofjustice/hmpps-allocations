package uk.gov.justice.digital.hmpps.hmppsallocations.domain

data class UnallocatedCaseTeamCount constructor(
  val teamCode: String,
  val caseCount: Int
)
