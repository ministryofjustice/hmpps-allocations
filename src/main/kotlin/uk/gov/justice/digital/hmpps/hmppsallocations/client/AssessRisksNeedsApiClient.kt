package uk.gov.justice.digital.hmpps.hmppsallocations.client

import org.springframework.http.HttpStatus
import org.springframework.web.reactive.function.client.WebClient
import reactor.core.publisher.Mono
import uk.gov.justice.digital.hmpps.hmppsallocations.domain.RiskSummary

class AssessRisksNeedsApiClient(private val webClient: WebClient) {

  fun getRiskSummary(crn: String): Mono<RiskSummary> {
    return webClient
      .get()
      .uri("/risks/crn/$crn/summary")
      .retrieve()
      .onStatus(
        { httpStatus -> HttpStatus.NOT_FOUND == httpStatus },
        { Mono.error(MissingRiskError("No risk summary found for $crn")) }
      )
      .bodyToMono(RiskSummary::class.java)
      .onErrorResume { ex ->
        when (ex) {
          is MissingRiskError -> Mono.empty()
          else -> Mono.error(ex)
        }
      }
  }
}

private class MissingRiskError(msg: String) : RuntimeException(msg)
