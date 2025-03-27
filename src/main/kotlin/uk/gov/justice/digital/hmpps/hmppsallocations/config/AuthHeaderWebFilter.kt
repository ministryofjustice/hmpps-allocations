package uk.gov.justice.digital.hmpps.hmppsallocations.config

import org.springframework.http.HttpHeaders
import org.springframework.security.core.context.ReactiveSecurityContextHolder
import org.springframework.security.core.context.SecurityContext
import org.springframework.stereotype.Component
import org.springframework.web.server.ServerWebExchange
import org.springframework.web.server.WebFilter
import org.springframework.web.server.WebFilterChain
import reactor.core.publisher.Mono

@Component
class AuthHeaderWebFilter : WebFilter {
  override fun filter(serverWebExchange: ServerWebExchange, webFilterChain: WebFilterChain): Mono<Void> {
    val path = serverWebExchange.request.uri.path
    if (path.startsWith("/v3/api") || path.startsWith("/health") || path.startsWith("/info") || path.startsWith("/swagger")) {
      return webFilterChain.filter(serverWebExchange)
    }
    val authHeader = serverWebExchange.request.headers[HttpHeaders.AUTHORIZATION]?.firstOrNull()
    return ReactiveSecurityContextHolder.getContext()
      .flatMap { securityContext: SecurityContext ->
        if (authHeader != null) {
          webFilterChain.filter(serverWebExchange).contextWrite {
            it.put(HttpHeaders.AUTHORIZATION, authHeader)
          }
        } else {
          webFilterChain.filter(serverWebExchange)
        }
      }
  }
}
