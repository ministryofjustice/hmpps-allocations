package uk.gov.justice.digital.hmpps.hmppsallocations.client

import org.springframework.web.reactive.function.client.WebClient
import reactor.core.publisher.Mono
import uk.gov.justice.digital.hmpps.hmppsallocations.domain.Assessment

class AssessmentApiClient(private val webClient: WebClient) {

  fun getAssessment(crn: String): Mono<Assessment> {
    return webClient
      .get()
      .uri("/needs/crn/$crn")
      .retrieve()
      .bodyToMono(Assessment::class.java)
  }
}
