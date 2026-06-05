package com.shivankkapoor.helmseek_backend.filter

import com.shivankkapoor.helmseek_backend.service.IpService
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.junit.jupiter.api.Test
import org.mockito.kotlin.*
import java.io.PrintWriter

class RateLimitFilterTest {

    private val ipService = mock<IpService>()
    private val response = mock<HttpServletResponse>()
    private val chain = mock<FilterChain>()
    private val writer = mock<PrintWriter>()

    init {
        whenever(response.writer).thenReturn(writer)
    }

    private fun request(method: String = "GET", uri: String = "/user/config", ip: String = "1.2.3.4"): HttpServletRequest {
        val req = mock<HttpServletRequest>()
        whenever(req.method).thenReturn(method)
        whenever(req.requestURI).thenReturn(uri)
        whenever(ipService.getClientIp(req)).thenReturn(ip)
        return req
    }

    private fun buildFilter(globalLimit: Long = 100, burstLimit: Long = 10, authLimit: Long = 5): RateLimitFilter =
        RateLimitFilter(ipService, globalLimit, burstLimit, authLimit)

    @Test
    fun `request under global limit passes through`() {
        val filter = buildFilter(globalLimit = 100, burstLimit = 10)
        val req = request()

        filter.doFilter(req, response, chain)

        verify(chain).doFilter(req, response)
        verify(response, never()).writer
    }

    @Test
    fun `request exceeding global burst returns 429`() {
        val filter = buildFilter(globalLimit = 100, burstLimit = 3)
        val ip = "2.2.2.2"
        repeat(3) { filter.doFilter(request(ip = ip), response, chain) }

        val blocked = mock<HttpServletResponse>()
        whenever(blocked.writer).thenReturn(writer)
        filter.doFilter(request(ip = ip), blocked, chain)

        verify(blocked).status = 429
    }

    @Test
    fun `login attempt under auth limit passes through`() {
        val filter = buildFilter(authLimit = 5)
        val req = request(method = "POST", uri = "/auth/login")

        filter.doFilter(req, response, chain)

        verify(chain).doFilter(req, response)
    }

    @Test
    fun `login attempts exceeding auth limit returns 429`() {
        val filter = buildFilter(authLimit = 2, burstLimit = 100, globalLimit = 1000)
        val ip = "3.3.3.3"
        repeat(2) { filter.doFilter(request(method = "POST", uri = "/auth/login", ip = ip), response, chain) }

        val blocked = mock<HttpServletResponse>()
        whenever(blocked.writer).thenReturn(writer)
        filter.doFilter(request(method = "POST", uri = "/auth/login", ip = ip), blocked, chain)

        verify(blocked).status = 429
    }

    @Test
    fun `non-login POST is not subject to auth rate limit`() {
        val filter = buildFilter(authLimit = 1, burstLimit = 100, globalLimit = 1000)
        val ip = "4.4.4.4"

        repeat(5) {
            filter.doFilter(request(method = "POST", uri = "/user/config", ip = ip), response, chain)
        }

        verify(chain, times(5)).doFilter(any(), any())
    }

    @Test
    fun `different IPs have independent rate limit buckets`() {
        val filter = buildFilter(globalLimit = 100, burstLimit = 2)

        filter.doFilter(request(ip = "5.5.5.5"), response, chain)
        filter.doFilter(request(ip = "5.5.5.5"), response, chain)

        val req2 = request(ip = "6.6.6.6")
        filter.doFilter(req2, response, chain)

        verify(chain, times(3)).doFilter(any(), any())
    }
}
