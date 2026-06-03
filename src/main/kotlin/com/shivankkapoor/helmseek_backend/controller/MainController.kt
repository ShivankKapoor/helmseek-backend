package com.shivankkapoor.helmseek_backend.controller

import com.shivankkapoor.helmseek_backend.dto.response.HealthResponseDTO
import com.shivankkapoor.helmseek_backend.service.HealthService
import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

@RestController
class MainController(
    private val healthService: HealthService
) {

    companion object {
        private val log = LoggerFactory.getLogger(MainController::class.java)
    }

    @GetMapping("/")
    fun home(): ResponseEntity<String> {
        log.info("[MAIN] GET /")
        return ResponseEntity.ok("<h1>Welcome to the HelmSeek backend!</h1>")
    }

    @GetMapping("/health")
    fun health(): ResponseEntity<HealthResponseDTO> {
        log.info("[HEALTH] GET /")
        val dbHealthy = healthService.isDbHealthy()
        return ResponseEntity.ok(HealthResponseDTO(
            status = "UP",
            db = if (dbHealthy) "UP" else "DOWN",
        ))
    }
}
