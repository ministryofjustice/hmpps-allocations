package uk.gov.justice.digital.hmpps.hmppsallocations.client.dto

data class RegionsAndTeamsRequest(
  val teamCodes: Set<String>,
) {
  companion object {
    fun from(teamCodes: Set<String>): RegionsAndTeamsRequest =
      RegionsAndTeamsRequest(
        teamCodes = teamCodes,
      )
  }
}
