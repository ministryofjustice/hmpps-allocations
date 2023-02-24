package uk.gov.justice.digital.hmpps.hmppsallocations.client

import kotlinx.coroutines.flow.Flow
import org.springframework.http.HttpStatus
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.bodyToFlow
import reactor.core.publisher.Mono
import uk.gov.justice.digital.hmpps.hmppsallocations.domain.Assessment

class AssessmentApiClient(private val webClient: WebClient) {

  suspend fun getAssessment(crn: String): Flow<Assessment> {
    return webClient
      .get()
      .uri("/offenders/crn/$crn/assessments/summary?assessmentStatus=COMPLETE")
      .retrieve()
      .onStatus(
        { httpStatus -> HttpStatus.NOT_FOUND == httpStatus },
        { Mono.empty() }
      )
      .bodyToFlow()
  }
}

private class MissingAssessmentError(msg: String) : RuntimeException(msg)
