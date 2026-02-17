package org.mywill.server.config

import io.github.oshai.kotlinlogging.KotlinLogging
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.oauth2.server.resource.web.BearerTokenResolver
import org.springframework.security.oauth2.server.resource.web.DefaultBearerTokenResolver
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter

private val myLogger = KotlinLogging.logger {}

@Component
class JwtAuthenticationFilter(private val jwtUtils: JwtUtils,
                              private val bearerTokenResolver: BearerTokenResolver = DefaultBearerTokenResolver()
) : OncePerRequestFilter() {

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        val token = runCatching { bearerTokenResolver.resolve(request) }.getOrNull()

        if (!token.isNullOrBlank()) {
            try {
                if (jwtUtils.validateToken(token)) {
                    val email = jwtUtils.getEmailFromToken(token)
                    if (email != null && SecurityContextHolder.getContext().authentication == null) {
                        val authentication = UsernamePasswordAuthenticationToken(email, null, emptyList())
                        authentication.details = WebAuthenticationDetailsSource().buildDetails(request)
                        SecurityContextHolder.getContext().authentication = authentication
                        myLogger.debug { "Successfully authenticated user: $email" }
                    }
                }
            } catch (e: Exception) {
                myLogger.error { "Cannot set user authentication: ${e.message}" }
            }
        }
        filterChain.doFilter(request, response)
    }
}
