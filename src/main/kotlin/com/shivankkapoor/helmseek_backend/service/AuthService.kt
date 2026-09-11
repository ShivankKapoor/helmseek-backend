package com.shivankkapoor.helmseek_backend.service

import com.github.benmanes.caffeine.cache.Caffeine
import com.shivankkapoor.helmseek_backend.controller.AuthController.Companion.COOKIE_NAME
import com.shivankkapoor.helmseek_backend.dto.aldrop.AldropLoginRequest
import com.shivankkapoor.helmseek_backend.dto.aldrop.AldropLoginResponse
import com.shivankkapoor.helmseek_backend.dto.aldrop.AldropLogoutRequest
import com.shivankkapoor.helmseek_backend.dto.aldrop.AldropValidateRequest
import com.shivankkapoor.helmseek_backend.dto.aldrop.AldropValidateResponse
import com.shivankkapoor.helmseek_backend.model.User
import com.shivankkapoor.helmseek_backend.repository.UserRepository
import jakarta.servlet.http.HttpServletRequest
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.client.RestClient
import org.springframework.web.client.RestClientException
import java.time.Duration
import java.util.UUID

@Service
class AuthService(
    private val userRepository: UserRepository,
    private val interactionService: InteractionService,
    @Qualifier("aldropRestClient") private val aldrop: RestClient
) {

    companion object {
        private val log = LoggerFactory.getLogger(AuthService::class.java)

        /**
         * UserService calls resolveUser several times per request. Without this every one of those
         * is a network hop to aldrop. Kept short so a logout elsewhere takes effect quickly.
         */
        private val TOKEN_CACHE_TTL: Duration = Duration.ofSeconds(45)
        private const val TOKEN_CACHE_MAX = 1_000L

        private val IPV4 = Regex("""^\d{1,3}(\.\d{1,3}){3}$""")
        private val IPV6 = Regex("""^[0-9a-fA-F:]+$""")
    }

    /** What aldrop tells us about a token: who owns it, and under what name. */
    private data class AldropSession(val userId: UUID, val username: String)

    private val sessionCache = Caffeine.newBuilder()
        .expireAfterWrite(TOKEN_CACHE_TTL)
        .maximumSize(TOKEN_CACHE_MAX)
        .build<String, AldropSession>()

    fun login(username: String, password: String, ip: String): String {
        val response = try {
            aldrop.post()
                .uri("/auth/login")
                .body(AldropLoginRequest(username.lowercase(), password, sanitizeIp(ip), null))
                .retrieve()
                .body(AldropLoginResponse::class.java)
        } catch (e: RestClientException) {
            log.warn("Login rejected by aldrop for username={}: {}", username, e.message)
            interactionService.recordAuthFailed(ip)
            throw AuthException("Invalid credentials")
        }

        val token = response?.token
        if (token == null) {
            log.error("Aldrop login returned no token for username={}", username)
            interactionService.recordAuthFailed(ip)
            throw AuthException("Invalid credentials")
        }

        val user = resolveUser(token, ip)
        interactionService.recordAuthSuccess(user = user.id!!, ip = ip)
        log.debug("Session created for username={}", username)
        return token
    }

    fun logout(token: String, ip: String) {
        val userId = runCatching { lookupSession(token, ip).userId }.getOrNull()
        sessionCache.invalidate(token)
        try {
            aldrop.post()
                .uri("/auth/logout")
                .body(AldropLogoutRequest(token, sanitizeIp(ip)))
                .retrieve()
                .toBodilessEntity()
        } catch (e: RestClientException) {
            log.warn("Aldrop logout failed: {}", e.message)
            throw AuthException("Logout failed")
        }
        userId?.let { interactionService.recordAuthLogout(user = it, ip = ip) }
        log.debug("Session deleted")
    }

    /**
     * Aldrop owns account creation, so a user can exist there before helmseek has ever seen them.
     * The first time such a user logs in we provision their helmseek row on the spot, keyed by the
     * id aldrop already issued. Every profile column has a default, so the new row is immediately
     * usable.
     */
    @Transactional
    fun resolveUser(token: String, ip: String? = null): User {
        val session = lookupSession(token, ip)
        return userRepository.findById(session.userId).orElseGet { provision(session) }
    }

    private fun provision(session: AldropSession): User {
        log.info("Provisioning helmseek user on first login, userId={}", session.userId)
        return try {
            userRepository.save(User(id = session.userId, username = session.username))
        } catch (e: DataIntegrityViolationException) {
            // Either a concurrent request won the race, or the username collides with a different
            // helmseek row. Re-read decides which; only the first is recoverable.
            userRepository.findById(session.userId).orElseThrow {
                log.error("Could not provision helmseek user userId={}", session.userId, e)
                AuthException("Invalid or expired session")
            }
        }
    }

    private fun lookupSession(token: String, ip: String? = null): AldropSession {
        sessionCache.getIfPresent(token)?.let {
            log.info("Session cache hit for userId={}", it.userId)
            return it
        }
        log.info("Session cache miss, validating token against aldrop")

        val response = try {
            aldrop.post()
                .uri("/auth/validate")
                .body(AldropValidateRequest(token, sanitizeIp(ip), null))
                .retrieve()
                .body(AldropValidateResponse::class.java)
        } catch (e: RestClientException) {
            log.debug("Aldrop rejected session: {}", e.message)
            throw AuthException("Invalid or expired session")
        }

        val userId = response?.userId ?: throw AuthException("Invalid or expired session")
        val username = response.username
        if (username == null) {
            // An aldrop too old to return the username; provisioning could not fill a NOT NULL column.
            log.error("Aldrop validate returned no username for userId={}", userId)
            throw AuthException("Invalid or expired session")
        }
        return AldropSession(userId, username).also { sessionCache.put(token, it) }
    }

    /** Aldrop validates ipAddress against an IPv4/IPv6 pattern and 400s on anything else. */
    private fun sanitizeIp(ip: String?): String? =
        ip?.takeIf { IPV4.matches(it) || (it.contains(':') && IPV6.matches(it)) }

    fun extractSessionId(request: HttpServletRequest): String? =
        request.cookies
            ?.find { it.name == COOKIE_NAME }
            ?.value
            ?.takeIf { it.isNotBlank() }
}

class AuthException(message: String) : RuntimeException(message)
