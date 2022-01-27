package uk.gov.justice.digital.hmpps.hmppsallocations.client

import org.springframework.http.HttpStatus
import org.springframework.web.reactive.function.client.WebClient
import reactor.core.publisher.Mono
import uk.gov.justice.digital.hmpps.hmppsallocations.domain.Assessment

class AssessmentApiClient(private val webClient: WebClient) {

  fun getAssessment(crn: String): Mono<Assessment> {
    return webClient
      .get()
      .uri("/needs/crn/$crn")
      .retrieve()
      .onStatus(
        { httpStatus -> HttpStatus.NOT_FOUND == httpStatus },
        { Mono.error(MissingAssessmentError("No assessment found for $crn")) }
      )
      .bodyToMono(Assessment::class.java)
      .onErrorResume { ex ->
        when (ex) {
          is MissingAssessmentError -> Mono.just(Assessment(null))
          else -> Mono.error(ex)
        }
      }
  }
}

private class MissingAssessmentError(msg: String) : RuntimeException(msg)
