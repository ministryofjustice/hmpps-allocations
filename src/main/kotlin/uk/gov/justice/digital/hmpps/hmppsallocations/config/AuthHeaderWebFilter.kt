package uk.gov.justice.digital.hmpps.hmppsallocations.config

import org.slf4j.LoggerFactory
import org.springframework.http.HttpHeaders
import org.springframework.security.core.GrantedAuthority
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
    val authHeader = serverWebExchange.request.headers[HttpHeaders.AUTHORIZATION]?.firstOrNull() ?: "none"
    // log.info("Auth header $authHeader")
    // val authHeader = serverWebExchange.request.headers[HttpHeaders.AUTHORIZATION]?.firstOrNull() ?: "none"
    // log.info("Auth header $authHeader")
    return ReactiveSecurityContextHolder.getContext()
      .flatMap { securityContext: SecurityContext ->
        val roles = securityContext.authentication.authorities.map(GrantedAuthority::getAuthority)
        val principal = securityContext.authentication.principal
        log.info("User roles: $roles")
        log.info("principal: $principal")
        webFilterChain.filter(serverWebExchange).contextWrite {
          it.put(HttpHeaders.AUTHORIZATION, authHeader)
        }
      }
  }

  companion object {
    val log = LoggerFactory.getLogger(this::class.java)
  }
}
