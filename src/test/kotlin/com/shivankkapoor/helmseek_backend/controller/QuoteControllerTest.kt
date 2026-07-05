package com.shivankkapoor.helmseek_backend.controller

import com.shivankkapoor.helmseek_backend.config.SecurityConfig
import com.shivankkapoor.helmseek_backend.dto.QuoteDTO
import com.shivankkapoor.helmseek_backend.filter.RateLimitFilter
import com.shivankkapoor.helmseek_backend.model.User
import com.shivankkapoor.helmseek_backend.service.AuthException
import com.shivankkapoor.helmseek_backend.service.AuthService
import com.shivankkapoor.helmseek_backend.service.IpService
import com.shivankkapoor.helmseek_backend.service.QuoteService
import com.shivankkapoor.helmseek_backend.service.UserService
import jakarta.servlet.http.Cookie
import jakarta.servlet.http.HttpServletRequest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.FilterType
import org.springframework.context.annotation.Import
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.util.UUID

@WebMvcTest(
    value = [QuoteController::class],
    excludeFilters = [ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = [RateLimitFilter::class])]
)
@Import(SecurityConfig::class)
class QuoteControllerTest {

    @Autowired private lateinit var mockMvc: MockMvc
    @MockitoBean private lateinit var ipService: IpService
    @MockitoBean private lateinit var authService: AuthService
    @MockitoBean private lateinit var quoteService: QuoteService
    @MockitoBean private lateinit var userService: UserService

    private val sessionId: UUID = UUID.randomUUID()
    private val testUser = User(id = UUID.randomUUID(), username = "testuser", password = "hashed")

    @BeforeEach
    fun setup() {
        whenever(ipService.getClientIp(any())).thenReturn("127.0.0.1")
        whenever(authService.resolveUser(sessionId)).thenReturn(testUser)
        whenever(authService.extractSessionId(any())).thenAnswer { invocation ->
            val req = invocation.getArgument<HttpServletRequest>(0)
            req.cookies
                ?.find { it.name == "helmseek_session" }
                ?.value
                ?.let { runCatching { UUID.fromString(it) }.getOrNull() }
        }
    }

    // ── GET /quote ─────────────────────────────────────────────────────────────

    @Test
    fun `getQuote with valid session returns 200 and quote`() {
        whenever(quoteService.getQuote()).thenReturn(QuoteDTO("Stay hungry, stay foolish.", "Steve Jobs"))

        mockMvc.perform(
            get("/quote")
                .cookie(Cookie("helmseek_session", sessionId.toString()))
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.quote").value("Stay hungry, stay foolish."))
            .andExpect(jsonPath("$.author").value("Steve Jobs"))
    }

    @Test
    fun `getQuote without cookie returns 401`() {
        mockMvc.perform(get("/quote"))
            .andExpect(status().isUnauthorized)
    }

    @Test
    fun `getQuote with invalid or expired session returns 401`() {
        val badSession = UUID.randomUUID()
        whenever(authService.resolveUser(badSession)).thenThrow(AuthException("Invalid or expired session"))

        mockMvc.perform(
            get("/quote")
                .cookie(Cookie("helmseek_session", badSession.toString()))
        )
            .andExpect(status().isUnauthorized)
    }

    // ── POST /quote/hideQuote ────────────────────────────────────────────────────

    @Test
    fun `hideQuote with valid session returns 200`() {
        mockMvc.perform(
            post("/quote/hideQuote")
                .cookie(Cookie("helmseek_session", sessionId.toString()))
        )
            .andExpect(status().isOk)

        verify(userService).hideQuote(sessionId, "127.0.0.1")
    }

    @Test
    fun `hideQuote without cookie returns 401`() {
        mockMvc.perform(post("/quote/hideQuote"))
            .andExpect(status().isUnauthorized)
    }

    @Test
    fun `hideQuote with invalid or expired session returns 401`() {
        val badSession = UUID.randomUUID()
        whenever(userService.hideQuote(badSession, "127.0.0.1")).thenThrow(AuthException("Invalid or expired session"))

        mockMvc.perform(
            post("/quote/hideQuote")
                .cookie(Cookie("helmseek_session", badSession.toString()))
        )
            .andExpect(status().isUnauthorized)
    }
}
