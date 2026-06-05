package com.shivankkapoor.helmseek_backend.service

import jakarta.servlet.http.HttpServletRequest
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

class IpServiceTest {

    private val ipService = IpService()

    private fun request(cf: String? = null, forwarded: String? = null, remote: String = "10.0.0.1"): HttpServletRequest {
        val req = mock<HttpServletRequest>()
        whenever(req.getHeader("CF-Connecting-IP")).thenReturn(cf)
        whenever(req.getHeader("X-Forwarded-For")).thenReturn(forwarded)
        whenever(req.remoteAddr).thenReturn(remote)
        return req
    }

    @Test
    fun `CF-Connecting-IP is preferred over all other headers`() {
        val req = request(cf = "1.2.3.4", forwarded = "5.6.7.8", remote = "9.10.11.12")
        assert(ipService.getClientIp(req) == "1.2.3.4")
    }

    @Test
    fun `X-Forwarded-For used when CF header absent`() {
        val req = request(forwarded = "5.6.7.8", remote = "9.10.11.12")
        assert(ipService.getClientIp(req) == "5.6.7.8")
    }

    @Test
    fun `first IP in X-Forwarded-For chain is returned`() {
        val req = request(forwarded = "5.6.7.8, 9.10.11.12, 13.14.15.16")
        assert(ipService.getClientIp(req) == "5.6.7.8")
    }

    @Test
    fun `remoteAddr used when both proxy headers absent`() {
        val req = request(remote = "9.10.11.12")
        assert(ipService.getClientIp(req) == "9.10.11.12")
    }
}
