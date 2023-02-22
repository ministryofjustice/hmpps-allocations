package uk.gov.justice.digital.hmpps.hmppsallocations.client

import kotlinx.coroutines.flow.Flow
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.awaitBodyOrNull
import org.springframework.web.reactive.function.client.bodyToFlow
import reactor.core.publisher.Mono
import uk.gov.justice.digital.hmpps.hmppsallocations.domain.RiskPredictor
import uk.gov.justice.digital.hmpps.hmppsallocations.domain.RoshSummary

class AssessRisksNeedsApiClient(private val webClient: WebClient) {

  suspend fun getRosh(crn: String, authToken: String): RoshSummary? {
    return webClient
      .get()
      .uri("/risks/crn/$crn/widget")
      .header(HttpHeaders.AUTHORIZATION, authToken)
      .retrieve()
      .onStatus(
        { httpStatus -> HttpStatus.NOT_FOUND == httpStatus },
        { Mono.empty() }
      ).awaitBodyOrNull()
  }

  fun getRiskPredictors(crn: String, authToken: String): Flow<RiskPredictor> {
    return webClient
      .get()
      .uri("/risks/crn/$crn/predictors/rsr/history")
      .header(HttpHeaders.AUTHORIZATION, authToken)
      .retrieve()
      .onStatus(
        { httpStatus -> HttpStatus.NOT_FOUND == httpStatus },
        { Mono.empty() }
      )
      .bodyToFlow()
  }
}
