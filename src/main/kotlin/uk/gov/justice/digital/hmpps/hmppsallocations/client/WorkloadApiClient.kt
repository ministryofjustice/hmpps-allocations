package uk.gov.justice.digital.hmpps.hmppsallocations.client

import org.springframework.web.reactive.function.client.WebClient
import reactor.core.publisher.Mono
import uk.gov.justice.digital.hmpps.hmppsallocations.domain.OffenderManagerWorkloads

class WorkloadApiClient(private val webClient: WebClient) {

  fun getOffenderManagersForTeam(): Mono<OffenderManagerWorkloads> {
    return webClient
      .get()
      .uri("/team/N03F01/offenderManagers")
      .retrieve()
      .bodyToMono(OffenderManagerWorkloads::class.java)
  }
}
