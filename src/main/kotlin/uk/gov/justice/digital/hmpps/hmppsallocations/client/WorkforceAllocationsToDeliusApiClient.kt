package uk.gov.justice.digital.hmpps.hmppsallocations.client

import com.fasterxml.jackson.annotation.JsonCreator
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.reactive.asFlow
import kotlinx.coroutines.reactor.awaitSingleOrNull
import org.owasp.html.PolicyFactory
import org.owasp.html.Sanitizers
import org.slf4j.LoggerFactory
import org.springframework.core.io.Resource
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.awaitBody
import org.springframework.web.reactive.function.client.awaitExchange
import org.springframework.web.reactive.function.client.bodyToFlow
import org.springframework.web.reactive.function.client.bodyToMono
import org.springframework.web.reactive.function.client.createExceptionAndAwait
import reactor.core.publisher.Mono
import reactor.util.retry.Retry
import uk.gov.justice.digital.hmpps.hmppsallocations.client.dto.DeliusCaseView
import uk.gov.justice.digital.hmpps.hmppsallocations.client.dto.DeliusProbationRecord
import uk.gov.justice.digital.hmpps.hmppsallocations.client.dto.DeliusRisk
import uk.gov.justice.digital.hmpps.hmppsallocations.client.dto.PersonOnProbationStaffDetailsResponse
import uk.gov.justice.digital.hmpps.hmppsallocations.client.dto.UnallocatedEvents
import uk.gov.justice.digital.hmpps.hmppsallocations.jpa.entity.UnallocatedCaseEntity
import java.time.Duration
import java.time.LocalDate
import java.time.ZonedDateTime

private const val NUMBER_OF_RETRIES = 3L
private const val RETRY_INTERVAL = 3L

class WorkforceAllocationsToDeliusApiClient(private val webClient: WebClient) {
  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
    private val policy: PolicyFactory = Sanitizers.FORMATTING.and(Sanitizers.LINKS)
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

  fun getDeliusCaseDetailsCases(cases: List<UnallocatedCaseEntity>): Flow<DeliusCaseDetail> =
    getDeliusCaseDetails(
      caseDetails = GetCaseDetails(
        cases.map { CaseIdentifier(it.crn, it.convictionNumber.toString()) },
      ),
    )
      .flatMapIterable { it.cases }
      .asFlow()

  fun getDeliusCaseDetails(crn: String, convictionNumber: Long): Mono<DeliusCaseDetails> =
    getDeliusCaseDetails(
      caseDetails = GetCaseDetails(
        listOf(CaseIdentifier(crn, convictionNumber.toString())),
      ),
    ).onErrorReturn(
      DeliusCaseDetails(
        cases = emptyList(),
      ),
    )

  private fun getDeliusCaseDetails(caseDetails: GetCaseDetails): Mono<DeliusCaseDetails> {
    return webClient
      .post()
      .uri("/allocation-demand")
      .body(Mono.just(caseDetails), GetCaseDetails::class.java)
      .retrieve()
      .bodyToMono(DeliusCaseDetails::class.java)
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

  fun getDeliusCaseView(crn: String, convictionNumber: Long): Mono<DeliusCaseView> {
    return webClient
      .get()
      .uri("/allocation-demand/$crn/$convictionNumber/case-view")
      .retrieve()
      .bodyToMono()
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
      .retrieve()
      .onStatus({ status -> status.is5xxServerError }) {
        Mono.error(AllocationsServerError("Internal server error"))
      }
      .onStatus({ status -> status.value() == HttpStatus.FORBIDDEN.value() }) {
        Mono.error(ForbiddenOffenderError("Unable to access offender details for $crn"))
      }
      .onStatus({ status -> status.value() == HttpStatus.NOT_FOUND.value() }) {
        Mono.empty()
      }
      .bodyToMono(UnallocatedEvents::class.java)
      .retryWhen(
        Retry.backoff(NUMBER_OF_RETRIES, Duration.ofSeconds(RETRY_INTERVAL))
          .filter { it is AllocationsServerError },
      )
      .awaitSingleOrNull()

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
      .retrieve()
      .onStatus({ status -> status.value() == HttpStatus.NOT_FOUND.value() }) {
        Mono.empty()
      }
      .onStatus({ status -> status.value() == HttpStatus.FORBIDDEN.value() }) {
        Mono.error(ForbiddenOffenderError("Unable to access allocated team for $crn , event number: $convictionNumber"))
      }.onStatus({ status -> status.value() == HttpStatus.INTERNAL_SERVER_ERROR.value() }) {
        Mono.error(AllocationsServerError("Internal server error"))
      }
      .bodyToMono(AllocatedEvent::class.java)
      .retryWhen(
        Retry.backoff(NUMBER_OF_RETRIES, Duration.ofSeconds(RETRY_INTERVAL))
          .filter { it is AllocationsServerError },
      )
      .awaitSingleOrNull()
}

class ForbiddenOffenderError(msg: String) : RuntimeException(msg)
class AllocationsServerError(msg: String) : RuntimeException(msg)
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
  var type: String,
  val handoverDate: LocalDate?,
)

data class Event(val number: String)

data class ProbationStatus(
  val status: String,
  val description: String,
)

data class InitialAppointment(val date: LocalDate, val staff: Staff)

data class Staff @JsonCreator constructor(
  val name: Name,
)
data class DeliusCaseDetails(val cases: List<DeliusCaseDetail>)

data class Name(var forename: String, var middleName: String?, var surname: String) {
  init {
    forename = Sanitizers.FORMATTING.and(Sanitizers.LINKS).sanitize(forename)
    middleName = Sanitizers.FORMATTING.and(Sanitizers.LINKS).sanitize(middleName)
    surname = Sanitizers.FORMATTING.and(Sanitizers.LINKS).sanitize(surname)
  }
  fun getCombinedName() = "$forename ${middleName?.takeUnless { it.isBlank() }?.let { "$middleName " } ?: ""}$surname"
}

data class CommunityPersonManager(val name: Name, val grade: String?, val teamCode: String?)

data class Sentence(val date: LocalDate, val length: String)

data class Document @JsonCreator constructor(
  val id: String?,
  var name: String,
  val dateCreated: ZonedDateTime?,
  val sensitive: Boolean,
  val relatedTo: DocumentRelatedTo,
) {
  init {
    name = Sanitizers.FORMATTING.and(Sanitizers.LINKS).sanitize(name)
  }
}

data class DocumentRelatedTo @JsonCreator constructor(
  val type: String,
  var name: String,
  var description: String,
  val event: DocumentEvent?,
) {
  init {
    name = Sanitizers.FORMATTING.and(Sanitizers.LINKS).sanitize(name)
    description = Sanitizers.FORMATTING.and(Sanitizers.LINKS).sanitize(description)
  }
}

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
