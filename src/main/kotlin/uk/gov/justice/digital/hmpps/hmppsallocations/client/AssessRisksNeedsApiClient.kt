package uk.gov.justice.digital.hmpps.hmppsallocations.client

import org.springframework.core.ParameterizedTypeReference
import org.springframework.http.HttpStatus
import org.springframework.web.reactive.function.client.WebClient
import reactor.core.publisher.Mono
import uk.gov.justice.digital.hmpps.hmppsallocations.domain.RiskPredictor
import uk.gov.justice.digital.hmpps.hmppsallocations.domain.RoshSummary
import java.util.Optional

class AssessRisksNeedsApiClient(private val webClient: WebClient) {

  fun getRosh(crn: String): Mono<Optional<RoshSummary>> {
    return webClient
      .get()
      .uri("/risks/crn/$crn/widget")
      .retrieve()
      .onStatus(
        { httpStatus -> HttpStatus.NOT_FOUND == httpStatus },
        { Mono.error(MissingRiskError("No risk summary found for $crn")) }
      )
      .bodyToMono(RoshSummary::class.java)
      .map { Optional.of(it) }
      .onErrorResume { ex ->
        when (ex) {
          is MissingRiskError -> Mono.just(Optional.empty())
          else -> Mono.error(ex)
        }
      }
  }

  fun getRiskPredictors(crn: String): Mono<List<RiskPredictor>> {
    val responseType = object : ParameterizedTypeReference<List<RiskPredictor>>() {}
    return webClient
      .get()
      .uri("/risks/crn/$crn/predictors/rsr/history")
      .retrieve()
      .onStatus(
        { httpStatus -> HttpStatus.NOT_FOUND == httpStatus },
        { Mono.error(MissingRiskError("No risk predictors found for $crn")) }
      )
      .bodyToMono(responseType)
      .onErrorResume { ex ->
        when (ex) {
          is MissingRiskError -> Mono.just(emptyList())
          else -> Mono.error(ex)
        }
      }
  }
}

private class MissingRiskError(msg: String) : RuntimeException(msg)
