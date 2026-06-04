package com.shivankkapoor.helmseek_backend.controller

import com.shivankkapoor.helmseek_backend.controller.AuthController.Companion.COOKIE_NAME
import com.shivankkapoor.helmseek_backend.dto.UserConfigDTO
import com.shivankkapoor.helmseek_backend.model.User
import com.shivankkapoor.helmseek_backend.repository.UserRepository
import com.shivankkapoor.helmseek_backend.service.AuthException
import com.shivankkapoor.helmseek_backend.service.AuthService
import com.shivankkapoor.helmseek_backend.service.IpService
import jakarta.servlet.http.HttpServletRequest
import jakarta.validation.Valid
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.util.UUID

@RestController
@RequestMapping("/user")
class UserController(
    private val authService: AuthService,
    private val userRepository: UserRepository,
    private val ipService: IpService
) {

    companion object {
        private val log = LoggerFactory.getLogger(UserController::class.java)
    }

    @GetMapping("/config")
    fun getConfig(request: HttpServletRequest): ResponseEntity<UserConfigDTO> {
        val user = resolveUser(request) ?: return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build()
        log.debug("Config fetched for username={} ip={}", user.username, ipService.getClientIp(request))
        return ResponseEntity.ok(user.toConfigDTO())
    }

    @PostMapping("/config")
    fun updateConfig(@Valid @RequestBody body: UserConfigDTO, request: HttpServletRequest): ResponseEntity<Void> {
        val user = resolveUser(request) ?: return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build()
        user.applyConfig(body)
        userRepository.save(user)
        log.info("Config updated for username={} ip={}", user.username, ipService.getClientIp(request))
        return ResponseEntity.ok().build()
    }

    private fun resolveUser(request: HttpServletRequest): User? {
        val sessionId = request.cookies
            ?.find { it.name == COOKIE_NAME }
            ?.value
            ?.let { runCatching { UUID.fromString(it) }.getOrNull() }
            ?: return null
        return try {
            authService.resolveUser(sessionId)
        } catch (e: AuthException) {
            null
        }
    }
}

private fun User.toConfigDTO() = UserConfigDTO(
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
    quickLinksEnabled = quickLinksEnabled,
    quickLinks = quickLinks
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
    quickLinksEnabled = dto.quickLinksEnabled
    quickLinks = dto.quickLinks
}
