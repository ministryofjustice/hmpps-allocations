package uk.gov.justice.digital.hmpps.hmppsallocations.client

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.awaitBody
import org.springframework.web.reactive.function.client.awaitExchangeOrNull
import org.springframework.web.reactive.function.client.createExceptionAndAwait
import uk.gov.justice.digital.hmpps.hmppsallocations.client.dto.RegionsAndTeamsRequest

class HmppsProbationEstateApiClient(private val webClient: WebClient) {

  suspend fun getRegionsAndTeams(teamCodes: Set<String>): List<RegionAndTeamOverview>? = webClient
    .post()
    .uri("/regions")
    .contentType(MediaType.APPLICATION_JSON)
    .bodyValue(RegionsAndTeamsRequest.from(teamCodes))
    .awaitExchangeOrNull { response ->
      when (response.statusCode()) {
        HttpStatus.OK -> response.awaitBody<List<RegionAndTeamOverview>>()
        else -> throw response.createExceptionAndAwait()
      }
    }
}

data class RegionAndTeamOverview @JsonCreator constructor(
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
