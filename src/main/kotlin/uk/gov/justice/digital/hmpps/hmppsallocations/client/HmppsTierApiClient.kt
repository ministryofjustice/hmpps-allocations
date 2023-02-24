package uk.gov.justice.digital.hmpps.hmppsallocations.client

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty
import org.springframework.http.HttpStatus
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.awaitBody
import org.springframework.web.reactive.function.client.awaitExchangeOrNull
import org.springframework.web.reactive.function.client.createExceptionAndAwait

class HmppsTierApiClient(private val webClient: WebClient) {

  suspend fun getTierByCrn(crn: String): String? = webClient
    .get()
    .uri("/crn/$crn/tier")
    .awaitExchangeOrNull { response ->
      when (response.statusCode()) {
        HttpStatus.OK -> response.awaitBody<TierDto>()
        HttpStatus.NOT_FOUND -> null
        else -> throw response.createExceptionAndAwait()
      }
    }?.tierScore
}
private data class TierDto @JsonCreator constructor(
  @JsonProperty("tierScore")
  val tierScore: String
)
