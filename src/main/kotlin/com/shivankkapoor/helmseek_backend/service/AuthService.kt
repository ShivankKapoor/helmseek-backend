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

    private val userIdCache = Caffeine.newBuilder()
        .expireAfterWrite(TOKEN_CACHE_TTL)
        .maximumSize(TOKEN_CACHE_MAX)
        .build<String, UUID>()

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

        val user = resolveUser(token)
        interactionService.recordAuthSuccess(user = user.id!!, ip = ip)
        log.debug("Session created for username={}", username)
        return token
    }

    fun logout(token: String, ip: String) {
        val userId = runCatching { lookupUserId(token) }.getOrNull()
        userIdCache.invalidate(token)
        try {
            aldrop.post()
                .uri("/auth/logout")
                .body(AldropLogoutRequest(token))
                .retrieve()
                .toBodilessEntity()
        } catch (e: RestClientException) {
            log.warn("Aldrop logout failed: {}", e.message)
            throw AuthException("Logout failed")
        }
        userId?.let { interactionService.recordAuthLogout(user = it, ip = ip) }
        log.debug("Session deleted")
    }

    @Transactional(readOnly = true)
    fun resolveUser(token: String): User {
        val userId = lookupUserId(token)
        return userRepository.findById(userId).orElseThrow {
            // Present in aldrop but not helmseek — the two user tables have drifted.
            log.error("Aldrop validated userId={} but no matching helmseek user row exists", userId)
            AuthException("Invalid or expired session")
        }
    }

    private fun lookupUserId(token: String): UUID {
        userIdCache.getIfPresent(token)?.let { return it }

        val response = try {
            aldrop.post()
                .uri("/auth/validate")
                .body(AldropValidateRequest(token, null, null))
                .retrieve()
                .body(AldropValidateResponse::class.java)
        } catch (e: RestClientException) {
            log.debug("Aldrop rejected session: {}", e.message)
            throw AuthException("Invalid or expired session")
        }

        val userId = response?.userId ?: throw AuthException("Invalid or expired session")
        userIdCache.put(token, userId)
        return userId
    }

    /** Aldrop validates ipAddress against an IPv4/IPv6 pattern and 400s on anything else. */
    private fun sanitizeIp(ip: String): String? =
        ip.takeIf { IPV4.matches(it) || (it.contains(':') && IPV6.matches(it)) }

    fun extractSessionId(request: HttpServletRequest): String? =
        request.cookies
            ?.find { it.name == COOKIE_NAME }
            ?.value
            ?.takeIf { it.isNotBlank() }
}

class AuthException(message: String) : RuntimeException(message)
