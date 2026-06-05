package com.shivankkapoor.helmseek_backend.controller

import com.shivankkapoor.helmseek_backend.config.SecurityConfig
import com.shivankkapoor.helmseek_backend.filter.RateLimitFilter
import com.shivankkapoor.helmseek_backend.service.AuthException
import com.shivankkapoor.helmseek_backend.service.AuthService
import com.shivankkapoor.helmseek_backend.service.IpService
import jakarta.servlet.http.Cookie
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.FilterType
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.util.UUID

@WebMvcTest(
    value = [AuthController::class],
    excludeFilters = [ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = [RateLimitFilter::class])]
)
@Import(SecurityConfig::class)
class AuthControllerTest {

    @Autowired private lateinit var mockMvc: MockMvc
    @MockitoBean private lateinit var authService: AuthService
    @MockitoBean private lateinit var ipService: IpService

    @BeforeEach
    fun setup() {
        whenever(ipService.getClientIp(any())).thenReturn("127.0.0.1")
    }

    @Test
    fun `login with valid credentials returns 200 and sets session cookie`() {
        val sessionId = UUID.randomUUID()
        whenever(authService.login("test", "test123")).thenReturn(sessionId)

        mockMvc.perform(
            post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"username":"test","password":"test123"}""")
        )
            .andExpect(status().isOk)
            .andExpect(cookie().exists("helmseek_session"))
            .andExpect(cookie().httpOnly("helmseek_session", true))
            .andExpect(cookie().maxAge("helmseek_session", 30 * 24 * 60 * 60))
    }

    @Test
    fun `login with wrong credentials returns 401`() {
        whenever(authService.login(any(), any())).thenThrow(AuthException("Invalid credentials"))

        mockMvc.perform(
            post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"username":"test","password":"wrong"}""")
        )
            .andExpect(status().isUnauthorized)
    }

    @Test
    fun `login with blank username returns 400`() {
        mockMvc.perform(
            post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"username":"","password":"test123"}""")
        )
            .andExpect(status().isBadRequest)
    }

    @Test
    fun `login with blank password returns 400`() {
        mockMvc.perform(
            post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"username":"test","password":""}""")
        )
            .andExpect(status().isBadRequest)
    }

    @Test
    fun `login with username over max length returns 400`() {
        val longUsername = "a".repeat(33)
        mockMvc.perform(
            post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"username":"$longUsername","password":"test123"}""")
        )
            .andExpect(status().isBadRequest)
    }

    @Test
    fun `logout with valid cookie returns 200 and clears cookie`() {
        mockMvc.perform(
            post("/auth/logout")
                .cookie(Cookie("helmseek_session", UUID.randomUUID().toString()))
        )
            .andExpect(status().isOk)
            .andExpect(cookie().maxAge("helmseek_session", 0))
    }

    @Test
    fun `logout without cookie returns 200`() {
        mockMvc.perform(post("/auth/logout"))
            .andExpect(status().isOk)
    }
}
