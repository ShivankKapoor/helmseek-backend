package com.shivankkapoor.helmseek_backend.controller

import com.shivankkapoor.helmseek_backend.config.SecurityConfig
import com.shivankkapoor.helmseek_backend.filter.RateLimitFilter
import com.shivankkapoor.helmseek_backend.model.User
import com.shivankkapoor.helmseek_backend.repository.UserRepository
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
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.util.UUID

@WebMvcTest(
    value = [UserController::class],
    excludeFilters = [ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = [RateLimitFilter::class])]
)
@Import(SecurityConfig::class)
class UserControllerTest {

    @Autowired private lateinit var mockMvc: MockMvc
    @MockitoBean private lateinit var authService: AuthService
    @MockitoBean private lateinit var userRepository: UserRepository
    @MockitoBean private lateinit var ipService: IpService

    private val sessionId: UUID = UUID.randomUUID()
    private val testUser = User(username = "test", password = "hashed")

    private val validConfig = """
        {
            "themeMode": "dark",
            "selectedColor": "#1a73e8,#155ab6",
            "heroEnabled": true,
            "heroMode": "greeting",
            "heroClockFormat": "12h",
            "heroShowSeconds": false,
            "heroGreetingName": "Shivank",
            "weatherEnabled": false,
            "weatherZip": "",
            "weatherCorner": "top-right",
            "weatherCity": "",
            "weatherLat": 0.0,
            "weatherLng": 0.0,
            "quickLinksEnabled": false,
            "quickLinks": "[]"
        }
    """.trimIndent()

    private val validWeather = """
        {
            "cachedTemperature": 72,
            "cachedWeatherCode": 1,
            "cachedWindDirection": 180,
            "cachedWindSpeed": 12.5,
            "cachedWeatherDescription": "Partly cloudy",
            "cachedIsDay": true
        }
    """.trimIndent()

    @BeforeEach
    fun setup() {
        whenever(ipService.getClientIp(any())).thenReturn("127.0.0.1")
        whenever(authService.resolveUser(sessionId)).thenReturn(testUser)
        whenever(userRepository.save(any<User>())).thenReturn(testUser)
    }

    // ── GET /user/config ──────────────────────────────────────────────────────

    @Test
    fun `getConfig with valid session returns 200 and config`() {
        mockMvc.perform(
            get("/user/config")
                .cookie(Cookie("helmseek_session", sessionId.toString()))
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.themeMode").value("light"))
            .andExpect(jsonPath("$.quickLinks").value("[]"))
    }

    @Test
    fun `getConfig without cookie returns 401`() {
        mockMvc.perform(get("/user/config"))
            .andExpect(status().isUnauthorized)
    }

    @Test
    fun `getConfig with invalid session returns 401`() {
        val badSession = UUID.randomUUID()
        whenever(authService.resolveUser(badSession)).thenThrow(AuthException("Invalid or expired session"))

        mockMvc.perform(
            get("/user/config")
                .cookie(Cookie("helmseek_session", badSession.toString()))
        )
            .andExpect(status().isUnauthorized)
    }

    // ── POST /user/config ─────────────────────────────────────────────────────

    @Test
    fun `updateConfig with valid body returns 200`() {
        mockMvc.perform(
            post("/user/config")
                .cookie(Cookie("helmseek_session", sessionId.toString()))
                .contentType(MediaType.APPLICATION_JSON)
                .content(validConfig)
        )
            .andExpect(status().isOk)
    }

    @Test
    fun `updateConfig without auth returns 401`() {
        mockMvc.perform(
            post("/user/config")
                .contentType(MediaType.APPLICATION_JSON)
                .content(validConfig)
        )
            .andExpect(status().isUnauthorized)
    }

    @Test
    fun `updateConfig with invalid themeMode returns 400`() {
        val bad = validConfig.replace("\"dark\"", "\"invalid\"")
        mockMvc.perform(
            post("/user/config")
                .cookie(Cookie("helmseek_session", sessionId.toString()))
                .contentType(MediaType.APPLICATION_JSON)
                .content(bad)
        )
            .andExpect(status().isBadRequest)
    }

    @Test
    fun `updateConfig with invalid heroMode returns 400`() {
        val bad = validConfig.replace("\"greeting\"", "\"unknown\"")
        mockMvc.perform(
            post("/user/config")
                .cookie(Cookie("helmseek_session", sessionId.toString()))
                .contentType(MediaType.APPLICATION_JSON)
                .content(bad)
        )
            .andExpect(status().isBadRequest)
    }

    @Test
    fun `updateConfig with javascript url in quick links returns 400`() {
        val bad = validConfig.replace("\"[]\"", "\"[{\\\"label\\\":\\\"x\\\",\\\"url\\\":\\\"javascript:alert(1)\\\"}]\"")
        mockMvc.perform(
            post("/user/config")
                .cookie(Cookie("helmseek_session", sessionId.toString()))
                .contentType(MediaType.APPLICATION_JSON)
                .content(bad)
        )
            .andExpect(status().isBadRequest)
    }

    @Test
    fun `updateConfig with malformed quick links json returns 400`() {
        val bad = validConfig.replace("\"[]\"", "\"not-json\"")
        mockMvc.perform(
            post("/user/config")
                .cookie(Cookie("helmseek_session", sessionId.toString()))
                .contentType(MediaType.APPLICATION_JSON)
                .content(bad)
        )
            .andExpect(status().isBadRequest)
    }

    @Test
    fun `updateConfig with valid https quick link returns 200`() {
        val good = validConfig.replace("\"[]\"", "\"[{\\\"label\\\":\\\"Google\\\",\\\"url\\\":\\\"https://google.com\\\"}]\"")
        mockMvc.perform(
            post("/user/config")
                .cookie(Cookie("helmseek_session", sessionId.toString()))
                .contentType(MediaType.APPLICATION_JSON)
                .content(good)
        )
            .andExpect(status().isOk)
    }

    // ── POST /user/weather ────────────────────────────────────────────────────

    @Test
    fun `updateWeather with valid body returns 200`() {
        mockMvc.perform(
            post("/user/weather")
                .cookie(Cookie("helmseek_session", sessionId.toString()))
                .contentType(MediaType.APPLICATION_JSON)
                .content(validWeather)
        )
            .andExpect(status().isOk)
    }

    @Test
    fun `updateWeather without auth returns 401`() {
        mockMvc.perform(
            post("/user/weather")
                .contentType(MediaType.APPLICATION_JSON)
                .content(validWeather)
        )
            .andExpect(status().isUnauthorized)
    }

    @Test
    fun `updateWeather with temperature out of range returns 400`() {
        val bad = validWeather.replace("72", "200")
        mockMvc.perform(
            post("/user/weather")
                .cookie(Cookie("helmseek_session", sessionId.toString()))
                .contentType(MediaType.APPLICATION_JSON)
                .content(bad)
        )
            .andExpect(status().isBadRequest)
    }

    @Test
    fun `updateWeather with wind direction out of range returns 400`() {
        val bad = validWeather.replace("180", "400")
        mockMvc.perform(
            post("/user/weather")
                .cookie(Cookie("helmseek_session", sessionId.toString()))
                .contentType(MediaType.APPLICATION_JSON)
                .content(bad)
        )
            .andExpect(status().isBadRequest)
    }
}
