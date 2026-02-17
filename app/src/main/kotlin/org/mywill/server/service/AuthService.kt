package org.mywill.server.service

import org.mywill.server.domain.entity.User
import org.mywill.server.domain.repository.UserRepository
import org.mywill.server.restaccess.dto.AuthRequest
import org.mywill.server.restaccess.dto.AuthResponse
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service

@Service
class AuthService(
    private val userRepository: UserRepository,
    private val passwordEncoder: PasswordEncoder
) {
    fun register(request: AuthRequest): AuthResponse {
        if (userRepository.findByEmail(request.email) != null) {
            return AuthResponse(false, "User already exists")
        }
        val encodedPassword = passwordEncoder.encode(request.password)
        userRepository.save(User(email = request.email, password = encodedPassword))
        return AuthResponse(true, "User registered successfully")
    }

    fun login(request: AuthRequest): AuthResponse {
        val user = userRepository.findByEmail(request.email)
            ?: return AuthResponse(false, "Invalid email or password")
        
        if (passwordEncoder.matches(request.password, user.password)) {
            return AuthResponse(true, "Login successful")
        }
        return AuthResponse(false, "Invalid email or password")
    }
}
