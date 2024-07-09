package uk.gov.justice.digital.hmpps.hmppsallocations.client

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.onEmpty
import kotlinx.coroutines.reactor.awaitSingleOrNull
import org.springframework.http.HttpStatus
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.awaitBody
import org.springframework.web.reactive.function.client.awaitExchangeOrNull
import org.springframework.web.reactive.function.client.bodyToFlow
import org.springframework.web.reactive.function.client.bodyToMono
import org.springframework.web.reactive.function.client.exchangeToFlow
import reactor.core.publisher.Mono
import uk.gov.justice.digital.hmpps.hmppsallocations.domain.Assessment
import uk.gov.justice.digital.hmpps.hmppsallocations.domain.RiskPredictor
import uk.gov.justice.digital.hmpps.hmppsallocations.domain.RoshSummary
import uk.gov.justice.digital.hmpps.hmppsallocations.domain.Timeline
import java.math.BigDecimal

class AssessRisksNeedsApiClient(private val webClient: WebClient) {

  suspend fun getLatestCompleteAssessment(crn: String): Assessment? {
    return webClient
      .get()
      .uri("/assessments/timeline/crn/$crn")
      .exchangeToMono { res ->
        when (res.statusCode()) {
          HttpStatus.NOT_FOUND -> res.releaseBody().then(Mono.defer { Mono.empty() })
          HttpStatus.OK -> res.bodyToMono<Timeline>()
            .mapNotNull { a -> a.timeline.filter { it.status == "COMPLETE" }.maxByOrNull { it.completed!! } }
          else -> res.createException().flatMap { Mono.error(it.rootCause!!) }
        }
      }.awaitSingleOrNull()
  }

  suspend fun getRosh(crn: String): RoshSummary? {
    return webClient
      .get()
      .uri("/risks/crn/$crn/widget")
      .awaitExchangeOrNull { response ->
        when (response.statusCode()) {
          HttpStatus.OK -> response.awaitBody()
          HttpStatus.NOT_FOUND -> RoshSummary("NOT_FOUND", null, emptyMap())
          else -> RoshSummary("UNAVAILABLE", null, emptyMap())
        }
      }
  }

  suspend fun getRiskPredictors(crn: String): Flow<RiskPredictor> {
    return webClient
      .get()
      .uri("/risks/crn/$crn/predictors/rsr/history")
      .exchangeToFlow { response ->
        flow {
          when (response.statusCode()) {
            HttpStatus.OK -> emitAll(response.bodyToFlow())
            HttpStatus.NOT_FOUND -> notFound()
            else -> emit(RiskPredictor(BigDecimal(Int.MIN_VALUE), "UNAVAILABLE", null))
          }
        }.onEmpty { notFound() }
      }
  }

  private suspend fun FlowCollector<RiskPredictor>.notFound() {
    emit(RiskPredictor(BigDecimal(Int.MIN_VALUE), "NOT_FOUND", null))
  }
}
