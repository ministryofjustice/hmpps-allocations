package uk.gov.justice.digital.hmpps.hmppsallocations.client

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flow
import org.springframework.http.HttpStatus
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.bodyToFlow
import org.springframework.web.reactive.function.client.createExceptionAndAwait
import org.springframework.web.reactive.function.client.exchangeToFlow
import uk.gov.justice.digital.hmpps.hmppsallocations.domain.Assessment

class AssessmentApiClient(private val webClient: WebClient) {

  suspend fun getAssessment(crn: String): Flow<Assessment> {
    return webClient
      .get()
      .uri("/offenders/crn/$crn/assessments/summary?assessmentStatus=COMPLETE")
      .exchangeToFlow { response ->
        flow {
          when (response.statusCode()) {
            HttpStatus.OK -> emitAll(response.bodyToFlow())
            HttpStatus.NOT_FOUND -> emptyFlow<Assessment>()
            else -> throw response.createExceptionAndAwait()
          }
        }
      }
  }
}
