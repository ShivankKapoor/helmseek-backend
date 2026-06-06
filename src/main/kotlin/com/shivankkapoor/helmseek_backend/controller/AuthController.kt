package com.shivankkapoor.helmseek_backend.controller

import com.shivankkapoor.helmseek_backend.dto.request.LoginRequestDTO
import com.shivankkapoor.helmseek_backend.service.AuthException
import com.shivankkapoor.helmseek_backend.service.AuthService
import com.shivankkapoor.helmseek_backend.service.IpService
import jakarta.servlet.http.HttpServletRequest
import jakarta.validation.Valid
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseCookie
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.time.Duration
import java.util.UUID

@RestController
@RequestMapping("/auth")
class AuthController(
    private val authService: AuthService,
    private val ipService: IpService,
    @Value("\${app.prod}") private val isProd: Boolean
) {

    companion object {
        const val COOKIE_NAME = "helmseek_session"
        private val log = LoggerFactory.getLogger(AuthController::class.java)
        val COOKIE_MAX_AGE: Duration = Duration.ofDays(30)
    }

    @PostMapping("/login")
    fun login(@Valid @RequestBody body: LoginRequestDTO, request: HttpServletRequest): ResponseEntity<Void> {
        val ip = ipService.getClientIp(request)
        val sessionId = try {
            authService.login(body.username, body.password, ip)
        } catch (e: AuthException) {
            log.warn("Failed login attempt for username={} ip={}", body.username, ip)
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build()
        }
        log.info("Login successful for username={} ip={}", body.username, ip)
        val cookie = buildCookie(sessionId.toString(), COOKIE_MAX_AGE)
        return ResponseEntity.ok()
            .header(HttpHeaders.SET_COOKIE, cookie.toString())
            .build()
    }

    @PostMapping("/logout")
    fun logout(request: HttpServletRequest): ResponseEntity<Void> {
        val ip = ipService.getClientIp(request)
        val sessionCookie = request.cookies?.find { it.name == COOKIE_NAME }
        if (sessionCookie != null) {
            runCatching { authService.logout(UUID.fromString(sessionCookie.value), ip) }
                .onSuccess { log.info("Logout successful for session={} ip={}", sessionCookie.value, ip) }
                .onFailure { log.warn("Logout failed for session={} ip={}", sessionCookie.value, ip) }
        } else {
            log.warn("Logout attempted with no session cookie ip={}", ip)
        }
        val expiredCookie = buildCookie("", Duration.ZERO)
        return ResponseEntity.ok()
            .header(HttpHeaders.SET_COOKIE, expiredCookie.toString())
            .build()
    }

    private fun buildCookie(value: String, maxAge: Duration): ResponseCookie =
        ResponseCookie.from(COOKIE_NAME, value)
            .httpOnly(true)
            .secure(isProd)
            .sameSite("Strict")
            .path("/")
            .maxAge(maxAge)
            .build()
}
