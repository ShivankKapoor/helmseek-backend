package com.shivankkapoor.helmseek_backend.service

import com.shivankkapoor.helmseek_backend.repository.HealthRepository
import org.springframework.stereotype.Service

@Service
class HealthService(private val healthRepository: HealthRepository) {

    fun isDbHealthy(): Boolean = healthRepository.isDbHealthy()
}
