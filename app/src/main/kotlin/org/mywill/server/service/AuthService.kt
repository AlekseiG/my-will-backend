package org.mywill.server.service

import io.github.oshai.kotlinlogging.KotlinLogging
import org.mywill.server.domain.entity.User
import org.mywill.server.domain.repository.UserRepository
import org.mywill.server.restaccess.dto.AuthRequest
import org.mywill.server.restaccess.dto.AuthResponse
import org.mywill.server.restaccess.dto.VerifyRequest
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import kotlin.random.Random

private val logger = KotlinLogging.logger {}

@Service
class AuthService(
    private val userRepository: UserRepository,
    private val passwordEncoder: PasswordEncoder,
    private val emailService: EmailService
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
            return AuthResponse(true, "Login successful")
        }
        return AuthResponse(false, "Invalid email or password")
    }
}
