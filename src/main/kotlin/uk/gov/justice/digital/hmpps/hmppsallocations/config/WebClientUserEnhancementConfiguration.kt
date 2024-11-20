package uk.gov.justice.digital.hmpps.hmppsallocations.config

import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import org.springframework.security.oauth2.client.AuthorizedClientServiceReactiveOAuth2AuthorizedClientManager
import org.springframework.security.oauth2.client.InMemoryReactiveOAuth2AuthorizedClientService
import org.springframework.security.oauth2.client.ReactiveOAuth2AuthorizedClientManager
import org.springframework.security.oauth2.client.ReactiveOAuth2AuthorizedClientProviderBuilder
import org.springframework.security.oauth2.client.registration.ReactiveClientRegistrationRepository
import org.springframework.security.oauth2.client.web.reactive.function.client.ServerOAuth2AuthorizedClientExchangeFilterFunction
import org.springframework.web.reactive.function.client.WebClient
import uk.gov.justice.digital.hmpps.hmppsallocations.client.AssessRisksNeedsApiClient
import uk.gov.justice.digital.hmpps.hmppsallocations.client.WorkforceAllocationsToDeliusApiClient

@Configuration
class WebClientUserEnhancementConfiguration(
  @Value("\${assessment.endpoint.url}") private val assessmentApiRootUri: String,
  @Value("\${assess-risks-needs.endpoint.url}") private val assessRisksNeedsApiRootUri: String,
  @Value("\${workforce-allocations-to-delius.endpoint.url}") private val workforceAllocationsToDeliusApiRootUri: String,
) {

  @Bean
  @Qualifier("assessRisksNeedsWebClientUserEnhancedAppScope")
  fun assessRisksNeedsWebClientUserEnhancedAppScope(
    clientRegistrationRepository: ReactiveClientRegistrationRepository,
    builder: WebClient.Builder,
  ): WebClient {
    return getOAuthWebClient(
      authorizedClientManagerUserEnhanced(clientRegistrationRepository),
      builder,
      assessRisksNeedsApiRootUri,
      "assess-risks-needs-api",
    )
  }

  @Primary
  @Bean
  @Qualifier("assessRisksNeedsApiClientUserEnhanced")
  fun assessRisksNeedsApiClientUserEnhanced(@Qualifier("assessRisksNeedsWebClientUserEnhancedAppScope") webClient: WebClient): AssessRisksNeedsApiClient {
    return AssessRisksNeedsApiClient(webClient)
  }

  @Bean
  fun assessmentWebClientUserEnhancedAppScope(
    clientRegistrationRepository: ReactiveClientRegistrationRepository,
    builder: WebClient.Builder,
  ): WebClient {
    return getOAuthWebClient(
      authorizedClientManagerUserEnhanced(clientRegistrationRepository),
      builder,
      assessmentApiRootUri,
      "assessment-api",
    )
  }

  @Bean
  fun workforceAllocationsToDeliusApiWebClientUserEnhancedAppScope(
    clientRegistrationRepository: ReactiveClientRegistrationRepository,
    builder: WebClient.Builder,
  ): WebClient {
    return getOAuthWebClient(
      authorizedClientManagerUserEnhanced(clientRegistrationRepository),
      builder,
      workforceAllocationsToDeliusApiRootUri,
      "workforce-allocations-to-delius-api",
    )
  }

  @Bean
  fun workforceAllocationsToDeliusApiClientUserEnhanced(@Qualifier("workforceAllocationsToDeliusApiWebClientUserEnhancedAppScope") webClient: WebClient): WorkforceAllocationsToDeliusApiClient {
    return WorkforceAllocationsToDeliusApiClient(webClient)
  }

  private fun authorizedClientManagerUserEnhanced(clients: ReactiveClientRegistrationRepository): ReactiveOAuth2AuthorizedClientManager {
    val service = InMemoryReactiveOAuth2AuthorizedClientService(clients)
    val manager = AuthorizedClientServiceReactiveOAuth2AuthorizedClientManager(clients, service)
    val provider = ReactiveOAuth2AuthorizedClientProviderBuilder.builder()
      .clientCredentials()
      .build()
    manager.setAuthorizedClientProvider(provider)
    return manager
  }

  private fun getOAuthWebClient(
    authorizedClientManager: ReactiveOAuth2AuthorizedClientManager,
    builder: WebClient.Builder,
    rootUri: String,
    registrationId: String,
  ): WebClient {
    val oauth2Client = ServerOAuth2AuthorizedClientExchangeFilterFunction(authorizedClientManager)
    oauth2Client.setDefaultClientRegistrationId(registrationId)
    return builder.baseUrl(rootUri)
      .filter(oauth2Client)
      .build()
  }
}
