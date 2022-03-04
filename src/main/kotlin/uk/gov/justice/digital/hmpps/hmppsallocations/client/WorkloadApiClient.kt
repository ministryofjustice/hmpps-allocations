package uk.gov.justice.digital.hmpps.hmppsallocations.client

import org.springframework.http.MediaType
import org.springframework.web.reactive.function.client.WebClient
import reactor.core.publisher.Mono
import uk.gov.justice.digital.hmpps.hmppsallocations.domain.CaseTypes
import uk.gov.justice.digital.hmpps.hmppsallocations.domain.OffenderManagerWorkloads
import uk.gov.justice.digital.hmpps.hmppsallocations.domain.PotentialCaseRequest
import uk.gov.justice.digital.hmpps.hmppsallocations.domain.PotentialOffenderManagerWorkload

class WorkloadApiClient(private val webClient: WebClient) {

  fun getOffenderManagersForTeam(): Mono<OffenderManagerWorkloads> {
    return webClient
      .get()
      .uri("/team/N03F01/offenderManagers")
      .retrieve()
      .bodyToMono(OffenderManagerWorkloads::class.java)
  }

  fun getPotentialCaseLoad(tier: String, caseType: CaseTypes, offenderManagerCode: String): Mono<PotentialOffenderManagerWorkload> {
    return webClient
      .post()
      .uri("/team/N03F01/offenderManagers/$offenderManagerCode/potentialCases")
      .bodyValue(PotentialCaseRequest(tier, caseType))
      .headers {
        it.contentType = MediaType.APPLICATION_JSON
      }
      .retrieve()
      .bodyToMono(PotentialOffenderManagerWorkload::class.java)
  }
}
