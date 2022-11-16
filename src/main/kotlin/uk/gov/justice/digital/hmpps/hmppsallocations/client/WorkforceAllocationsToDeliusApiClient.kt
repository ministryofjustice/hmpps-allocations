package uk.gov.justice.digital.hmpps.hmppsallocations.client

import com.fasterxml.jackson.annotation.JsonCreator
import org.slf4j.LoggerFactory
import org.springframework.core.io.Resource
import org.springframework.http.ResponseEntity
import org.springframework.web.reactive.function.client.WebClient
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import uk.gov.justice.digital.hmpps.hmppsallocations.jpa.entity.UnallocatedCaseEntity
import java.time.LocalDate
import java.time.ZonedDateTime

class WorkforceAllocationsToDeliusApiClient(private val webClient: WebClient) {
  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }

  fun getDeliusCaseDetails(cases: List<UnallocatedCaseEntity>): Mono<List<DeliusCaseDetail>> {
    val getCaseDetails = GetCaseDetails(cases.map { CaseIdentifier(it.crn, it.convictionNumber.toString()) })
    return webClient
      .post()
      .uri("/allocation-demand")
      .body(Mono.just(getCaseDetails), GetCaseDetails::class.java)
      .retrieve()
      .bodyToMono(DeliusCaseDetails::class.java)
      .map { it.cases }
      .onErrorResume {
        log.warn("Error retrieving delius case details", it)
        Mono.just(emptyList())
      }
  }

  fun getDocuments(crn: String, convictionNumber: String): Flux<Document> {
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
}

data class CaseIdentifier(val crn: String, val eventNumber: String)
data class GetCaseDetails(val cases: List<CaseIdentifier>)

data class DeliusCaseDetail(val crn: String, val name: Name, val sentence: Sentence, val event: Event, val initialAppointment: InitialAppointment?)
data class Event(val number: String)
data class InitialAppointment(val date: LocalDate?)
data class DeliusCaseDetails(val cases: List<DeliusCaseDetail>)
data class Name(val forename: String, val surname: String)

data class Sentence(val date: LocalDate, val length: String)

data class Document @JsonCreator constructor(
  val id: String,
  val name: String,
  val dateCreated: ZonedDateTime,
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
