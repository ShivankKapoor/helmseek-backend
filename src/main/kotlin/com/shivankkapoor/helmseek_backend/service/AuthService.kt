package com.shivankkapoor.helmseek_backend.service

import com.shivankkapoor.helmseek_backend.model.Session
import com.shivankkapoor.helmseek_backend.model.User
import com.shivankkapoor.helmseek_backend.repository.SessionRepository
import com.shivankkapoor.helmseek_backend.repository.UserRepository
import org.slf4j.LoggerFactory
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.OffsetDateTime
import java.util.UUID

@Service
class AuthService(
    private val userRepository: UserRepository,
    private val sessionRepository: SessionRepository,
    private val passwordEncoder: PasswordEncoder,
    private val interactionService: InteractionService
) {

    companion object {
        private val log = LoggerFactory.getLogger(AuthService::class.java)
    }

    @Transactional
    fun login(username: String, password: String, ip: String): UUID {
        val user = userRepository.findByUsername(username.lowercase())
        if (user == null) {
            passwordEncoder.matches(
                password,
                "\$argon2id\$v=19\$m=65536,t=2,p=1\$AcQqkv/HoFC/30x1VdPubg\$shuyKPZ7hKAc2pb8ALx6QrSr2VzyYV0Rcc7IJSjFEqs"
            )
            log.warn("Login failed — unknown username={}", username)
            interactionService.recordAuthFailed(ip)
            throw AuthException("Invalid credentials")
        }
        if (!passwordEncoder.matches(password, user.password)) {
            log.warn("Login failed — wrong password for username={}", username)
            interactionService.recordAuthFailed(ip)
            throw AuthException("Invalid credentials")
        }
        val session = sessionRepository.save(Session(user = user))
        interactionService.recordAuthSuccess(user = user.id!!, ip = ip)
        log.debug("Session created id={} for username={}", session.id, username)
        return session.id!!
    }

    @Transactional
    fun logout(sessionId: UUID, ip: String) {
        val session = sessionRepository.findById(sessionId).orElse(null)
        sessionRepository.deleteById(sessionId)
        session?.user?.id?.let { interactionService.recordAuthLogout(user = it, ip = ip) }
        log.debug("Session deleted id={}", sessionId)
    }

    @Transactional(readOnly = true)
    fun resolveUser(sessionId: UUID): User {
        return sessionRepository.findByIdAndExpiresAtAfter(sessionId, OffsetDateTime.now())?.user
            ?: throw AuthException("Invalid or expired session")
    }
}

class AuthException(message: String) : RuntimeException(message)
