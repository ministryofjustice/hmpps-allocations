package uk.gov.justice.digital.hmpps.hmppsallocations.client

import org.springframework.web.reactive.function.client.WebClient
import reactor.core.publisher.Mono
import uk.gov.justice.digital.hmpps.hmppsallocations.domain.OffenderManagerOverview

class WorkloadApiClient(private val webClient: WebClient) {

  fun getOffenderManagerOverview(offenderManagerCode: String): Mono<OffenderManagerOverview> {
    return webClient
      .get()
      .uri("/team/N03F01/offenderManagers/$offenderManagerCode")
      .retrieve()
      .bodyToMono(OffenderManagerOverview::class.java)
  }
}
