package com.shivankkapoor.helmseek_backend.service

import com.shivankkapoor.helmseek_backend.model.Session
import com.shivankkapoor.helmseek_backend.model.User
import com.shivankkapoor.helmseek_backend.repository.SessionRepository
import com.shivankkapoor.helmseek_backend.repository.UserRepository
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.*
import org.springframework.security.crypto.password.PasswordEncoder
import java.time.OffsetDateTime
import java.util.Optional
import java.util.UUID

class AuthServiceTest {

    private val userRepository = mock<UserRepository>()
    private val sessionRepository = mock<SessionRepository>()
    private val passwordEncoder = mock<PasswordEncoder>()
    private val interactionService = mock<InteractionService>()
    private val authService = AuthService(userRepository, sessionRepository, passwordEncoder, interactionService)

    private val userId = UUID.randomUUID()
    private val testUser = User(id = userId, username = "testuser", password = "hashed")
    private val ip = "127.0.0.1"

    // ── login ──────────────────────────────────────────────────────────────────

    @Test
    fun `login with valid credentials returns session id`() {
        val session = Session(id = UUID.randomUUID(), user = testUser)
        whenever(userRepository.findByUsername("testuser")).thenReturn(testUser)
        whenever(passwordEncoder.matches("password", "hashed")).thenReturn(true)
        whenever(sessionRepository.save(any<Session>())).thenReturn(session)

        val result = authService.login("testuser", "password", ip)

        assert(result == session.id)
        verify(sessionRepository).save(any<Session>())
    }

    @Test
    fun `login normalises username to lowercase`() {
        whenever(userRepository.findByUsername("testuser")).thenReturn(testUser)
        whenever(passwordEncoder.matches(any(), any())).thenReturn(true)
        whenever(sessionRepository.save(any<Session>())).thenReturn(Session(id = UUID.randomUUID(), user = testUser))

        authService.login("TestUser", "password", ip)

        verify(userRepository).findByUsername("testuser")
    }

    @Test
    fun `login with unknown username throws AuthException`() {
        whenever(userRepository.findByUsername(any())).thenReturn(null)

        assertThrows<AuthException> { authService.login("unknown", "password", ip) }
    }

    @Test
    fun `login with unknown username still runs dummy hash to prevent timing oracle`() {
        whenever(userRepository.findByUsername(any())).thenReturn(null)

        runCatching { authService.login("unknown", "password", ip) }

        verify(passwordEncoder).matches(eq("password"), any())
    }

    @Test
    fun `login with wrong password throws AuthException`() {
        whenever(userRepository.findByUsername("testuser")).thenReturn(testUser)
        whenever(passwordEncoder.matches("wrong", "hashed")).thenReturn(false)

        assertThrows<AuthException> { authService.login("testuser", "wrong", ip) }
    }

    @Test
    fun `login with wrong password does not create session`() {
        whenever(userRepository.findByUsername("testuser")).thenReturn(testUser)
        whenever(passwordEncoder.matches(any(), any())).thenReturn(false)

        runCatching { authService.login("testuser", "wrong", ip) }

        verify(sessionRepository, never()).save(any<Session>())
    }

    @Test
    fun `login with unknown username records auth failed`() {
        whenever(userRepository.findByUsername(any())).thenReturn(null)

        runCatching { authService.login("unknown", "password", ip) }

        verify(interactionService).recordAuthFailed(ip)
    }

    @Test
    fun `login with wrong password records auth failed`() {
        whenever(userRepository.findByUsername("testuser")).thenReturn(testUser)
        whenever(passwordEncoder.matches(any(), any())).thenReturn(false)

        runCatching { authService.login("testuser", "wrong", ip) }

        verify(interactionService).recordAuthFailed(ip)
    }

    @Test
    fun `login with valid credentials records auth success`() {
        val session = Session(id = UUID.randomUUID(), user = testUser)
        whenever(userRepository.findByUsername("testuser")).thenReturn(testUser)
        whenever(passwordEncoder.matches("password", "hashed")).thenReturn(true)
        whenever(sessionRepository.save(any<Session>())).thenReturn(session)

        authService.login("testuser", "password", ip)

        verify(interactionService).recordAuthSuccess(user = userId, ip = ip)
    }

    // ── logout ─────────────────────────────────────────────────────────────────

    @Test
    fun `logout deletes session by id`() {
        val sessionId = UUID.randomUUID()
        whenever(sessionRepository.findById(sessionId)).thenReturn(Optional.empty())

        authService.logout(sessionId, ip)

        verify(sessionRepository).deleteById(sessionId)
    }

    @Test
    fun `logout records auth logout when session exists`() {
        val sessionId = UUID.randomUUID()
        val session = Session(id = sessionId, user = testUser)
        whenever(sessionRepository.findById(sessionId)).thenReturn(Optional.of(session))

        authService.logout(sessionId, ip)

        verify(interactionService).recordAuthLogout(user = userId, ip = ip)
    }

    @Test
    fun `logout does not record interaction when session not found`() {
        val sessionId = UUID.randomUUID()
        whenever(sessionRepository.findById(sessionId)).thenReturn(Optional.empty())

        authService.logout(sessionId, ip)

        verify(interactionService, never()).recordAuthLogout(any(), any())
    }

    // ── resolveUser ────────────────────────────────────────────────────────────

    @Test
    fun `resolveUser with valid session returns user`() {
        val sessionId = UUID.randomUUID()
        val session = Session(id = sessionId, user = testUser)
        whenever(sessionRepository.findByIdAndExpiresAtAfter(eq(sessionId), any())).thenReturn(session)

        val result = authService.resolveUser(sessionId)

        assert(result == testUser)
    }

    @Test
    fun `resolveUser with expired or missing session throws AuthException`() {
        val sessionId = UUID.randomUUID()
        whenever(sessionRepository.findByIdAndExpiresAtAfter(eq(sessionId), any())).thenReturn(null)

        assertThrows<AuthException> { authService.resolveUser(sessionId) }
    }

    @Test
    fun `resolveUser passes current time to expiry check`() {
        val sessionId = UUID.randomUUID()
        whenever(sessionRepository.findByIdAndExpiresAtAfter(eq(sessionId), any())).thenReturn(null)

        val before = OffsetDateTime.now()
        runCatching { authService.resolveUser(sessionId) }
        val after = OffsetDateTime.now()

        val captor = argumentCaptor<OffsetDateTime>()
        verify(sessionRepository).findByIdAndExpiresAtAfter(eq(sessionId), captor.capture())
        val used = captor.firstValue
        assert(!used.isBefore(before) && !used.isAfter(after))
    }
}
