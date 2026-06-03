package com.shivankkapoor.helmseek_backend.service

import jakarta.servlet.http.HttpServletRequest
import org.springframework.stereotype.Service

@Service
class IpService {

    fun getClientIp(request: HttpServletRequest): String =
        request.getHeader("CF-Connecting-IP")
            ?: request.getHeader("X-Forwarded-For")?.split(",")?.first()?.trim()
            ?: request.remoteAddr
}
