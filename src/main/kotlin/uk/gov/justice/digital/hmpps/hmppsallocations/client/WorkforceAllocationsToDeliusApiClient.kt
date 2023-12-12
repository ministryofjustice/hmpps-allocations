package uk.gov.justice.digital.hmpps.hmppsallocations.client

import com.fasterxml.jackson.annotation.JsonCreator
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.reactive.asFlow
import org.slf4j.LoggerFactory
import org.springframework.core.io.Resource
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.awaitBody
import org.springframework.web.reactive.function.client.awaitExchange
import org.springframework.web.reactive.function.client.awaitExchangeOrNull
import org.springframework.web.reactive.function.client.bodyToFlow
import org.springframework.web.reactive.function.client.createExceptionAndAwait
import reactor.core.publisher.Mono
import uk.gov.justice.digital.hmpps.hmppsallocations.client.dto.DeliusCaseView
import uk.gov.justice.digital.hmpps.hmppsallocations.client.dto.DeliusProbationRecord
import uk.gov.justice.digital.hmpps.hmppsallocations.client.dto.DeliusRisk
import uk.gov.justice.digital.hmpps.hmppsallocations.client.dto.PersonOnProbationStaffDetailsResponse
import uk.gov.justice.digital.hmpps.hmppsallocations.client.dto.UnallocatedEvents
import uk.gov.justice.digital.hmpps.hmppsallocations.jpa.entity.UnallocatedCaseEntity
import java.time.LocalDate
import java.time.ZonedDateTime

class WorkforceAllocationsToDeliusApiClient(private val webClient: WebClient) {
  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }

  suspend fun getUserAccess(crns: List<String>, username: String? = null): DeliusUserAccess {
    return webClient
      .post()
      .uri { ub ->
        ub.path("/users/limited-access")
        username?.also { ub.queryParam("username", it) }
        ub.build()
      }
      .bodyValue(crns)
      .awaitExchange { response ->
        when (response.statusCode()) {
          HttpStatus.OK -> response.awaitBody()
          else -> throw response.createExceptionAndAwait()
        }
      }
  }

  suspend fun getUserAccess(crn: String, username: String? = null): DeliusCaseAccess? =
    getUserAccess(listOf(crn), username).access.firstOrNull { it.crn == crn }

  fun getDeliusCaseDetails(cases: List<UnallocatedCaseEntity>): Flow<DeliusCaseDetail> {
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

  fun getDocuments(crn: String): Flow<Document> {
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

  suspend fun getUnallocatedEvents(crn: String): UnallocatedEvents? =
    webClient
      .get()
      .uri("/allocation-demand/$crn/unallocated-events")
      .awaitExchangeOrNull { response ->
        when (response.statusCode()) {
          HttpStatus.OK -> response.awaitBody()
          HttpStatus.NOT_FOUND -> null
          HttpStatus.FORBIDDEN -> throw ForbiddenOffenderError("Unable to access offender details for $crn")
          else -> throw response.createExceptionAndAwait()
        }
      }

  suspend fun personOnProbationStaffDetails(crn: String, staffCode: String): PersonOnProbationStaffDetailsResponse =
    webClient
      .get()
      .uri("/allocation-demand/impact?crn=$crn&staff=$staffCode")
      .retrieve()
      .awaitBody()

  suspend fun getAllocatedTeam(crn: String, convictionNumber: Int): AllocatedEvent? =
    webClient
      .get()
      .uri("allocation-completed/order-manager?crn=$crn&eventNumber=$convictionNumber")
      .awaitExchangeOrNull { response ->
        when (response.statusCode()) {
          HttpStatus.OK -> response.awaitBody()
          HttpStatus.NOT_FOUND -> null
          HttpStatus.FORBIDDEN -> throw ForbiddenOffenderError("Unable to access allocated team for crn: $crn, event number: $convictionNumber")
          else -> throw response.createExceptionAndAwait()
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
  val initialAppointment: InitialAppointment?,
  val event: Event,
  val probationStatus: ProbationStatus,
  val communityPersonManager: CommunityPersonManager?,
  val type: String,
)

data class Event(val number: String)

data class ProbationStatus(val description: String)
data class InitialAppointment(val date: LocalDate?, val staff: Staff?)

data class Staff @JsonCreator constructor(
  val name: Name,
)
data class DeliusCaseDetails(val cases: List<DeliusCaseDetail>)

data class Name(val forename: String, val middleName: String?, val surname: String) {
  fun getCombinedName() = "$forename ${middleName?.takeUnless { it.isBlank() }?.let { "$middleName " } ?: ""}$surname"
}

data class CommunityPersonManager(val name: Name, val grade: String?)

data class Sentence(val date: LocalDate, val length: String)

data class Document @JsonCreator constructor(
  val id: String?,
  val name: String,
  val dateCreated: ZonedDateTime?,
  val sensitive: Boolean,
  val relatedTo: DocumentRelatedTo,
)

data class DocumentRelatedTo @JsonCreator constructor(
  val type: String,
  val name: String,
  val description: String,
  val event: DocumentEvent?,
)

data class DocumentEvent @JsonCreator constructor(
  val eventType: String,
  val eventNumber: String,
  val mainOffence: String,
)

data class DeliusCaseAccess(
  val crn: String,
  val userRestricted: Boolean,
  val userExcluded: Boolean,
)

data class DeliusUserAccess(
  val access: List<DeliusCaseAccess>,
)

data class AllocatedEvent @JsonCreator constructor(
  val teamCode: String,
)
