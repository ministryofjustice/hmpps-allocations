package uk.gov.justice.digital.hmpps.hmppsallocations.config

import org.springframework.http.MediaType
import org.springframework.security.core.Authentication
import org.springframework.security.core.context.ReactiveSecurityContextHolder
import org.springframework.security.core.context.SecurityContext
import org.springframework.security.oauth2.client.endpoint.OAuth2ClientCredentialsGrantRequest
import org.springframework.security.oauth2.client.endpoint.ReactiveOAuth2AccessTokenResponseClient
import org.springframework.security.oauth2.core.endpoint.OAuth2AccessTokenResponse
import org.springframework.security.oauth2.core.endpoint.OAuth2ParameterNames
import org.springframework.security.oauth2.core.web.reactive.function.OAuth2BodyExtractors
import org.springframework.util.CollectionUtils
import org.springframework.web.reactive.function.BodyInserters
import org.springframework.web.reactive.function.client.ClientResponse
import org.springframework.web.reactive.function.client.WebClient
import reactor.core.publisher.Mono
import java.io.UnsupportedEncodingException
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.util.Collections

class UserEnhancedReactiveClientCredentialsTokenResponseClient :
  ReactiveOAuth2AccessTokenResponseClient<OAuth2ClientCredentialsGrantRequest> {

  private val webClient = WebClient.builder().build()

  private val bodyExtractor = OAuth2BodyExtractors
    .oauth2AccessTokenResponse()
  override fun getTokenResponse(authorizationGrantRequest: OAuth2ClientCredentialsGrantRequest?): Mono<OAuth2AccessTokenResponse> {
    return ReactiveSecurityContextHolder.getContext()
      .map(SecurityContext::getAuthentication)
      .map(Authentication::getName)
      .flatMap { username ->
        webClient.post()
          .uri(authorizationGrantRequest!!.clientRegistration.providerDetails.tokenUri)
          .headers { headers ->
            headers.contentType = MediaType.APPLICATION_FORM_URLENCODED
            headers.accept = Collections.singletonList(MediaType.APPLICATION_JSON)
            headers.setBasicAuth(encodeClientCredential(authorizationGrantRequest.clientRegistration.clientId), encodeClientCredential(authorizationGrantRequest.clientRegistration.clientSecret))
          }
          .body(
            BodyInserters.fromFormData(OAuth2ParameterNames.GRANT_TYPE, authorizationGrantRequest.grantType.value)
              .with("username", username)
          )
          .exchange()
          .flatMap { response ->
            readTokenResponse(authorizationGrantRequest, response)
          }
      }
  }

  private fun encodeClientCredential(clientCredential: String): String? {
    return try {
      URLEncoder.encode(clientCredential, StandardCharsets.UTF_8.toString())
    } catch (ex: UnsupportedEncodingException) {
      // Will not happen since UTF-8 is a standard charset
      throw IllegalArgumentException(ex)
    }
  }

  private fun readTokenResponse(grantRequest: OAuth2ClientCredentialsGrantRequest, response: ClientResponse): Mono<OAuth2AccessTokenResponse?>? {
    return response.body<Mono<OAuth2AccessTokenResponse>>(this.bodyExtractor)
      .map { tokenResponse: OAuth2AccessTokenResponse ->
        populateTokenResponse(
          grantRequest,
          tokenResponse
        )
      }
  }

  fun populateTokenResponse(grantRequest: OAuth2ClientCredentialsGrantRequest?, tokenResponse: OAuth2AccessTokenResponse): OAuth2AccessTokenResponse? {
    var tokenResponse = tokenResponse
    if (CollectionUtils.isEmpty(tokenResponse.accessToken.scopes)) {
      val defaultScopes: Set<String> = emptySet()
      tokenResponse = OAuth2AccessTokenResponse
        .withResponse(tokenResponse)
        .scopes(defaultScopes)
        .build()
    }
    return tokenResponse
  }
}
