package uk.gov.justice.digital.hmpps.hmppsallocations.client

import org.slf4j.LoggerFactory
import org.springframework.web.reactive.function.client.WebClient
import reactor.core.publisher.Mono
import uk.gov.justice.digital.hmpps.hmppsallocations.jpa.entity.UnallocatedCaseEntity
import java.time.LocalDate

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
}

data class CaseIdentifier(val crn: String, val eventNumber: String)
data class GetCaseDetails(val cases: List<CaseIdentifier>)

data class DeliusCaseDetail(val crn: String, val name: Name, val sentence: Sentence, val event: Event, val initialAppointment: InitialAppointment?)
data class Event(val number: String)
data class InitialAppointment(val date: LocalDate?)
data class DeliusCaseDetails(val cases: List<DeliusCaseDetail>)
data class Name(val forename: String, val surname: String)

data class Sentence(val date: LocalDate, val length: String)
