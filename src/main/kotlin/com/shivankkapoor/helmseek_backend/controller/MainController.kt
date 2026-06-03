package com.shivankkapoor.helmseek_backend.controller

import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

@RestController
class MainController {

    companion object {
        private val log = LoggerFactory.getLogger(MainController::class.java)
    }

    @GetMapping("/")
    fun home(): ResponseEntity<String> {
        log.info("[MAIN] GET /")
        return ResponseEntity.ok("<h1>Welcome to the HelmSeek backend!</h1>")
    }
}
