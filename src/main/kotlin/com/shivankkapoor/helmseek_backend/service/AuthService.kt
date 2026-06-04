package com.shivankkapoor.helmseek_backend.service

import com.shivankkapoor.helmseek_backend.model.Session
import com.shivankkapoor.helmseek_backend.repository.SessionRepository
import com.shivankkapoor.helmseek_backend.repository.UserRepository
import org.slf4j.LoggerFactory
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class AuthService(
    private val userRepository: UserRepository,
    private val sessionRepository: SessionRepository,
    private val passwordEncoder: PasswordEncoder
) {

    companion object {
        private val log = LoggerFactory.getLogger(AuthService::class.java)
    }

    @Transactional
    fun login(username: String, password: String): UUID {
        val user = userRepository.findByUsername(username)
            ?: run {
                log.warn("Login failed — unknown username={}", username)
                throw AuthException("Invalid credentials")
            }
        if (!passwordEncoder.matches(password, user.password)) {
            log.warn("Login failed — wrong password for username={}", username)
            throw AuthException("Invalid credentials")
        }
        val session = sessionRepository.save(Session(user = user))
        log.debug("Session created id={} for username={}", session.id, username)
        return session.id!!
    }

    @Transactional
    fun logout(sessionId: UUID) {
        sessionRepository.deleteById(sessionId)
        log.debug("Session deleted id={}", sessionId)
    }
}

class AuthException(message: String) : RuntimeException(message)
