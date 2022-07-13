package uk.gov.justice.digital.hmpps.hmppsallocations.client

import com.fasterxml.jackson.annotation.JsonCreator
import org.springframework.core.ParameterizedTypeReference
import org.springframework.core.io.Resource
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.reactive.function.client.WebClient
import reactor.core.publisher.Mono
import uk.gov.justice.digital.hmpps.hmppsallocations.client.domain.Document
import uk.gov.justice.digital.hmpps.hmppsallocations.client.domain.Documents
import uk.gov.justice.digital.hmpps.hmppsallocations.domain.Contact
import uk.gov.justice.digital.hmpps.hmppsallocations.domain.Conviction
import uk.gov.justice.digital.hmpps.hmppsallocations.domain.ConvictionRequirements
import uk.gov.justice.digital.hmpps.hmppsallocations.domain.InactiveConviction
import uk.gov.justice.digital.hmpps.hmppsallocations.domain.OffenderAssessment
import uk.gov.justice.digital.hmpps.hmppsallocations.domain.OffenderRegistrations
import uk.gov.justice.digital.hmpps.hmppsallocations.domain.OffenderSummary
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Optional

class CommunityApiClient(private val webClient: WebClient) {

  fun getActiveConvictions(crn: String): Mono<List<Conviction>> {
    val responseType = object : ParameterizedTypeReference<List<Conviction>>() {}

    return webClient
      .get()
      .uri("/offenders/crn/$crn/convictions?activeOnly=true")
      .retrieve()
      .bodyToMono(responseType)
  }

  fun getConviction(crn: String, convictionId: Long): Mono<Optional<Conviction>> {
    return webClient
      .get()
      .uri("/offenders/crn/$crn/convictions/$convictionId")
      .retrieve()
      .onStatus(
        { httpStatus -> HttpStatus.NOT_FOUND == httpStatus },
        { Mono.error(MissingConvictionError("No Conviction found for $crn")) }
      )
      .bodyToMono(Conviction::class.java)
      .map { Optional.of(it) }
      .onErrorResume { ex ->
        when (ex) {
          is MissingConvictionError -> Mono.just(Optional.empty())
          else -> Mono.error(ex)
        }
      }
  }
  fun getActiveRequirements(crn: String, convictionId: Long): Mono<ConvictionRequirements> {
    return webClient
      .get()
      .uri("/offenders/crn/$crn/convictions/$convictionId/requirements?activeOnly=true")
      .retrieve()
      .bodyToMono(ConvictionRequirements::class.java)
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

  fun getInactiveConvictions(crn: String): Mono<List<InactiveConviction>> {
    val responseType = object : ParameterizedTypeReference<List<InactiveConviction>>() {}
    return webClient
      .get()
      .uri("/offenders/crn/$crn/convictions")
      .retrieve()
      .bodyToMono(responseType)
      .map { convictions -> convictions.filter { c -> !c.active } }
  }

  fun getInductionContacts(crn: String, contactDateFrom: LocalDate): Mono<List<Contact>> {
    val responseType = object : ParameterizedTypeReference<List<Contact>>() {}
    val contactDateFromQuery = contactDateFrom.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
    return webClient
      .get()
      .uri("/offenders/crn/$crn/contact-summary/inductions?contactDateFrom=$contactDateFromQuery")
      .retrieve()
      .bodyToMono(responseType)
  }

  fun getOffenderSummary(crn: String): Mono<OffenderSummary> {
    return webClient
      .get()
      .uri("/offenders/crn/$crn")
      .retrieve()
      .bodyToMono(OffenderSummary::class.java)
  }

  fun getOffenderManagerName(crn: String): Mono<OffenderManager> {
    val responseType = object : ParameterizedTypeReference<List<OffenderManager>>() {}

    return webClient
      .get()
      .uri("/offenders/crn/$crn/allOffenderManagers")
      .retrieve()
      .bodyToMono(responseType)
      .map { it.first() }
  }

  fun getDocuments(crn: String, convictionId: Long): Mono<List<Document>> {
    return webClient
      .get()
      .uri("/offenders/crn/$crn/documents/grouped")
      .retrieve()
      .bodyToMono(Documents::class.java)
      .map {
        it.convictions.filter { documentConviction -> documentConviction.convictionId.toLong() == convictionId }
          .flatMap { documentConviction -> documentConviction.documents } + it.documents
      }
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

  fun getDocuments(crn: String, documentId: String): Mono<ResponseEntity<Resource>> {
    return webClient
      .get()
      .uri("/offenders/crn/$crn/documents/$documentId")
      .retrieve()
      .toEntity(Resource::class.java)
  }

  data class Staff @JsonCreator constructor(
    val forenames: String,
    val surname: String
  )

  data class Grade @JsonCreator constructor(
    val code: String
  )

  data class OffenderManager @JsonCreator constructor(val staff: Staff, val grade: Grade?)
}

private class MissingConvictionError(msg: String) : RuntimeException(msg)
private class MissingOffenderAssessmentError(msg: String) : RuntimeException(msg)
