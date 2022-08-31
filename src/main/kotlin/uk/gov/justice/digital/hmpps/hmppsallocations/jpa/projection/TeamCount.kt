package uk.gov.justice.digital.hmpps.hmppsallocations.jpa.projection

interface TeamCount {
  fun getTeamCode(): String
  fun getCaseCount(): Int
}
