package uk.gov.justice.digital.hmpps.hmppsallocations.client

import com.fasterxml.jackson.annotation.JsonCreator
import org.slf4j.LoggerFactory
import org.springframework.core.ParameterizedTypeReference
import org.springframework.http.HttpStatus
import org.springframework.web.reactive.function.client.WebClient
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import uk.gov.justice.digital.hmpps.hmppsallocations.domain.Contact
import uk.gov.justice.digital.hmpps.hmppsallocations.domain.Conviction
import uk.gov.justice.digital.hmpps.hmppsallocations.domain.OffenderAssessment
import uk.gov.justice.digital.hmpps.hmppsallocations.domain.OffenderDetails
import uk.gov.justice.digital.hmpps.hmppsallocations.domain.OffenderRegistrations
import uk.gov.justice.digital.hmpps.hmppsallocations.mapper.deliusToStaffGrade
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Optional

class CommunityApiClient(private val webClient: WebClient) {

  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }

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

  fun getAllConvictions(crn: String): Mono<List<Conviction>> {
    val responseType = object : ParameterizedTypeReference<List<Conviction>>() {}

    return webClient
      .get()
      .uri("/offenders/crn/$crn/convictions")
      .retrieve()
      .onStatus(
        { httpStatus -> HttpStatus.NOT_FOUND == httpStatus },
        { Mono.error(MissingConvictionError("No Conviction found for $crn")) }
      )
      .bodyToMono(responseType)
      .onErrorResume { ex ->
        when (ex) {
          is MissingConvictionError -> Mono.just(emptyList())
          else -> Mono.error(ex)
        }
      }
  }

  fun getInductionContacts(crn: String, contactDateFrom: LocalDate): Flux<Contact> {
    val contactDateFromQuery = contactDateFrom.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
    return webClient
      .get()
      .uri("/offenders/crn/$crn/contact-summary/inductions?contactDateFrom=$contactDateFromQuery")
      .retrieve()
      .bodyToFlux(Contact::class.java)
      .onErrorResume {
        log.warn("Error retrieving induction contacts", it)
        Flux.empty()
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

  fun getAllRegistrations(crn: String): Mono<OffenderRegistrations> {
    return webClient
      .get()
      .uri("/offenders/crn/$crn/registrations")
      .retrieve()
      .bodyToMono(OffenderRegistrations::class.java)
  }

  fun getAssessment(crn: String): Mono<Optional<OffenderAssessment>> {
    return webClient
      .get()
      .uri("/offenders/crn/$crn/assessments")
      .retrieve()
      .onStatus(
        { httpStatus -> HttpStatus.NOT_FOUND == httpStatus },
        { Mono.error(MissingOffenderAssessmentError("No offender assessment found for $crn")) }
      )
      .bodyToMono(OffenderAssessment::class.java)
      .map { Optional.of(it) }
      .onErrorResume { ex ->
        when (ex) {
          is MissingOffenderAssessmentError -> Mono.just(Optional.empty())
          else -> Mono.error(ex)
        }
      }
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
private class MissingOffenderAssessmentError(msg: String) : RuntimeException(msg)
class ForbiddenOffenderError(msg: String) : RuntimeException(msg)
