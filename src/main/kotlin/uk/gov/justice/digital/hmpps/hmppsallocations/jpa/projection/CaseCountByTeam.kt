package uk.gov.justice.digital.hmpps.hmppsallocations.jpa.projection

interface CaseCountByTeam {
  fun getTeamCode(): String
  fun getCaseCount(): Int
}
