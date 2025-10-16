package uk.gov.justice.digital.hmpps.hmppsallocations.client

import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withTimeout
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.awaitBody
import org.springframework.web.reactive.function.client.awaitExchangeOrNull
import reactor.core.publisher.Mono
import uk.gov.justice.digital.hmpps.hmppsallocations.client.dto.ProbationDeliveryUnitDetails
import uk.gov.justice.digital.hmpps.hmppsallocations.client.dto.ProbationEstateRegionAndTeamOverview
import uk.gov.justice.digital.hmpps.hmppsallocations.client.dto.RegionsAndTeamsRequest

private const val TIMEOUT_VALUE = 30000L

@Suppress("SwallowedException")
class HmppsProbationEstateApiClient(private val webClient: WebClient) {

  suspend fun getRegionsAndTeams(teamCodes: Set<String>): List<ProbationEstateRegionAndTeamOverview>? {
    try {
      return withTimeout(TIMEOUT_VALUE) {
        webClient
          .post()
          .uri("/regions")
          .contentType(MediaType.APPLICATION_JSON)
          .bodyValue(RegionsAndTeamsRequest.from(teamCodes))
          .awaitExchangeOrNull { response ->
            when (response.statusCode()) {
              HttpStatus.OK -> response.awaitBody<List<ProbationEstateRegionAndTeamOverview>>()
              HttpStatus.INTERNAL_SERVER_ERROR -> throw AllocationsFailedDependencyException("/regions failed for 500 error")
              else -> {
                log.error(
                  "Unexpected response from probation-estate's regions-and-teams API. Getting response-status: {}",
                  response.statusCode(),
                )
                null
              }
            }
          }
      }
    } catch (e: TimeoutCancellationException) {
      AssessRisksNeedsApiClient.Companion.log.warn("/regions failed for timeout", e)
      throw AllocationsWebClientTimeoutException(e.message!!)
    }
  }

  suspend fun getProbationDeliveryUnitByCode(pduCode: String): ProbationDeliveryUnitDetails? {
    try {
      return withTimeout(TIMEOUT_VALUE) {
        webClient
          .get()
          .uri("/probationDeliveryUnit/{pduCode}", pduCode)
          .retrieve()
          .onStatus({ it.is5xxServerError }) { res ->
            res.createException().flatMap {
              Mono.error(
                AllocationsFailedDependencyException("/probationDeliveryUnit/$pduCode failed with ${res.statusCode()}"),
              )
            }
          }
          .awaitBody()
      }
    } catch (e: TimeoutCancellationException) {
      log.warn("/probationDeliveryUnit/$pduCode failed for timeout", e)
      throw AllocationsWebClientTimeoutException(e.message!!)
    }
  }

  suspend fun getProbationEstate(): AllRegionDeliveryUnit = withTimeout(TIMEOUT_VALUE) {
    webClient
      .get()
      .uri("/all/regions")
      .retrieve()
      .onStatus({ it.is5xxServerError }) { res ->
        res.createException().flatMap {
          Mono.error(
            AllocationsFailedDependencyException("/all/regions failed with ${res.statusCode()}"),
          )
        }
      }
      .awaitBody()
  }

  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }

  data class ProbationEstate(
    val name: String,
    val regions: Map<String, AllRegionDeliveryUnit>,
  )

  data class AllRegionDeliveryUnit(
    val name: String,
    val pdus: Map<String, AllProbationDeliveryUnit>,
  )

  data class AllProbationDeliveryUnit(
    val name: String,
    val ldus: Map<String, AllLocalDeliveryUnit>,
  )

  data class AllLocalDeliveryUnit(
    val name: String,
    val teams: Map<String, AllTeam>,
  )

  data class AllTeam(
    val name: String,
    val code: String,
  )
}
