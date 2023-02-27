package uk.gov.justice.digital.hmpps.hmppsallocations.client

import org.springframework.http.HttpStatus
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.awaitBody
import org.springframework.web.reactive.function.client.awaitExchangeOrNull
import org.springframework.web.reactive.function.client.createExceptionAndAwait

class CommunityApiClient(private val webClient: WebClient) {

  suspend fun getUserAccess(crn: String): DeliusUserAccess? {
    return webClient
      .get()
      .uri("/offenders/crn/$crn/userAccess")
      .awaitExchangeOrNull { response ->
        when (response.statusCode()) {
          HttpStatus.OK -> response.awaitBody()
          HttpStatus.FORBIDDEN -> response.awaitBody()
          HttpStatus.NOT_FOUND -> null
          else -> throw response.createExceptionAndAwait()
        }
      }
  }
}

data class DeliusUserAccess(
  val userRestricted: Boolean,
  val userExcluded: Boolean
)
