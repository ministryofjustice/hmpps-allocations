package uk.gov.justice.digital.hmpps.hmppsallocations.client

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.onEmpty
import kotlinx.coroutines.reactor.awaitSingleOrNull
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.bodyToFlow
import org.springframework.web.reactive.function.client.bodyToMono
import reactor.core.publisher.Mono
import reactor.util.retry.Retry
import uk.gov.justice.digital.hmpps.hmppsallocations.domain.Assessment
import uk.gov.justice.digital.hmpps.hmppsallocations.domain.RiskPredictor
import uk.gov.justice.digital.hmpps.hmppsallocations.domain.RoshSummary
import uk.gov.justice.digital.hmpps.hmppsallocations.domain.Timeline
import java.math.BigDecimal
import java.time.Duration

private const val RETRY_ATTEMPTS = 3L

private const val RETRY_DELAY = 1L

private const val NOT_FOUND = "NOT_FOUND"

private const val UNAVAILABLE = "UNAVAILABLE"

class AssessRisksNeedsApiClient(private val webClient: WebClient) {
  companion object {
    val log = LoggerFactory.getLogger(this::class.java)
  }

  suspend fun getLatestCompleteAssessment(crn: String): Assessment? {
    log.info("arnclient timeline $crn")
    return webClient
      .get()
      .uri("/assessments/timeline/crn/{crn}", crn)
      .retrieve()
      .onStatus({ it == HttpStatus.NOT_FOUND }) { res -> res.releaseBody().then(Mono.defer { Mono.empty() }) }
      .onStatus({ it != HttpStatus.OK }) { res -> res.createException().flatMap { Mono.error(it) } }
      .bodyToMono<Timeline>()
      .mapNotNull { timeline ->
        timeline.timeline
          .filter { it.status == "COMPLETE" }
          .maxByOrNull { it.completed!! }
      }
      .retryWhen(
        Retry.backoff(RETRY_ATTEMPTS, Duration.ofSeconds(RETRY_DELAY))
          .filter { it.message == UNAVAILABLE },
      )
      .awaitSingleOrNull()
  }

  suspend fun getRosh(crn: String): RoshSummary? {
    log.info("arn widget $crn")
    return webClient
      .get()
      .uri("/risks/crn/{crn}/widget", crn)
      .retrieve()
      .onStatus({ it == HttpStatus.NOT_FOUND }) { Mono.error(Exception(NOT_FOUND)) }
      .onStatus({ it != HttpStatus.OK }) { Mono.error(Exception(UNAVAILABLE)) }
      .bodyToMono<RoshSummary>()
      .retryWhen(
        Retry.backoff(RETRY_ATTEMPTS, Duration.ofSeconds(RETRY_DELAY))
          .filter { it.message == UNAVAILABLE },
      )
      .timeout(Duration.ofSeconds(20))
      .onErrorResume { throwable ->
        when (throwable.message) {
          NOT_FOUND -> Mono.just(RoshSummary(NOT_FOUND, null, emptyMap()))
          else -> Mono.just(RoshSummary(UNAVAILABLE, null, emptyMap()))
        }
      }
      .awaitSingleOrNull()
  }

  suspend fun getRiskPredictors(crn: String): Flow<RiskPredictor> {
    log.info("arn rsr $crn")
    return webClient
      .get()
      .uri("/risks/crn/{crn}/predictors/rsr/history", crn)
      .retrieve()
      .onStatus({ it == HttpStatus.NOT_FOUND }) { Mono.error(Exception(NOT_FOUND)) }
      .onStatus({ it.is5xxServerError }) { Mono.error(Exception("SERVER_ERROR")) }
      .onStatus({ it != HttpStatus.OK }) { Mono.error(Exception(UNAVAILABLE)) }
      .bodyToFlow<RiskPredictor>()
//      .retryWhen(
//        { cause, attempt ->
//          if (cause.message == "SERVER_ERROR" && attempt < RETRY_ATTEMPTS) {
//            delay(Duration.ofSeconds(RETRY_DELAY))
//            true
//          } else {
//            false
//          }
//        },
//      )
      .catch {
        when (it.message) {
          NOT_FOUND -> emit(RiskPredictor(BigDecimal(Int.MIN_VALUE), NOT_FOUND, null))
          else -> emit(RiskPredictor(BigDecimal(Int.MIN_VALUE), UNAVAILABLE, null))
        }
      }
      .onEmpty { emit(RiskPredictor(BigDecimal(Int.MIN_VALUE), NOT_FOUND, null)) }
  }
}
