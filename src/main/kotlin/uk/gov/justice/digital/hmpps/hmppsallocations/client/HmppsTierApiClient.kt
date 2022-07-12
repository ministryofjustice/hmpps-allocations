package uk.gov.justice.digital.hmpps.hmppsallocations.client

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.web.reactive.function.client.WebClient
import reactor.core.publisher.Mono

class HmppsTierApiClient(private val webClient: WebClient) {

  fun getTierByCrn(crn: String): String? = webClient
    .get()
    .uri("/crn/$crn/tier")
    .retrieve()
    .onStatus(
      { httpStatus -> HttpStatus.NOT_FOUND == httpStatus },
      { Mono.error(MissingTierError("No tier found for $crn")) }
    )
    .bodyToMono(TierDto::class.java)
    .map { it.tierScore }
    .onErrorResume { ex ->
      when (ex) {
        is MissingTierError -> Mono.empty()
        else -> Mono.error(ex)
      }
    }
    .block()
    .also {
      log.info("Fetching Tier for $crn")
    }

  companion object {
    private val log = LoggerFactory.getLogger(HmppsTierApiClient::class.java)
  }
}

private class MissingTierError(msg: String) : RuntimeException(msg)

private data class TierDto @JsonCreator constructor(
  @JsonProperty("tierScore")
  val tierScore: String
)
