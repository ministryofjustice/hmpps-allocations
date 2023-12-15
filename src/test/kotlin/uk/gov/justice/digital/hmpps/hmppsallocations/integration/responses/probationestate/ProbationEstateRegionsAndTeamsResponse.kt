package uk.gov.justice.digital.hmpps.hmppsallocations.integration.responses.probationestate

fun regionsAndTeamsResponseBody(
  regions: List<Pair<String, String>>,
  teams: List<Pair<String, String>>,
): String {
  val regionAndTeamEntries = regions.mapIndexed { index, region ->
    """
    {
      "region": {
         "code": "${region.first}",
         "name": "${region.second}"
       },
       "team": {
         "code": "${teams[index].first}",
         "name": "${teams[index].second}"
       }
    }
    """.trimIndent()
  }
  return "${regionAndTeamEntries.map { it }}"
}
