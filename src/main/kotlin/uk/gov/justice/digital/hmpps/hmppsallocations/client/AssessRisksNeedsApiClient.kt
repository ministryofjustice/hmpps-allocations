package uk.gov.justice.digital.hmpps.hmppsallocations.client

import org.springframework.http.HttpStatus
import org.springframework.web.reactive.function.client.WebClient
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import uk.gov.justice.digital.hmpps.hmppsallocations.domain.RiskPredictor
import uk.gov.justice.digital.hmpps.hmppsallocations.domain.RoshSummary

class AssessRisksNeedsApiClient(private val webClient: WebClient) {

  fun getRosh(crn: String): Mono<RoshSummary> {
    return webClient
      .get()
      .uri("/risks/crn/$crn/widget")
      .retrieve()
      .onStatus(
        { httpStatus -> HttpStatus.NOT_FOUND == httpStatus },
        { Mono.error(MissingRiskError("No risk summary found for $crn")) }
      )
      .bodyToMono(RoshSummary::class.java)
      .onErrorResume { ex ->
        when (ex) {
          is MissingRiskError -> Mono.empty()
          else -> Mono.error(ex)
        }
      }
  }

  fun getRiskPredictors(crn: String): Flux<RiskPredictor> {
    return webClient
      .get()
      .uri("/risks/crn/$crn/predictors/rsr/history")
      .retrieve()
      .onStatus(
        { httpStatus -> HttpStatus.NOT_FOUND == httpStatus },
        { Mono.error(MissingRiskError("No risk predictors found for $crn")) }
      )
      .bodyToFlux(RiskPredictor::class.java)
      .onErrorResume { ex ->
        when (ex) {
          is MissingRiskError -> Flux.empty()
          else -> Flux.error(ex)
        }
      }
  }
}

private class MissingRiskError(msg: String) : RuntimeException(msg)
