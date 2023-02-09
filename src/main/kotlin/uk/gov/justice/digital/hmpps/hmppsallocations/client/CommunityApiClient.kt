package uk.gov.justice.digital.hmpps.hmppsallocations.client

import com.fasterxml.jackson.annotation.JsonCreator
import org.springframework.http.HttpStatus
import org.springframework.web.reactive.function.client.WebClient
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import uk.gov.justice.digital.hmpps.hmppsallocations.domain.Conviction
import uk.gov.justice.digital.hmpps.hmppsallocations.domain.OffenderDetails
import uk.gov.justice.digital.hmpps.hmppsallocations.mapper.deliusToStaffGrade

class CommunityApiClient(private val webClient: WebClient) {

  fun getActiveConvictions(crn: String): Flux<Conviction> {
    return webClient
      .get()
      .uri("/offenders/crn/$crn/convictions?activeOnly=true")
      .retrieve()
      .onStatus(
        { httpStatus -> HttpStatus.NOT_FOUND == httpStatus },
        { Mono.error(MissingConvictionError("No Convictions found for $crn")) }
      )
      .bodyToFlux(Conviction::class.java)
      .onErrorResume { ex ->
        when (ex) {
          is MissingConvictionError -> Flux.empty()
          else -> Flux.error(ex)
        }
      }
  }

  fun getConviction(crn: String, convictionId: Long): Mono<Conviction?> {
    return webClient
      .get()
      .uri("/offenders/crn/$crn/convictions/$convictionId")
      .retrieve()
      .onStatus(
        { httpStatus -> HttpStatus.NOT_FOUND == httpStatus },
        { Mono.error(MissingConvictionError("No Conviction found for $crn")) }
      )
      .bodyToMono(Conviction::class.java)
      .onErrorResume { ex ->
        when (ex) {
          is MissingConvictionError -> Mono.empty()
          else -> Mono.error(ex)
        }
      }
  }

  fun getOffenderDetails(crn: String): Mono<OffenderDetails> {
    return webClient
      .get()
      .uri("/offenders/crn/$crn/all")
      .retrieve()
      .onStatus(
        { httpStatus -> HttpStatus.FORBIDDEN == httpStatus },
        { Mono.error(ForbiddenOffenderError("Unable to access offender details for $crn")) }
      )
      .bodyToMono(OffenderDetails::class.java)
  }

  data class Staff @JsonCreator constructor(
    val forenames: String,
    val surname: String
  )

  data class Grade @JsonCreator constructor(
    val code: String
  )

  data class OffenderManager @JsonCreator constructor(val staff: Staff, val grade: Grade?, val isUnallocated: Boolean) {
    val staffGrade: String? = deliusToStaffGrade(this.grade?.code)
  }
}

private class MissingConvictionError(msg: String) : RuntimeException(msg)
class ForbiddenOffenderError(msg: String) : RuntimeException(msg)
