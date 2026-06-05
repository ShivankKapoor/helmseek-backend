package com.shivankkapoor.helmseek_backend.filter

import com.google.common.cache.CacheBuilder
import com.shivankkapoor.helmseek_backend.service.IpService
import io.github.bucket4j.Bandwidth
import io.github.bucket4j.Bucket
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter
import java.time.Duration
import java.util.concurrent.TimeUnit

@Component
class RateLimitFilter(
    private val ipService: IpService,
    @Value("\${app.rate-limit-per-minute}") private val globalLimit: Long,
    @Value("\${app.rate-limit-burst}") private val burstLimit: Long,
    @Value("\${app.rate-limit-auth-per-minute}") private val authLimit: Long,
) : OncePerRequestFilter() {

    private val globalBuckets = CacheBuilder.newBuilder()
        .expireAfterAccess(10, TimeUnit.MINUTES)
        .build<String, Bucket>()

    private val authBuckets = CacheBuilder.newBuilder()
        .expireAfterAccess(10, TimeUnit.MINUTES)
        .build<String, Bucket>()

    private fun globalBucket(ip: String): Bucket = globalBuckets.get(ip) {
        Bucket.builder()
            .addLimit(Bandwidth.builder().capacity(burstLimit).refillGreedy(globalLimit, Duration.ofMinutes(1)).build())
            .build()
    }

    private fun authBucket(ip: String): Bucket = authBuckets.get(ip) {
        Bucket.builder()
            .addLimit(Bandwidth.builder().capacity(authLimit).refillGreedy(authLimit, Duration.ofMinutes(1)).build())
            .build()
    }

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        chain: FilterChain
    ) {
        val ip = ipService.getClientIp(request)
        val isAuthLogin = request.method == "POST" && request.requestURI == "/auth/login"

        if (isAuthLogin && !authBucket(ip).tryConsume(1)) {
            response.status = HttpStatus.TOO_MANY_REQUESTS.value()
            response.contentType = "application/json;charset=UTF-8"
            response.writer.write("""{"error":"Too many login attempts. Try again later."}""")
            return
        }

        if (!globalBucket(ip).tryConsume(1)) {
            response.status = HttpStatus.TOO_MANY_REQUESTS.value()
            response.contentType = "application/json;charset=UTF-8"
            response.writer.write("""{"error":"Too many requests. Try again later."}""")
            return
        }

        chain.doFilter(request, response)
    }
}
