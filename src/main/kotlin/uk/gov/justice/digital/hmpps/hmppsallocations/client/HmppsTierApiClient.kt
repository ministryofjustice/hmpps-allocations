package uk.gov.justice.digital.hmpps.hmppsallocations.client

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient

@Component
class HmppsTierApiClient(@Qualifier("hmppsTierWebClientAppScope") private val webClient: WebClient) {

  fun getTierByCrn(crn: String): String = webClient
    .get()
    .uri("/crn/$crn/tier")
    .retrieve()
    .bodyToMono(TierDto::class.java)
    .block()!!.tierScore
    .also {
      log.info("Fetching Tier for $crn")
      log.debug("Body: $it for $crn")
    }

  companion object {
    private val log = LoggerFactory.getLogger(HmppsTierApiClient::class.java)
  }
}

private data class TierDto @JsonCreator constructor(
  @JsonProperty("tierScore")
  val tierScore: String
)
