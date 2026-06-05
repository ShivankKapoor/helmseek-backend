package com.shivankkapoor.helmseek_backend.service

import com.shivankkapoor.helmseek_backend.repository.HealthRepository
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

class HealthServiceTest {

    private val healthRepository = mock<HealthRepository>()
    private val healthService = HealthService(healthRepository)

    @Test
    fun `isDbHealthy returns true when repository returns true`() {
        whenever(healthRepository.isDbHealthy()).thenReturn(true)
        assert(healthService.isDbHealthy())
    }

    @Test
    fun `isDbHealthy returns false when repository returns false`() {
        whenever(healthRepository.isDbHealthy()).thenReturn(false)
        assert(!healthService.isDbHealthy())
    }
}
