package uk.gov.justice.digital.hmpps.hmppsallocations.client

import com.fasterxml.jackson.annotation.JsonCreator
import org.springframework.core.io.Resource
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.WebClientResponseException
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import uk.gov.justice.digital.hmpps.hmppsallocations.client.dto.DeliusCaseView
import uk.gov.justice.digital.hmpps.hmppsallocations.client.dto.DeliusProbationRecord
import uk.gov.justice.digital.hmpps.hmppsallocations.client.dto.DeliusRisk
import uk.gov.justice.digital.hmpps.hmppsallocations.client.dto.UnallocatedEvents
import uk.gov.justice.digital.hmpps.hmppsallocations.jpa.entity.UnallocatedCaseEntity
import java.time.LocalDate
import java.time.ZonedDateTime

class WorkforceAllocationsToDeliusApiClient(private val webClient: WebClient) {

  fun getDeliusCaseDetails(cases: List<UnallocatedCaseEntity>): Flux<DeliusCaseDetail> {
    val getCaseDetails = GetCaseDetails(cases.map { CaseIdentifier(it.crn, it.convictionNumber.toString()) })
    return webClient
      .post()
      .uri("/allocation-demand")
      .body(Mono.just(getCaseDetails), GetCaseDetails::class.java)
      .retrieve()
      .bodyToMono(DeliusCaseDetails::class.java)
      .flatMapIterable { it.cases }
  }

  fun getDocuments(crn: String): Flux<Document> {
    return webClient
      .get()
      .uri("/offenders/$crn/documents")
      .retrieve()
      .bodyToFlux(Document::class.java)
  }

  fun getDocumentById(crn: String, documentId: String): Mono<ResponseEntity<Resource>> {
    return webClient
      .get()
      .uri("/offenders/$crn/documents/$documentId")
      .retrieve()
      .toEntity(Resource::class.java)
  }

  fun getDeliusCaseView(crn: String, convictionNumber: Long): Mono<DeliusCaseView> {
    return webClient
      .get()
      .uri("/allocation-demand/$crn/$convictionNumber/case-view")
      .retrieve()
      .bodyToMono(DeliusCaseView::class.java)
  }

  fun getProbationRecord(crn: String, excludeConvictionNumber: Long): Mono<DeliusProbationRecord> {
    return webClient
      .get()
      .uri("/allocation-demand/$crn/$excludeConvictionNumber/probation-record")
      .retrieve()
      .bodyToMono(DeliusProbationRecord::class.java)
  }

  fun getDeliusRisk(crn: String): Mono<DeliusRisk> {
    return webClient
      .get()
      .uri("/allocation-demand/$crn/risk")
      .retrieve()
      .bodyToMono(DeliusRisk::class.java)
  }

  fun getUnallocatedEvents(crn: String): Mono<UnallocatedEvents?> =
    webClient
      .get()
      .uri("/allocation-demand/$crn/unallocated-events")
      .retrieve()
      .bodyToMono(UnallocatedEvents::class.java)
      .onErrorResume(WebClientResponseException::class.java) { ex ->
        when (ex.rawStatusCode) {
          HttpStatus.NOT_FOUND.value() -> Mono.empty()
          HttpStatus.FORBIDDEN.value() -> Mono.error(ForbiddenOffenderError("Unable to access offender details for $crn"))
          else -> Mono.error(ex)
        }
      }
}

class ForbiddenOffenderError(msg: String) : RuntimeException(msg)
data class CaseIdentifier(val crn: String, val eventNumber: String)
data class GetCaseDetails(val cases: List<CaseIdentifier>)

data class DeliusCaseDetail(
  val crn: String,
  val name: Name,
  val sentence: Sentence,
  val event: Event,
  val initialAppointment: InitialAppointment?,
  val probationStatus: ProbationStatus,
  val communityPersonManager: CommunityPersonManager?,
  val type: String
)

data class Event(val number: String)

data class ProbationStatus(val description: String)
data class InitialAppointment(val date: LocalDate?)
data class DeliusCaseDetails(val cases: List<DeliusCaseDetail>)
data class Name(val forename: String, val middleName: String?, val surname: String) {
  fun getCombinedName() = "$forename ${middleName?.takeUnless { it.isBlank() }?.let{ "$middleName " } ?: ""}$surname"
}
data class CommunityPersonManager(val name: Name, val grade: String?)

data class Sentence(val date: LocalDate, val length: String)

data class Document @JsonCreator constructor(
  val id: String?,
  val name: String,
  val dateCreated: ZonedDateTime?,
  val sensitive: Boolean,
  val relatedTo: DocumentRelatedTo
)

data class DocumentRelatedTo @JsonCreator constructor(
  val type: String,
  val name: String,
  val description: String,
  val event: DocumentEvent?
)

data class DocumentEvent @JsonCreator constructor(
  val eventType: String,
  val eventNumber: String,
  val mainOffence: String,
)
