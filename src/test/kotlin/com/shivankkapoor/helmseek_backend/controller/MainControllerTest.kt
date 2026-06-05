package com.shivankkapoor.helmseek_backend.controller

import com.shivankkapoor.helmseek_backend.config.SecurityConfig
import com.shivankkapoor.helmseek_backend.filter.RateLimitFilter
import com.shivankkapoor.helmseek_backend.service.HealthService
import org.junit.jupiter.api.Test
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.FilterType
import org.springframework.context.annotation.Import
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@WebMvcTest(
    value = [MainController::class],
    excludeFilters = [ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = [RateLimitFilter::class])]
)
@Import(SecurityConfig::class)
class MainControllerTest {

    @Autowired private lateinit var mockMvc: MockMvc
    @MockitoBean private lateinit var healthService: HealthService

    @Test
    fun `health with DB up returns status UP`() {
        whenever(healthService.isDbHealthy()).thenReturn(true)

        mockMvc.perform(get("/health"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.status").value("UP"))
            .andExpect(jsonPath("$.db").value("UP"))
    }

    @Test
    fun `health with DB down returns db DOWN`() {
        whenever(healthService.isDbHealthy()).thenReturn(false)

        mockMvc.perform(get("/health"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.status").value("UP"))
            .andExpect(jsonPath("$.db").value("DOWN"))
    }

    @Test
    fun `home returns 200`() {
        mockMvc.perform(get("/"))
            .andExpect(status().isOk)
    }

    @Test
    fun `monitor returns 200 with uptime`() {
        mockMvc.perform(get("/monitor"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.status").value("UP"))
            .andExpect(jsonPath("$.uptime").exists())
    }
}
