package uk.gov.justice.digital.hmpps.hmppsallocations.config

import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpHeaders
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.oauth2.client.AuthorizedClientServiceOAuth2AuthorizedClientManager
import org.springframework.security.oauth2.client.InMemoryOAuth2AuthorizedClientService
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientProviderBuilder
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService
import org.springframework.security.oauth2.client.endpoint.DefaultClientCredentialsTokenResponseClient
import org.springframework.security.oauth2.client.endpoint.OAuth2ClientCredentialsGrantRequest
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository
import org.springframework.security.oauth2.client.web.reactive.function.client.ServletOAuth2AuthorizedClientExchangeFilterFunction
import org.springframework.web.context.annotation.RequestScope
import org.springframework.web.reactive.function.client.ClientRequest
import org.springframework.web.reactive.function.client.ExchangeFunction
import org.springframework.web.reactive.function.client.WebClient
import uk.gov.justice.digital.hmpps.hmppsallocations.client.AssessRisksNeedsApiClient
import uk.gov.justice.digital.hmpps.hmppsallocations.client.AssessmentApiClient
import uk.gov.justice.digital.hmpps.hmppsallocations.client.WorkforceAllocationsToDeliusApiClient
import javax.servlet.http.HttpServletRequest

@Configuration
class WebClientUserEnhancementConfiguration(
  @Value("\${assessment.endpoint.url}") private val assessmentApiRootUri: String,
  @Value("\${assess-risks-needs.endpoint.url}") private val assessRisksNeedsApiRootUri: String,
  @Value("\${workforce-allocations-to-delius.endpoint.url}") private val workforceAllocationsToDeliusApiRootUri: String,
) {

  @Bean
  @RequestScope
  fun assessRisksNeedsWebClientUserEnhancedAppScope(builder: WebClient.Builder, httpServletRequest: HttpServletRequest): WebClient {
    return builder.baseUrl(assessRisksNeedsApiRootUri)
      .filter { request: ClientRequest, next: ExchangeFunction ->
        val filtered = ClientRequest.from(request)
          .header(HttpHeaders.AUTHORIZATION, httpServletRequest.getHeader(HttpHeaders.AUTHORIZATION))
          .build()
        next.exchange(filtered)
      }
      .build()
  }

  @Bean
  fun assessRisksNeedsApiClientUserEnhanced(@Qualifier("assessRisksNeedsWebClientUserEnhancedAppScope") webClient: WebClient): AssessRisksNeedsApiClient {
    return AssessRisksNeedsApiClient(webClient)
  }

  @Bean
  @RequestScope
  fun assessmentWebClientUserEnhancedAppScope(
    clientRegistrationRepository: ClientRegistrationRepository,
    builder: WebClient.Builder
  ): WebClient {
    return getOAuthWebClient(authorizedClientManagerUserEnhanced(clientRegistrationRepository), builder, assessmentApiRootUri, "assessment-api")
  }

  @Bean
  fun assessmentApiClientUserEnhanced(@Qualifier("assessmentWebClientUserEnhancedAppScope") webClient: WebClient): AssessmentApiClient {
    return AssessmentApiClient(webClient)
  }

  @Bean
  @RequestScope
  fun workforceAllocationsToDeliusApiWebClientUserEnhancedAppScope(
    clientRegistrationRepository: ClientRegistrationRepository,
    builder: WebClient.Builder
  ): WebClient {
    return getOAuthWebClient(authorizedClientManagerUserEnhanced(clientRegistrationRepository), builder, workforceAllocationsToDeliusApiRootUri, "workforce-allocations-to-delius-api")
  }

  @Bean
  fun workforceAllocationsToDeliusApiClientUserEnhanced(@Qualifier("workforceAllocationsToDeliusApiWebClientUserEnhancedAppScope") webClient: WebClient): WorkforceAllocationsToDeliusApiClient {
    return WorkforceAllocationsToDeliusApiClient(webClient)
  }

  private fun authorizedClientManagerUserEnhanced(clients: ClientRegistrationRepository?): OAuth2AuthorizedClientManager {
    val service: OAuth2AuthorizedClientService = InMemoryOAuth2AuthorizedClientService(clients)
    val manager = AuthorizedClientServiceOAuth2AuthorizedClientManager(clients, service)

    val defaultClientCredentialsTokenResponseClient = DefaultClientCredentialsTokenResponseClient()

    val authentication = SecurityContextHolder.getContext().authentication

    defaultClientCredentialsTokenResponseClient.setRequestEntityConverter { grantRequest: OAuth2ClientCredentialsGrantRequest ->
      val converter = CustomOAuth2ClientCredentialsGrantRequestEntityConverter()
      val username = authentication.name
      converter.enhanceWithUsername(grantRequest, username)
    }

    val authorizedClientProvider = OAuth2AuthorizedClientProviderBuilder.builder()
      .clientCredentials { clientCredentialsGrantBuilder: OAuth2AuthorizedClientProviderBuilder.ClientCredentialsGrantBuilder ->
        clientCredentialsGrantBuilder.accessTokenResponseClient(defaultClientCredentialsTokenResponseClient)
      }
      .build()

    manager.setAuthorizedClientProvider(authorizedClientProvider)
    return manager
  }

  private fun getOAuthWebClient(
    authorizedClientManager: OAuth2AuthorizedClientManager,
    builder: WebClient.Builder,
    rootUri: String,
    registrationId: String
  ): WebClient {
    val oauth2Client = ServletOAuth2AuthorizedClientExchangeFilterFunction(authorizedClientManager)
    oauth2Client.setDefaultClientRegistrationId(registrationId)
    return builder.baseUrl(rootUri)
      .apply(oauth2Client.oauth2Configuration())
      .build()
  }
}
