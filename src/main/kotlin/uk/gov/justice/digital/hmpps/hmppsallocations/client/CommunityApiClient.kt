package uk.gov.justice.digital.hmpps.hmppsallocations.client

import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.core.ParameterizedTypeReference
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import uk.gov.justice.digital.hmpps.hmppsallocations.domain.Conviction

@Component
class CommunityApiClient(@Qualifier("communityWebClientAppScope") private val webClient: WebClient) {

  fun getConvictions(crn: String): List<Conviction> {
    val responseType = object : ParameterizedTypeReference<List<Conviction>>() {}

    return webClient
      .get()
      .uri("/secure/offenders/crn/$crn/convictions?activeOnly=true")
      .retrieve()
      .bodyToMono(responseType)
      .block() ?: listOf()
  }
}
