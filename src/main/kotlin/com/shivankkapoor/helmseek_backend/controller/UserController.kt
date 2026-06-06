package com.shivankkapoor.helmseek_backend.controller

import com.shivankkapoor.helmseek_backend.controller.AuthController.Companion.COOKIE_NAME
import com.shivankkapoor.helmseek_backend.dto.UserConfigDTO
import com.shivankkapoor.helmseek_backend.dto.request.WeatherCacheRequestDTO
import com.shivankkapoor.helmseek_backend.service.AuthException
import com.shivankkapoor.helmseek_backend.service.IpService
import com.shivankkapoor.helmseek_backend.service.UserException
import com.shivankkapoor.helmseek_backend.service.UserService
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
    private val userService: UserService,
    private val ipService: IpService
) {
    companion object {
        private val log = LoggerFactory.getLogger(UserController::class.java)
    }

    @GetMapping("/config")
    fun getConfig(request: HttpServletRequest): ResponseEntity<UserConfigDTO> {
        val sessionId = extractSessionId(request) ?: return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build()
        return try {
            val config = userService.getConfig(sessionId)
            log.debug("Config fetched ip={}", ipService.getClientIp(request))
            ResponseEntity.ok(config)
        } catch (e: AuthException) {
            ResponseEntity.status(HttpStatus.UNAUTHORIZED).build()
        }
    }

    @PostMapping("/config")
    fun updateConfig(@Valid @RequestBody body: UserConfigDTO, request: HttpServletRequest): ResponseEntity<Void> {
        val sessionId = extractSessionId(request) ?: return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build()
        return try {
            userService.updateConfig(sessionId, body)
            log.info("Config updated ip={}", ipService.getClientIp(request))
            ResponseEntity.ok().build()
        } catch (e: AuthException) {
            ResponseEntity.status(HttpStatus.UNAUTHORIZED).build()
        } catch (e: UserException) {
            ResponseEntity.badRequest().build()
        }
    }

    @PostMapping("/weather")
    fun updateWeather(@Valid @RequestBody body: WeatherCacheRequestDTO, request: HttpServletRequest): ResponseEntity<Void> {
        val sessionId = extractSessionId(request) ?: return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build()
        return try {
            userService.updateWeather(sessionId, body)
            log.debug("Weather updated ip={}", ipService.getClientIp(request))
            ResponseEntity.ok().build()
        } catch (e: AuthException) {
            ResponseEntity.status(HttpStatus.UNAUTHORIZED).build()
        }
    }

    private fun extractSessionId(request: HttpServletRequest): UUID? =
        request.cookies
            ?.find { it.name == COOKIE_NAME }
            ?.value
            ?.let { runCatching { UUID.fromString(it) }.getOrNull() }
}
