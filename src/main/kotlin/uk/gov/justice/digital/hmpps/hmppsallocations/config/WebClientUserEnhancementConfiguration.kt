package uk.gov.justice.digital.hmpps.hmppsallocations.config

import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.core.context.ReactiveSecurityContextHolder
import org.springframework.security.oauth2.client.AuthorizedClientServiceReactiveOAuth2AuthorizedClientManager
import org.springframework.security.oauth2.client.InMemoryReactiveOAuth2AuthorizedClientService
import org.springframework.security.oauth2.client.ReactiveOAuth2AuthorizedClientManager
import org.springframework.security.oauth2.client.ReactiveOAuth2AuthorizedClientProviderBuilder
import org.springframework.security.oauth2.client.registration.ReactiveClientRegistrationRepository
import org.springframework.security.oauth2.client.web.reactive.function.client.ServerOAuth2AuthorizedClientExchangeFilterFunction
import org.springframework.security.oauth2.core.OAuth2AccessToken
import org.springframework.security.oauth2.server.resource.authentication.BearerTokenAuthentication
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken
import org.springframework.web.reactive.function.client.ClientRequest
import org.springframework.web.reactive.function.client.ExchangeFilterFunction
import org.springframework.web.reactive.function.client.WebClient
import reactor.core.publisher.Mono
import uk.gov.justice.digital.hmpps.hmppsallocations.client.AssessRisksNeedsApiClient
import uk.gov.justice.digital.hmpps.hmppsallocations.client.WorkforceAllocationsToDeliusApiClient

@Configuration
class WebClientUserEnhancementConfiguration(
  @Value("\${assessment.endpoint.url}") private val assessmentApiRootUri: String,
  @Value("\${assess-risks-needs.endpoint.url}") private val assessRisksNeedsApiRootUri: String,
  @Value("\${workforce-allocations-to-delius.endpoint.url}") private val workforceAllocationsToDeliusApiRootUri: String,
) {

  companion object {
    val log = LoggerFactory.getLogger(this::class.java)
  }

  private fun assessRisksNeedsWebClient(builder: WebClient.Builder): WebClient {
    return builder.baseUrl(assessRisksNeedsApiRootUri)
      .filter(logRequest())
      .filter(withAuth())
      .defaultHeader("Content-Type", "application/json")
      .build()
  }

  @Bean
  @Qualifier("assessRisksNeedsWebClientUserEnhancedAppScope")
  fun assessRisksNeedsWebClientUserEnhancedAppScope(
    builder: WebClient.Builder,
  ): WebClient {
    log.info("in assess risks need web client user enhanced app scope")
    return assessRisksNeedsWebClient(builder)
  }

  @Bean
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

  private fun logRequest(): ExchangeFilterFunction {
    return ExchangeFilterFunction.ofRequestProcessor { request: ClientRequest ->
      request.headers().forEach { name, values ->
        values.forEach { value -> println("Request Header: $name=$value") }
      }
      Mono.just(request)
    }
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
      .filter(logRequest())
      .build()
  }

  private fun withAuth(): ExchangeFilterFunction {
    return ExchangeFilterFunction.ofRequestProcessor { request ->
      ReactiveSecurityContextHolder.getContext()
        .map { securityContext ->
          val authentication = securityContext.authentication
          val token = when (authentication) {
            is BearerTokenAuthentication -> authentication.token.tokenValue
            is OAuth2AccessToken -> authentication.tokenValue
            is JwtAuthenticationToken -> authentication.token.tokenValue
            else -> null
          }
          token?.let {
            ClientRequest.from(request)
              .header("Authorization", "Bearer $it")
              .build()
          } ?: request
        }
    }
  }
}
