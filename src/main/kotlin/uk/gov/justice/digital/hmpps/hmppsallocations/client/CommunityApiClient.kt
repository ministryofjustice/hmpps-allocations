package uk.gov.justice.digital.hmpps.hmppsallocations.client

import com.fasterxml.jackson.annotation.JsonCreator
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import uk.gov.justice.digital.hmpps.hmppsallocations.domain.Conviction

@Component
class CommunityApiClient(@Qualifier("communityWebClientAppScope") private val webClient: WebClient) {

  /*fun getConvictions(crn: String) {
    webClient
      .get()
      .uri("/secure/offenders/crn/$crn/convictions")
      .retrieve().bodyToMono(Convictions::class.java)
  }*/

/*  companion object {
    private val log = LoggerFactory.getLogger(CommunityApiClient::class.java)
  }*/

  data class Convictions @JsonCreator constructor(
    val listOfConviction: List<Conviction>
  )
}
