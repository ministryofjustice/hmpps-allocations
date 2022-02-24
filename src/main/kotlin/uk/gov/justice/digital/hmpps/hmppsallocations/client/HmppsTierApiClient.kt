package uk.gov.justice.digital.hmpps.hmppsallocations.client

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.web.reactive.function.client.WebClient
import reactor.core.publisher.Mono
import java.util.Optional

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
    .map { Optional.of(it) }
    .onErrorResume { ex ->
      when (ex) {
        is MissingTierError -> Mono.just(Optional.empty())
        else -> Mono.error(ex)
      }
    }
    .block()?.map { it.tierScore }
    ?.orElseGet(null)
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
