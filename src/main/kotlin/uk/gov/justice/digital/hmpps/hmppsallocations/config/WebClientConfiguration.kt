package uk.gov.justice.digital.hmpps.hmppsallocations.config

import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.oauth2.client.AuthorizedClientServiceReactiveOAuth2AuthorizedClientManager
import org.springframework.security.oauth2.client.ReactiveOAuth2AuthorizedClientManager
import org.springframework.security.oauth2.client.ReactiveOAuth2AuthorizedClientProviderBuilder
import org.springframework.security.oauth2.client.ReactiveOAuth2AuthorizedClientService
import org.springframework.security.oauth2.client.registration.ReactiveClientRegistrationRepository
import org.springframework.security.oauth2.client.web.reactive.function.client.ServerOAuth2AuthorizedClientExchangeFilterFunction
import org.springframework.web.reactive.function.client.WebClient
import uk.gov.justice.digital.hmpps.hmppsallocations.client.HmppsTierApiClient
import uk.gov.justice.digital.hmpps.hmppsallocations.client.WorkforceAllocationsToDeliusApiClient

@Configuration
class WebClientConfiguration(
  @Value("\${hmpps-tier.endpoint.url}") private val hmppsTierApiRootUri: String,
  @Value("\${workforce-allocations-to-delius.endpoint.url}") private val workforceAllocationsToDeliusApiRootUri: String,
) {

  @Bean
  fun authorizedClientManagerAppScope(
    clientRegistrationRepository: ReactiveClientRegistrationRepository,
    oAuth2AuthorizedClientService: ReactiveOAuth2AuthorizedClientService
  ): ReactiveOAuth2AuthorizedClientManager {
    val authorizedClientProvider = ReactiveOAuth2AuthorizedClientProviderBuilder.builder().clientCredentials().build()
    val authorizedClientManager = AuthorizedClientServiceReactiveOAuth2AuthorizedClientManager(
      clientRegistrationRepository,
      oAuth2AuthorizedClientService
    )
    authorizedClientManager.setAuthorizedClientProvider(authorizedClientProvider)
    return authorizedClientManager
  }

  @Bean
  fun hmppsTierWebClientAppScope(
    @Qualifier(value = "authorizedClientManagerAppScope") authorizedClientManager: ReactiveOAuth2AuthorizedClientManager,
    builder: WebClient.Builder
  ): WebClient {
    return getOAuthWebClient(authorizedClientManager, builder, hmppsTierApiRootUri, "hmpps-tier-api")
  }

  @Bean
  fun hmppsTierApiClient(@Qualifier("hmppsTierWebClientAppScope") webClient: WebClient): HmppsTierApiClient {
    return HmppsTierApiClient(webClient)
  }

  @Bean
  fun workforceAllocationsToDeliusApiClientWebClientAppScope(
    @Qualifier(value = "authorizedClientManagerAppScope") authorizedClientManager: ReactiveOAuth2AuthorizedClientManager,
    builder: WebClient.Builder
  ): WebClient {
    return getOAuthWebClient(authorizedClientManager, builder, workforceAllocationsToDeliusApiRootUri, "workforce-allocations-to-delius-api")
  }
  @Bean
  fun workforceAllocationsToDeliusApiClient(@Qualifier("workforceAllocationsToDeliusApiClientWebClientAppScope") webClient: WebClient): WorkforceAllocationsToDeliusApiClient {
    return WorkforceAllocationsToDeliusApiClient(webClient)
  }

  private fun getOAuthWebClient(
    authorizedClientManager: ReactiveOAuth2AuthorizedClientManager,
    builder: WebClient.Builder,
    rootUri: String,
    registrationId: String
  ): WebClient {
    val oauth2Client = ServerOAuth2AuthorizedClientExchangeFilterFunction(authorizedClientManager)
    oauth2Client.setDefaultClientRegistrationId(registrationId)
    return builder.baseUrl(rootUri)
      .filter(oauth2Client)
      .build()
  }
}
