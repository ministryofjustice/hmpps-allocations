package uk.gov.justice.digital.hmpps.hmppsallocations.client

import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.awaitBody
import org.springframework.web.reactive.function.client.awaitExchangeOrNull
import uk.gov.justice.digital.hmpps.hmppsallocations.client.dto.ProbationEstateRegionAndTeamOverview
import uk.gov.justice.digital.hmpps.hmppsallocations.client.dto.RegionsAndTeamsRequest

class HmppsProbationEstateApiClient(private val webClient: WebClient) {

  suspend fun getRegionsAndTeams(teamCodes: Set<String>): List<ProbationEstateRegionAndTeamOverview>? = webClient
    .post()
    .uri("/regions")
    .contentType(MediaType.APPLICATION_JSON)
    .bodyValue(RegionsAndTeamsRequest.from(teamCodes))
    .awaitExchangeOrNull { response ->
      when (response.statusCode()) {
        HttpStatus.OK -> response.awaitBody<List<ProbationEstateRegionAndTeamOverview>>()
        else -> {
          log.error("Unexpected response from probation-estate's regions-and-teams API. Getting response-status: {}", response.statusCode())
          null
        }
      }
    }

  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }
}

