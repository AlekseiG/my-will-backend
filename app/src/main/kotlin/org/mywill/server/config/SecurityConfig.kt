package org.mywill.server.config

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

@Configuration
@EnableWebSecurity
class SecurityConfig(
    private val jwtAuthenticationFilter: JwtAuthenticationFilter,
    private val jwtUtils: JwtUtils,
    private val userRepository: org.mywill.server.repository.UserRepository
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
                    .requestMatchers(org.springframework.http.HttpMethod.OPTIONS, "/**").permitAll()
                    .anyRequest().authenticated()
            }
            .oauth2Login { oauth2 ->
                oauth2.loginPage("http://localhost:8081") // Указываем фронтенд как страницу логина
                oauth2.authorizationEndpoint { it.baseUri("/oauth2/authorization") }
                oauth2.successHandler { _, response, authentication ->
                    val principal = authentication.principal as org.springframework.security.oauth2.core.user.OAuth2User
                    val email = principal.getAttribute<String>("email") ?: throw RuntimeException("Email not found in OAuth2 provider")

                    // Find or create user
                    val user = userRepository.findByEmail(email) ?: userRepository.save(
                        org.mywill.server.entity.User(email = email, password = null, verified = true)
                    )

                    val token = jwtUtils.generateToken(user.email)
                    response.sendRedirect("http://localhost:8081/#token=$token")
                }
            }
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter::class.java)
        return http.build()
    }
}
