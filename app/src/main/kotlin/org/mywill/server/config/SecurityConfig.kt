package org.mywill.server.config

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.crypto.argon2.Argon2PasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.config.Customizer
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter
import org.springframework.security.web.authentication.HttpStatusEntryPoint
import org.springframework.http.HttpStatus
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository
import org.springframework.security.oauth2.client.web.DefaultOAuth2AuthorizationRequestResolver
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestCustomizers
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.security.core.Authentication
import org.springframework.security.oauth2.core.user.OAuth2User

@Configuration
@EnableWebSecurity
class SecurityConfig(
    private val jwtAuthenticationFilter: JwtAuthenticationFilter,
    private val jwtUtils: JwtUtils,
    private val userRepository: org.mywill.server.repository.UserRepository,
    private val clientRegistrationRepository: ClientRegistrationRepository,
    @Value("\${app.frontend-base-url:http://localhost:8081}")
    private val frontendBaseUrl: String
) {

    @Bean
    fun passwordEncoder(): PasswordEncoder {
        return Argon2PasswordEncoder.defaultsForSpringSecurity_v5_8()
    }

    @Bean
    fun securityFilterChain(http: HttpSecurity): SecurityFilterChain {
        http
            .cors(Customizer.withDefaults())
            .csrf { it.disable() }
            .sessionManagement { it.sessionCreationPolicy(SessionCreationPolicy.STATELESS) }
            .exceptionHandling { exceptions ->
                exceptions.defaultAuthenticationEntryPointFor(
                    HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED),
                    PathPatternRequestMatcher.withDefaults().matcher("/api/**")
                )
            }
            .authorizeHttpRequests { auth ->
                auth
                    .requestMatchers("/", "/admin/ui", "/auth/**", "/index.html", "/static/**", "/*.js", "/oauth2/**", "/login/oauth2/**").permitAll()
                    .requestMatchers("/swagger-ui/**", "/v3/api-docs/**", "/swagger-resources/**", "/webjars/**").permitAll()
                    .requestMatchers(org.springframework.http.HttpMethod.OPTIONS, "/**").permitAll()
                    .anyRequest().authenticated()
            }
            .oauth2Login { oauth2 ->
                oauth2.authorizationEndpoint {
                    val resolver = DefaultOAuth2AuthorizationRequestResolver(clientRegistrationRepository, "/oauth2/authorization")
                    resolver.setAuthorizationRequestCustomizer { builder ->
                        val requestAttributes = org.springframework.web.context.request.RequestContextHolder.getRequestAttributes() as? org.springframework.web.context.request.ServletRequestAttributes
                        val request = requestAttributes?.request
                        val host = request?.serverName ?: ""
                        
                        // Добавляем параметры только если заходим по прямому IP (не localhost и не домен)
                        val isIpAddress = host.matches(Regex("""\d{1,3}(\.\d{1,3}){3}"""))
                        if (isIpAddress && host != "127.0.0.1") {
                            builder.additionalParameters { params ->
                                params["device_id"] = "android_app"
                                params["device_name"] = "MyWill_App"
                            }
                        }
                        OAuth2AuthorizationRequestCustomizers.withPkce().accept(builder)
                    }
                    it.authorizationRequestResolver(resolver)
                }
                oauth2.successHandler(object : SimpleUrlAuthenticationSuccessHandler() {
                    override fun onAuthenticationSuccess(
                        request: HttpServletRequest,
                        response: HttpServletResponse,
                        authentication: Authentication
                    ) {
                        val principal = authentication.principal as OAuth2User
                        val email = principal.getAttribute<String>("email") ?: ""
                        
                        val user = userRepository.findByEmail(email) ?: userRepository.save(
                            org.mywill.server.entity.User(email = email, password = null, verified = true)
                        )

                        val token = jwtUtils.generateToken(user.email)
                        
                        val isMobile = request.getHeader("User-Agent")?.contains("Android", ignoreCase = true) == true
                        val targetUrl = if (isMobile) {
                            "mywill://auth?token=$token"
                        } else {
                            "$frontendBaseUrl/#token=$token"
                        }
                        
                        println("[DEBUG_LOG] Redirecting user to: $targetUrl")
                        getRedirectStrategy().sendRedirect(request, response, targetUrl)
                    }
                })
            }
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter::class.java)
        
        return http.build()
    }
}
