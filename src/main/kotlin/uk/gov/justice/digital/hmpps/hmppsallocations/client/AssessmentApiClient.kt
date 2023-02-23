package uk.gov.justice.digital.hmpps.hmppsallocations.client

import org.springframework.core.ParameterizedTypeReference
import org.springframework.http.HttpStatus
import org.springframework.web.reactive.function.client.WebClient
import reactor.core.publisher.Mono
import uk.gov.justice.digital.hmpps.hmppsallocations.domain.Assessment

class AssessmentApiClient(private val webClient: WebClient) {

  fun getAssessment(crn: String): Mono<List<Assessment>> {
    val responseType = object : ParameterizedTypeReference<List<Assessment>>() {}
    return webClient
      .get()
      .uri("/offenders/crn/$crn/assessments/summary?assessmentStatus=COMPLETE")
      .retrieve()
      .onStatus(
        { httpStatus -> HttpStatus.NOT_FOUND == httpStatus },
        { Mono.error(MissingAssessmentError("No assessment found for $crn")) }
      )
      .bodyToMono(responseType)
      .onErrorResume { ex ->
        when (ex) {
          is MissingAssessmentError -> Mono.just(emptyList())
          else -> Mono.error(ex)
        }
      }
  }
}

private class MissingAssessmentError(msg: String) : RuntimeException(msg)
