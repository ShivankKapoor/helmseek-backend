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
import java.util.UUID

class AuthServiceTest {

    private val userRepository = mock<UserRepository>()
    private val sessionRepository = mock<SessionRepository>()
    private val passwordEncoder = mock<PasswordEncoder>()
    private val authService = AuthService(userRepository, sessionRepository, passwordEncoder)

    private val testUser = User(
        id = UUID.randomUUID(),
        username = "testuser",
        password = "hashed"
    )

    // ── login ──────────────────────────────────────────────────────────────────

    @Test
    fun `login with valid credentials returns session id`() {
        val session = Session(id = UUID.randomUUID(), user = testUser)
        whenever(userRepository.findByUsername("testuser")).thenReturn(testUser)
        whenever(passwordEncoder.matches("password", "hashed")).thenReturn(true)
        whenever(sessionRepository.save(any<Session>())).thenReturn(session)

        val result = authService.login("testuser", "password")

        assert(result == session.id)
        verify(sessionRepository).save(any<Session>())
    }

    @Test
    fun `login normalises username to lowercase`() {
        whenever(userRepository.findByUsername("testuser")).thenReturn(testUser)
        whenever(passwordEncoder.matches(any(), any())).thenReturn(true)
        whenever(sessionRepository.save(any<Session>())).thenReturn(Session(id = UUID.randomUUID(), user = testUser))

        authService.login("TestUser", "password")

        verify(userRepository).findByUsername("testuser")
    }

    @Test
    fun `login with unknown username throws AuthException`() {
        whenever(userRepository.findByUsername(any())).thenReturn(null)

        assertThrows<AuthException> { authService.login("unknown", "password") }
    }

    @Test
    fun `login with unknown username still runs dummy hash to prevent timing oracle`() {
        whenever(userRepository.findByUsername(any())).thenReturn(null)

        runCatching { authService.login("unknown", "password") }

        verify(passwordEncoder).matches(eq("password"), any())
    }

    @Test
    fun `login with wrong password throws AuthException`() {
        whenever(userRepository.findByUsername("testuser")).thenReturn(testUser)
        whenever(passwordEncoder.matches("wrong", "hashed")).thenReturn(false)

        assertThrows<AuthException> { authService.login("testuser", "wrong") }
    }

    @Test
    fun `login with wrong password does not create session`() {
        whenever(userRepository.findByUsername("testuser")).thenReturn(testUser)
        whenever(passwordEncoder.matches(any(), any())).thenReturn(false)

        runCatching { authService.login("testuser", "wrong") }

        verify(sessionRepository, never()).save(any<Session>())
    }

    // ── logout ─────────────────────────────────────────────────────────────────

    @Test
    fun `logout deletes session by id`() {
        val sessionId = UUID.randomUUID()

        authService.logout(sessionId)

        verify(sessionRepository).deleteById(sessionId)
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
