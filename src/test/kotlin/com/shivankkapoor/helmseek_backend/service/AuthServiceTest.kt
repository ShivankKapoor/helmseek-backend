package com.shivankkapoor.helmseek_backend.service

import com.shivankkapoor.helmseek_backend.model.User
import com.shivankkapoor.helmseek_backend.repository.UserRepository
import jakarta.servlet.http.Cookie
import jakarta.servlet.http.HttpServletRequest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.*
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.test.web.client.MockRestServiceServer
import org.springframework.test.web.client.match.MockRestRequestMatchers.*
import org.springframework.test.web.client.response.MockRestResponseCreators.*
import org.springframework.web.client.RestClient
import java.util.Optional
import java.util.UUID

class AuthServiceTest {

    private val userRepository = mock<UserRepository>()
    private val interactionService = mock<InteractionService>()

    private val builder = RestClient.builder()
        .baseUrl("http://aldrop.test")
        .defaultHeader("Authorization", "Bearer test-key")
    private val server: MockRestServiceServer = MockRestServiceServer.bindTo(builder).build()
    private val authService = AuthService(userRepository, interactionService, builder.build())

    private val userId = UUID.randomUUID()
    private val testUser = User(id = userId, username = "testuser")
    private val ip = "127.0.0.1"

    /** Tokens must be unique per test — AuthService caches token -> userId for 45s. */
    private fun token() = UUID.randomUUID().toString()

    private fun expectValidate(userId: UUID) {
        server.expect(requestTo("http://aldrop.test/auth/validate"))
            .andExpect(method(HttpMethod.POST))
            .andExpect(header("Authorization", "Bearer test-key"))
            .andRespond(withSuccess("""{"userId":"$userId","username":"testuser"}""", MediaType.APPLICATION_JSON))
    }

    private fun expectValidateRejected() {
        server.expect(requestTo("http://aldrop.test/auth/validate"))
            .andRespond(withStatus(HttpStatus.UNAUTHORIZED))
    }

    private fun expectLogin(token: String) {
        server.expect(requestTo("http://aldrop.test/auth/login"))
            .andExpect(method(HttpMethod.POST))
            .andRespond(withSuccess("""{"token":"$token","totpToken":null}""", MediaType.APPLICATION_JSON))
    }

    // ── login ──────────────────────────────────────────────────────────────────

    @Test
    fun `login with valid credentials returns aldrop token`() {
        val token = token()
        expectLogin(token)
        expectValidate(userId)
        whenever(userRepository.findById(userId)).thenReturn(Optional.of(testUser))

        val result = authService.login("testuser", "password", ip)

        assert(result == token)
        server.verify()
    }

    @Test
    fun `login normalises username to lowercase`() {
        val token = token()
        server.expect(requestTo("http://aldrop.test/auth/login"))
            .andExpect(jsonPath("$.username").value("testuser"))
            .andRespond(withSuccess("""{"token":"$token"}""", MediaType.APPLICATION_JSON))
        expectValidate(userId)
        whenever(userRepository.findById(userId)).thenReturn(Optional.of(testUser))

        authService.login("TestUser", "password", ip)

        server.verify()
    }

    @Test
    fun `login sends the client ip to aldrop`() {
        val token = token()
        server.expect(requestTo("http://aldrop.test/auth/login"))
            .andExpect(jsonPath("$.ipAddress").value(ip))
            .andRespond(withSuccess("""{"token":"$token"}""", MediaType.APPLICATION_JSON))
        expectValidate(userId)
        whenever(userRepository.findById(userId)).thenReturn(Optional.of(testUser))

        authService.login("testuser", "password", ip)

        server.verify()
    }

    @Test
    fun `login omits a non-ip address rather than sending one aldrop would reject`() {
        val token = token()
        server.expect(requestTo("http://aldrop.test/auth/login"))
            .andExpect(jsonPath("$.ipAddress").doesNotExist())
            .andRespond(withSuccess("""{"token":"$token"}""", MediaType.APPLICATION_JSON))
        expectValidate(userId)
        whenever(userRepository.findById(userId)).thenReturn(Optional.of(testUser))

        authService.login("testuser", "password", "unknown")

        server.verify()
    }

    @Test
    fun `login with bad credentials throws AuthException`() {
        server.expect(requestTo("http://aldrop.test/auth/login"))
            .andRespond(withStatus(HttpStatus.UNAUTHORIZED))

        assertThrows<AuthException> { authService.login("testuser", "wrong", ip) }
    }

    @Test
    fun `login with bad credentials records auth failed`() {
        server.expect(requestTo("http://aldrop.test/auth/login"))
            .andRespond(withStatus(HttpStatus.UNAUTHORIZED))

        runCatching { authService.login("testuser", "wrong", ip) }

        verify(interactionService).recordAuthFailed(ip)
    }

    @Test
    fun `login when aldrop is unreachable throws AuthException`() {
        server.expect(requestTo("http://aldrop.test/auth/login"))
            .andRespond(withServerError())

        assertThrows<AuthException> { authService.login("testuser", "password", ip) }
    }

    @Test
    fun `login records auth success with the resolved user id`() {
        val token = token()
        expectLogin(token)
        expectValidate(userId)
        whenever(userRepository.findById(userId)).thenReturn(Optional.of(testUser))

        authService.login("testuser", "password", ip)

        verify(interactionService).recordAuthSuccess(user = userId, ip = ip)
    }

    // ── logout ─────────────────────────────────────────────────────────────────

    @Test
    fun `logout posts the token to aldrop`() {
        val token = token()
        expectValidate(userId)
        server.expect(requestTo("http://aldrop.test/auth/logout"))
            .andExpect(method(HttpMethod.POST))
            .andExpect(jsonPath("$.token").value(token))
            .andRespond(withSuccess())

        authService.logout(token, ip)

        server.verify()
    }

    @Test
    fun `logout records auth logout for the session owner`() {
        val token = token()
        expectValidate(userId)
        server.expect(requestTo("http://aldrop.test/auth/logout")).andRespond(withSuccess())

        authService.logout(token, ip)

        verify(interactionService).recordAuthLogout(user = userId, ip = ip)
    }

    @Test
    fun `logout of an already invalid session still clears it and records nothing`() {
        val token = token()
        expectValidateRejected()
        server.expect(requestTo("http://aldrop.test/auth/logout")).andRespond(withSuccess())

        authService.logout(token, ip)

        verify(interactionService, never()).recordAuthLogout(any(), any())
        server.verify()
    }

    // ── resolveUser ────────────────────────────────────────────────────────────

    @Test
    fun `resolveUser with valid token returns user`() {
        val token = token()
        expectValidate(userId)
        whenever(userRepository.findById(userId)).thenReturn(Optional.of(testUser))

        assert(authService.resolveUser(token) == testUser)
    }

    @Test
    fun `resolveUser with rejected token throws AuthException`() {
        expectValidateRejected()

        assertThrows<AuthException> { authService.resolveUser(token()) }
    }

    @Test
    fun `resolveUser provisions a helmseek row on a users first login`() {
        expectValidate(userId)
        whenever(userRepository.findById(userId)).thenReturn(Optional.empty())
        whenever(userRepository.save(any<User>())).thenAnswer { it.arguments[0] }

        val result = authService.resolveUser(token())

        val saved = argumentCaptor<User>()
        verify(userRepository).save(saved.capture())
        assert(saved.firstValue.id == userId)
        assert(saved.firstValue.username == "testuser")
        assert(result.id == userId)
    }

    @Test
    fun `provisioned user gets the default profile`() {
        expectValidate(userId)
        whenever(userRepository.findById(userId)).thenReturn(Optional.empty())
        whenever(userRepository.save(any<User>())).thenAnswer { it.arguments[0] }

        val result = authService.resolveUser(token())

        assert(result.themeMode == "light")
        assert(result.quickLinks == "[]")
        assert(!result.weatherEnabled)
    }

    @Test
    fun `resolveUser does not provision when the helmseek row already exists`() {
        expectValidate(userId)
        whenever(userRepository.findById(userId)).thenReturn(Optional.of(testUser))

        authService.resolveUser(token())

        verify(userRepository, never()).save(any<User>())
    }

    @Test
    fun `a concurrent provision that loses the race falls back to the winners row`() {
        expectValidate(userId)
        whenever(userRepository.findById(userId))
            .thenReturn(Optional.empty())
            .thenReturn(Optional.of(testUser))
        whenever(userRepository.save(any<User>()))
            .thenThrow(DataIntegrityViolationException("duplicate key"))

        assert(authService.resolveUser(token()) == testUser)
    }

    @Test
    fun `provisioning that fails for any other reason throws AuthException`() {
        expectValidate(userId)
        whenever(userRepository.findById(userId)).thenReturn(Optional.empty())
        whenever(userRepository.save(any<User>()))
            .thenThrow(DataIntegrityViolationException("username already taken"))

        assertThrows<AuthException> { authService.resolveUser(token()) }
    }

    @Test
    fun `a validate response with no username is rejected rather than provisioning a bad row`() {
        server.expect(requestTo("http://aldrop.test/auth/validate"))
            .andRespond(withSuccess("""{"userId":"$userId"}""", MediaType.APPLICATION_JSON))

        assertThrows<AuthException> { authService.resolveUser(token()) }
        verify(userRepository, never()).save(any<User>())
    }

    @Test
    fun `resolveUser caches the token so repeat calls do not re-hit aldrop`() {
        val token = token()
        expectValidate(userId)   // exactly one validate expected
        whenever(userRepository.findById(userId)).thenReturn(Optional.of(testUser))

        repeat(5) { authService.resolveUser(token) }

        server.verify()
    }

    // ── extractSessionId ───────────────────────────────────────────────────────

    private fun requestWithCookies(vararg cookies: Cookie): HttpServletRequest {
        val request = mock<HttpServletRequest>()
        whenever(request.cookies).thenReturn(cookies)
        return request
    }

    @Test
    fun `extractSessionId returns the opaque cookie value verbatim`() {
        val request = requestWithCookies(Cookie("helmseek_session", "aldrop-opaque-token"))

        assert(authService.extractSessionId(request) == "aldrop-opaque-token")
    }

    @Test
    fun `extractSessionId with no cookies returns null`() {
        val request = mock<HttpServletRequest>()
        whenever(request.cookies).thenReturn(null)

        assert(authService.extractSessionId(request) == null)
    }

    @Test
    fun `extractSessionId with unrelated cookie returns null`() {
        val request = requestWithCookies(Cookie("some_other_cookie", "value"))

        assert(authService.extractSessionId(request) == null)
    }

    @Test
    fun `extractSessionId with blank session cookie returns null`() {
        val request = requestWithCookies(Cookie("helmseek_session", ""))

        assert(authService.extractSessionId(request) == null)
    }
}
