package org.mywill.server.service

import io.github.oshai.kotlinlogging.KotlinLogging
import org.mywill.server.config.JwtUtils
import org.mywill.server.entity.User
import org.mywill.server.repository.UserRepository
import org.mywill.server.controller.dto.AuthRequest
import org.mywill.server.controller.dto.AuthResponse
import org.mywill.server.controller.dto.VerifyRequest
import jakarta.servlet.http.HttpSession
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.web.context.HttpSessionSecurityContextRepository
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import kotlin.random.Random

private val logger = KotlinLogging.logger {}

@Service
class AuthService(
    private val userRepository: UserRepository,
    private val passwordEncoder: PasswordEncoder,
    private val emailService: EmailService,
    private val httpSession: HttpSession,
    private val jwtUtils: JwtUtils
) {
    fun register(request: AuthRequest): AuthResponse {
        if (userRepository.findByEmail(request.email) != null) {
            return AuthResponse(false, "User already exists")
        }
        val encodedPassword = passwordEncoder.encode(request.password)
        val verificationCode = Random.nextInt(1000, 9999).toString()
        
        userRepository.save(
            User(
                email = request.email,
                password = encodedPassword,
                verified = false,
                verificationCode = verificationCode
            )
        )
        
        try {
            emailService.sendVerificationCode(request.email, verificationCode)
        } catch (e: Exception) {
            // В реальном приложении стоит логировать ошибку и возможно возвращать предупреждение
            logger.error(e) { "failed to send email: ${e.message}" }
            return AuthResponse(true, "Registration successful, but failed to send email. Your code is: $verificationCode")
        }
        
        return AuthResponse(true, "Registration successful. Please check your email for the verification code.")
    }

    fun verify(request: VerifyRequest): AuthResponse {
        val user = userRepository.findByEmail(request.email)
            ?: return AuthResponse(false, "User not found")
        
        if (user.verified) {
            return AuthResponse(false, "User already verified")
        }

        if (user.verificationCode == request.code) {
            user.verified = true
            user.verificationCode = null
            userRepository.save(user)
            return AuthResponse(true, "Email verified successfully")
        }
        
        return AuthResponse(false, "Invalid verification code")
    }

    fun login(request: AuthRequest): AuthResponse {
        val user = userRepository.findByEmail(request.email)
            ?: return AuthResponse(false, "Invalid email or password")
        
        if (!user.verified) {
            return AuthResponse(false, "Please verify your email first. Code: ${user.verificationCode}")
        }
        
        if (passwordEncoder.matches(request.password, user.password)) {
            val authentication = UsernamePasswordAuthenticationToken(user.email, null, emptyList())
            val securityContext = SecurityContextHolder.createEmptyContext()
            securityContext.authentication = authentication
            SecurityContextHolder.setContext(securityContext)
            
            httpSession.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, securityContext)
            
            val token = jwtUtils.generateToken(user.email)
            logger.info { "User ${user.email} authenticated. Session ID: ${httpSession.id}. Token generated." }
            return AuthResponse(true, "Login successful", token)
        }
        return AuthResponse(false, "Invalid email or password")
    }
}
