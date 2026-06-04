package com.shivankkapoor.helmseek_backend.controller

import com.shivankkapoor.helmseek_backend.dto.response.HealthResponseDTO
import com.shivankkapoor.helmseek_backend.service.HealthService
import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController
import kotlin.time.Clock
import kotlin.time.Instant

@RestController
class MainController(
    private val healthService: HealthService
) {

    companion object {
        private val log = LoggerFactory.getLogger(MainController::class.java)
        private val startTime: Instant = Clock.System.now()
    }

    @GetMapping("/")
    fun home(): ResponseEntity<String> {
        log.info("Home endpoint called")
        return ResponseEntity.ok("<h1>Welcome to the HelmSeek backend!</h1>")
    }

    @GetMapping("/health")
    fun health(): ResponseEntity<HealthResponseDTO> {
        log.info("Health endpoint called")
        val dbHealthy = healthService.isDbHealthy()
        return ResponseEntity.ok(HealthResponseDTO(
            status = "UP",
            db = if (dbHealthy) "UP" else "DOWN",
        ))
    }

    @GetMapping("/monitor")
    fun monitor(): ResponseEntity<Map<String, String>> {
        log.info("Monitoring endpoint called")

        val uptime = Clock.System.now() - startTime
        val days = uptime.inWholeDays
        val hours = uptime.inWholeHours % 24
        val minutes = uptime.inWholeMinutes % 60
        val seconds = uptime.inWholeSeconds % 60

        val uptimeStr = "${if (days > 0) "${days}d " else ""}${hours}h ${minutes}m ${seconds}s"

        return ResponseEntity.ok(mapOf(
            "status" to "UP",
            "uptime" to uptimeStr,
            "platform" to "Java",
            "vendor" to System.getProperty("java.vendor"),
        ))
    }
}
