package com.shivankkapoor.helmseek_backend.controller

import com.shivankkapoor.helmseek_backend.dto.QuoteDTO
import com.shivankkapoor.helmseek_backend.service.AuthException
import com.shivankkapoor.helmseek_backend.service.AuthService
import com.shivankkapoor.helmseek_backend.service.IpService
import com.shivankkapoor.helmseek_backend.service.QuoteService
import com.shivankkapoor.helmseek_backend.service.UserService
import jakarta.servlet.http.HttpServletRequest
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/quote")
class QuoteController(
    private val ipService: IpService,
    private val authService: AuthService,
    private val quoteService: QuoteService,
    private val userService: UserService
) {

    companion object {
        private val log = LoggerFactory.getLogger(QuoteController::class.java)
    }

    @GetMapping
    fun getQuote(request: HttpServletRequest): ResponseEntity<QuoteDTO> {
        val sessionId = authService.extractSessionId(request) ?: return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build()
        val ip = ipService.getClientIp(request)
        return try {
            authService.resolveUser(sessionId)
            log.info("Quote request made from ip={}", ip)
            ResponseEntity.ok(quoteService.getQuote())
        } catch (e: AuthException) {
            ResponseEntity.status(HttpStatus.UNAUTHORIZED).build()
        }
    }

    @PostMapping("/hideQuote")
    fun hideQuote(request: HttpServletRequest): ResponseEntity<Void> {
        val sessionId = authService.extractSessionId(request) ?: return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build()
        val ip = ipService.getClientIp(request)
        return try {
            userService.hideQuote(sessionId, ip)
            log.info("Quote hidden ip={}", ip)
            ResponseEntity.ok().build()
        } catch (e: AuthException) {
            ResponseEntity.status(HttpStatus.UNAUTHORIZED).build()
        }
    }

    @PostMapping("/unhideQuote")
    fun unhideQuote(request: HttpServletRequest): ResponseEntity<Void> {
        val sessionId = authService.extractSessionId(request) ?: return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build()
        val ip = ipService.getClientIp(request)
        return try {
            userService.unhideQuote(sessionId, ip)
            log.info("Quote unhidden ip={}", ip)
            ResponseEntity.ok().build()
        } catch (e: AuthException) {
            ResponseEntity.status(HttpStatus.UNAUTHORIZED).build()
        }
    }
}