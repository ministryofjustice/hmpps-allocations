package uk.gov.justice.digital.hmpps.hmppsallocations.domain

data class CaseCountByTeam constructor(
  val teamCode: String,
  val caseCount: Int,
)
