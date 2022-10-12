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

  fun getInductionContacts(cases: List<UnallocatedCaseEntity>): Mono<UnallocatedCases> {
    val casesList = Cases(cases.map { Case(it.crn, it.convictionNumber.toString()) })
    return webClient
      .post()
      .uri("/allocation-demand")
      .body(Mono.just(casesList), Cases::class.java)
      .retrieve()
      .bodyToMono(UnallocatedCases::class.java)
      .onErrorResume {
        log.warn("Error retrieving induction contacts", it)
        Mono.empty()
      }
  }
}

data class Case(val crn: String, val eventNumber: String)
data class Cases(val cases: List<Case>)

data class UnallocatedCase(val crn: String, val event: Event, val initialAppointment: InitialAppointment)
data class Event(val number: String)
data class InitialAppointment(val date: LocalDate)
data class UnallocatedCases(val cases: List<UnallocatedCase>)