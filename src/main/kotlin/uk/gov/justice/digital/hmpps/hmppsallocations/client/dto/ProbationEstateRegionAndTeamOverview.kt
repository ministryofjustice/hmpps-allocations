package uk.gov.justice.digital.hmpps.hmppsallocations.client.dto

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty

data class ProbationEstateRegionAndTeamOverview @JsonCreator constructor(
  @JsonProperty("region")
  val region: RegionOverview,
  @JsonProperty("team")
  val team: TeamOverview,
)

data class RegionOverview @JsonCreator constructor(
  val code: String,
  val name: String,
)

data class TeamOverview @JsonCreator constructor(
  val code: String,
  val name: String,
)