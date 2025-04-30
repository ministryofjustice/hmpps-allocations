package uk.gov.justice.digital.hmpps.hmppsallocations.client

import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus
import org.springframework.web.reactive.function.client.ClientResponse
import org.springframework.web.reactive.function.client.ExchangeFunction
import org.springframework.web.reactive.function.client.WebClient
import reactor.core.publisher.Mono

class HmppsProbationEstateApiClientTest {
  private val regionResponse = """
    [
      {
        "region": {
          "code": "N01",
          "name": "North East"
        },
        "team": {
          "code": "N01T01",
          "name": "Newcastle Team 1"
        }
      },
      {
        "region": {
          "code": "N01",
          "name": "North East"
        },
        "team": {
          "code": "N01T02",
          "name": "Newcastle Team 2"
        }
      }
    ]
  """.trimIndent()

  @Test
  fun getRegionsAndTeams() = runBlocking {
    val exchangeFunction = ExchangeFunction { request ->
      Mono.just(
        ClientResponse.create(HttpStatus.OK)
          .header("Content-Type", "application/json")
          .body(regionResponse)
          .build(),
      )
    }
    val webClient = WebClient.builder().exchangeFunction(exchangeFunction).build()
    val result = HmppsProbationEstateApiClient(webClient).getRegionsAndTeams(setOf("N01T01", "N01T02"))
    assert(result?.size == 2)
    assert(result?.get(0)?.team?.code == "N01T01")
    assert(result?.get(0)?.team?.name == "Newcastle Team 1")
    assert(result?.get(0)?.region?.code == "N01")
    assert(result?.get(0)?.region?.name == "North East")
    assert(result?.get(1)?.team?.code == "N01T02")
    assert(result?.get(1)?.team?.name == "Newcastle Team 2")
    assert(result?.get(1)?.region?.code == "N01")
    assert(result?.get(1)?.region?.name == "North East")
  }
}
