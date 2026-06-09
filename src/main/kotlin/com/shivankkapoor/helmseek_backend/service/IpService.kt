package com.shivankkapoor.helmseek_backend.service

import com.shivankkapoor.helmseek_backend.dto.response.LocationResponseDTO
import jakarta.servlet.http.HttpServletRequest
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.web.client.RestClient
import org.springframework.web.client.RestClientException

@Service
class IpService(
    @Value("\${api.meridian}") private val meridianUrl: String, builder: RestClient.Builder
) {
    companion object {
        private val log = LoggerFactory.getLogger(IpService::class.java)
    }

    private val meridianClient = builder.baseUrl(meridianUrl).build()

    fun getClientIp(request: HttpServletRequest): String =
        request.getHeader("CF-Connecting-IP") ?: request.getHeader("X-Forwarded-For")?.split(",")?.first()?.trim()
        ?: request.remoteAddr

    fun getLocation(ip: String): LocationResponseDTO {
        if (meridianUrl.isBlank()) return LocationResponseDTO("UNKNOWN", "UNKNOWN", "UNKNOWN", "UNKNOWN")
        return try {
            meridianClient.get().uri("/location/$ip").retrieve().body(LocationResponseDTO::class.java)
                ?: LocationResponseDTO("UNKNOWN", "UNKNOWN", "UNKNOWN", "UNKNOWN")
        } catch (e: RestClientException) {
            log.error(e.message, e)
            return LocationResponseDTO("UNKNOWN", "UNKNOWN", "UNKNOWN", "UNKNOWN")
        }
    }
}
