package com.shivankkapoor.helmseek_backend.service

import com.shivankkapoor.helmseek_backend.dto.UserConfigDTO
import com.shivankkapoor.helmseek_backend.dto.request.WeatherCacheRequestDTO
import com.shivankkapoor.helmseek_backend.model.User
import com.shivankkapoor.helmseek_backend.repository.UserRepository
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.*
import tools.jackson.databind.json.JsonMapper
import tools.jackson.module.kotlin.KotlinModule
import java.util.UUID

class UserServiceTest {

    private val authService = mock<AuthService>()
    private val userRepository = mock<UserRepository>()
    private val interactionService = mock<InteractionService>()
    private val objectMapper = JsonMapper.builder()
        .addModule(KotlinModule.Builder().build())
        .build()
    private val userService = UserService(authService, userRepository, objectMapper, interactionService)

    private val sessionId = UUID.randomUUID()
    private val userId = UUID.randomUUID()
    private val ip = "127.0.0.1"
    private val testUser = User(id = userId, username = "testuser", password = "hashed")

    private val validDto = UserConfigDTO(
        themeMode = "dark",
        selectedColor = "#1a73e8,#155ab6",
        heroEnabled = true,
        heroMode = "greeting",
        heroClockFormat = "12h",
        heroShowSeconds = false,
        heroGreetingName = "Test",
        weatherEnabled = false,
        weatherZip = "",
        weatherCorner = "top-right",
        weatherCity = "",
        weatherLat = 0.0,
        weatherLng = 0.0,
        fontFamily = "Fira Code",
        quickLinksEnabled = false,
        quickLinks = "[]",
        motdEnabled = false
    )

    private val validWeatherDto = WeatherCacheRequestDTO(
        cachedTemperature = 72,
        cachedWeatherCode = 1,
        cachedWindDirection = 180,
        cachedWindSpeed = 12.5,
        cachedWeatherDescription = "Partly cloudy",
        cachedIsDay = true
    )

    // ── getConfig ─────────────────────────────────────────────────────────────

    @Test
    fun `getConfig returns DTO for valid session`() {
        whenever(authService.resolveUser(sessionId)).thenReturn(testUser)

        val result = userService.getConfig(sessionId, ip)

        assert(result.username == testUser.username)
        assert(result.themeMode == testUser.themeMode)
    }

    @Test
    fun `getConfig records get config interaction`() {
        whenever(authService.resolveUser(sessionId)).thenReturn(testUser)

        userService.getConfig(sessionId, ip)

        verify(interactionService).recordGetConfig(user = userId, ip = ip)
    }

    @Test
    fun `getConfig with invalid session throws AuthException`() {
        whenever(authService.resolveUser(sessionId)).thenThrow(AuthException("Invalid session"))

        assertThrows<AuthException> { userService.getConfig(sessionId, ip) }
    }

    // ── updateConfig ──────────────────────────────────────────────────────────

    @Test
    fun `updateConfig saves user`() {
        whenever(authService.resolveUser(sessionId)).thenReturn(testUser)
        whenever(userRepository.save(any<User>())).thenReturn(testUser)

        userService.updateConfig(sessionId, validDto, ip)

        verify(userRepository).save(testUser)
    }

    @Test
    fun `updateConfig applies config fields to user`() {
        whenever(authService.resolveUser(sessionId)).thenReturn(testUser)
        whenever(userRepository.save(any<User>())).thenReturn(testUser)

        userService.updateConfig(sessionId, validDto, ip)

        assert(testUser.themeMode == "dark")
    }

    @Test
    fun `updateConfig records update config interaction`() {
        whenever(authService.resolveUser(sessionId)).thenReturn(testUser)
        whenever(userRepository.save(any<User>())).thenReturn(testUser)

        userService.updateConfig(sessionId, validDto, ip)

        verify(interactionService).recordUpdateConfig(user = userId, ip = ip)
    }

    @Test
    fun `updateConfig with invalid session throws AuthException`() {
        whenever(authService.resolveUser(sessionId)).thenThrow(AuthException("Invalid session"))

        assertThrows<AuthException> { userService.updateConfig(sessionId, validDto, ip) }
    }

    @Test
    fun `updateConfig with invalid font family throws UserException`() {
        whenever(authService.resolveUser(sessionId)).thenReturn(testUser)
        val dto = validDto.copy(fontFamily = "Comic Sans MS")

        assertThrows<UserException> { userService.updateConfig(sessionId, dto, ip) }
    }

    @Test
    fun `updateConfig with malformed quick links throws UserException`() {
        whenever(authService.resolveUser(sessionId)).thenReturn(testUser)
        val dto = validDto.copy(quickLinks = "not-json")

        assertThrows<UserException> { userService.updateConfig(sessionId, dto, ip) }
    }

    @Test
    fun `updateConfig with javascript url throws UserException`() {
        whenever(authService.resolveUser(sessionId)).thenReturn(testUser)
        val dto = validDto.copy(quickLinks = """[{"label":"x","url":"javascript:alert(1)"}]""")

        assertThrows<UserException> { userService.updateConfig(sessionId, dto, ip) }
    }

    @Test
    fun `updateConfig with http url succeeds`() {
        whenever(authService.resolveUser(sessionId)).thenReturn(testUser)
        whenever(userRepository.save(any<User>())).thenReturn(testUser)
        val dto = validDto.copy(quickLinks = """[{"label":"Google","url":"http://google.com"}]""")

        userService.updateConfig(sessionId, dto, ip)

        verify(userRepository).save(testUser)
    }

    @Test
    fun `updateConfig with https url succeeds`() {
        whenever(authService.resolveUser(sessionId)).thenReturn(testUser)
        whenever(userRepository.save(any<User>())).thenReturn(testUser)
        val dto = validDto.copy(quickLinks = """[{"label":"Google","url":"https://google.com"}]""")

        userService.updateConfig(sessionId, dto, ip)

        verify(userRepository).save(testUser)
    }

    @Test
    fun `updateConfig does not record interaction on failure`() {
        whenever(authService.resolveUser(sessionId)).thenReturn(testUser)
        val dto = validDto.copy(quickLinks = "not-json")

        runCatching { userService.updateConfig(sessionId, dto, ip) }

        verify(interactionService, never()).recordUpdateConfig(any(), any())
    }

    // ── updateWeather ─────────────────────────────────────────────────────────

    @Test
    fun `updateWeather saves all weather fields`() {
        whenever(authService.resolveUser(sessionId)).thenReturn(testUser)
        whenever(userRepository.save(any<User>())).thenReturn(testUser)

        userService.updateWeather(sessionId, validWeatherDto, ip)

        assert(testUser.cachedTemperature == 72)
        assert(testUser.cachedWeatherCode == 1)
        assert(testUser.cachedWindDirection == 180)
        assert(testUser.cachedWindSpeed == 12.5)
        assert(testUser.cachedWeatherDescription == "Partly cloudy")
        assert(testUser.cachedIsDay == true)
        assert(testUser.lastWeatherUpdate != null)
        verify(userRepository).save(testUser)
    }

    @Test
    fun `updateWeather records update weather interaction`() {
        whenever(authService.resolveUser(sessionId)).thenReturn(testUser)
        whenever(userRepository.save(any<User>())).thenReturn(testUser)

        userService.updateWeather(sessionId, validWeatherDto, ip)

        verify(interactionService).recordUpdateWeather(user = userId, ip = ip)
    }

    @Test
    fun `updateWeather with invalid session throws AuthException`() {
        whenever(authService.resolveUser(sessionId)).thenThrow(AuthException("Invalid session"))

        assertThrows<AuthException> { userService.updateWeather(sessionId, validWeatherDto, ip) }
    }
}
