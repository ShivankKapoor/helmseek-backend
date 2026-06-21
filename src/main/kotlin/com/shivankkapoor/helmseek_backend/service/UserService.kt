package com.shivankkapoor.helmseek_backend.service

import tools.jackson.databind.ObjectMapper
import tools.jackson.module.kotlin.readValue
import com.shivankkapoor.helmseek_backend.dto.QuickLink
import com.shivankkapoor.helmseek_backend.dto.UserConfigDTO
import com.shivankkapoor.helmseek_backend.dto.request.WeatherCacheRequestDTO
import com.shivankkapoor.helmseek_backend.model.User
import com.shivankkapoor.helmseek_backend.repository.UserRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.OffsetDateTime
import java.util.UUID

@Service
class UserService(
    private val authService: AuthService,
    private val userRepository: UserRepository,
    private val objectMapper: ObjectMapper,
    private val interactionService: InteractionService
) {
    companion object {
        private val log = LoggerFactory.getLogger(UserService::class.java)
    }

    fun getConfig(sessionId: UUID, ip: String): UserConfigDTO {
        val user = authService.resolveUser(sessionId)
        log.debug("Config fetched for username={}", user.username)
        interactionService.recordGetConfig(user = user.id!!, ip = ip)
        return user.toConfigDTO()
    }

    fun updateConfig(sessionId: UUID, dto: UserConfigDTO, ip: String) {
        val user = authService.resolveUser(sessionId)
        val links = parseQuickLinks(dto.quickLinks) ?: run {
            log.warn("Invalid quick links JSON for username={}", user.username)
            throw UserException("Invalid quick links JSON")
        }
        if (links.any { !it.url.startsWith("https://") && !it.url.startsWith("http://") }) {
            log.warn("Invalid URL in quick links for username={}", user.username)
            throw UserException("Invalid URL in quick links")
        }
        user.applyConfig(dto)
        userRepository.save(user)
        log.info("Config updated for username={}", user.username)
        interactionService.recordUpdateConfig(user = user.id!!, ip = ip)
    }

    fun updateWeather(sessionId: UUID, dto: WeatherCacheRequestDTO, ip: String) {
        val user = authService.resolveUser(sessionId)
        user.cachedTemperature = dto.cachedTemperature
        user.cachedWeatherCode = dto.cachedWeatherCode
        user.cachedWindDirection = dto.cachedWindDirection
        user.cachedWindSpeed = dto.cachedWindSpeed
        user.cachedWeatherDescription = dto.cachedWeatherDescription
        user.cachedIsDay = dto.cachedIsDay
        user.lastWeatherUpdate = OffsetDateTime.now()
        userRepository.save(user)
        log.debug("Weather updated for username={}", user.username)
        interactionService.recordUpdateWeather(user = user.id!!, ip = ip)
    }

    private fun parseQuickLinks(json: String): List<QuickLink>? = try {
        objectMapper.readValue<List<QuickLink>>(json)
    } catch (e: Exception) {
        null
    }
}

class UserException(message: String) : RuntimeException(message)

private fun User.toConfigDTO() = UserConfigDTO(
    username = username,
    themeMode = themeMode,
    selectedColor = selectedColor,
    heroEnabled = heroEnabled,
    heroMode = heroMode,
    heroClockFormat = heroClockFormat,
    heroShowSeconds = heroShowSeconds,
    heroGreetingName = heroGreetingName,
    weatherEnabled = weatherEnabled,
    weatherZip = weatherZip,
    weatherCorner = weatherCorner,
    weatherCity = weatherCity,
    weatherLat = weatherLat,
    weatherLng = weatherLng,
    fontFamily = fontFamily,
    quickLinksEnabled = quickLinksEnabled,
    quickLinks = quickLinks,
    cachedTemperature = cachedTemperature,
    cachedWeatherCode = cachedWeatherCode,
    cachedWindDirection = cachedWindDirection,
    cachedWindSpeed = cachedWindSpeed,
    cachedWeatherDescription = cachedWeatherDescription,
    cachedIsDay = cachedIsDay,
    lastWeatherUpdate = lastWeatherUpdate
)

private fun User.applyConfig(dto: UserConfigDTO) {
    themeMode = dto.themeMode
    selectedColor = dto.selectedColor
    heroEnabled = dto.heroEnabled
    heroMode = dto.heroMode
    heroClockFormat = dto.heroClockFormat
    heroShowSeconds = dto.heroShowSeconds
    heroGreetingName = dto.heroGreetingName
    weatherEnabled = dto.weatherEnabled
    weatherZip = dto.weatherZip
    weatherCorner = dto.weatherCorner
    weatherCity = dto.weatherCity
    weatherLat = dto.weatherLat
    weatherLng = dto.weatherLng
    fontFamily = dto.fontFamily
    quickLinksEnabled = dto.quickLinksEnabled
    quickLinks = dto.quickLinks
}
