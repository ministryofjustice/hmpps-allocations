package uk.gov.justice.digital.hmpps.hmppsallocations.client

import com.fasterxml.jackson.annotation.JsonCreator
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.reactive.asFlow
import org.springframework.core.io.Resource
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.WebClientResponseException
import org.springframework.web.reactive.function.client.awaitBody
import org.springframework.web.reactive.function.client.bodyToFlow
import reactor.core.publisher.Mono
import uk.gov.justice.digital.hmpps.hmppsallocations.client.dto.DeliusCaseView
import uk.gov.justice.digital.hmpps.hmppsallocations.client.dto.DeliusProbationRecord
import uk.gov.justice.digital.hmpps.hmppsallocations.client.dto.DeliusRisk
import uk.gov.justice.digital.hmpps.hmppsallocations.client.dto.UnallocatedEvents
import uk.gov.justice.digital.hmpps.hmppsallocations.jpa.entity.UnallocatedCaseEntity
import java.time.LocalDate
import java.time.ZonedDateTime

class WorkforceAllocationsToDeliusApiClient(private val webClient: WebClient) {

  suspend fun getDeliusCaseDetails(cases: List<UnallocatedCaseEntity>): Flow<DeliusCaseDetail> {
    val getCaseDetails = GetCaseDetails(cases.map { CaseIdentifier(it.crn, it.convictionNumber.toString()) })
    return webClient
      .post()
      .uri("/allocation-demand")
      .body(Mono.just(getCaseDetails), GetCaseDetails::class.java)
      .retrieve()
      .bodyToMono(DeliusCaseDetails::class.java)
      .flatMapIterable { it.cases }
      .asFlow()
  }

  suspend fun getDocuments(crn: String): Flow<Document> {
    return webClient
      .get()
      .uri("/offenders/$crn/documents")
      .retrieve()
      .bodyToFlow()
  }

  fun getDocumentById(crn: String, documentId: String): Mono<ResponseEntity<Resource>> {
    return webClient
      .get()
      .uri("/offenders/$crn/documents/$documentId")
      .retrieve()
      .toEntity(Resource::class.java)
  }

  suspend fun getDeliusCaseView(crn: String, convictionNumber: Long): DeliusCaseView {
    return webClient
      .get()
      .uri("/allocation-demand/$crn/$convictionNumber/case-view")
      .retrieve()
      .awaitBody()
  }

  suspend fun getProbationRecord(crn: String, excludeConvictionNumber: Long): DeliusProbationRecord {
    return webClient
      .get()
      .uri("/allocation-demand/$crn/$excludeConvictionNumber/probation-record")
      .retrieve()
      .awaitBody()
  }

  suspend fun getDeliusRisk(crn: String): DeliusRisk {
    return webClient
      .get()
      .uri("/allocation-demand/$crn/risk")
      .retrieve()
      .awaitBody()
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
