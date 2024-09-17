package uk.gov.justice.digital.hmpps.hmppsallocations.client

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty
import org.slf4j.LoggerFactory
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
        HttpStatus.NOT_FOUND ->
          {
            log.debug("Tier not found for CRN $crn")
            throw MissingTierException("Tier not found for CRN $crn")
          }
        else -> throw response.createExceptionAndAwait()
      }
    }?.tierScore

  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }
}
class MissingTierException(msg: String) : RuntimeException()

private data class TierDto @JsonCreator constructor(
  @JsonProperty("tierScore")
  val tierScore: String,
)
