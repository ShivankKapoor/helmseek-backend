package com.shivankkapoor.helmseek_backend.repository

import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Repository

@Repository
class HealthRepository(private val jdbcTemplate: JdbcTemplate) {

    fun isDbHealthy(): Boolean {
        return try {
            jdbcTemplate.queryForObject("SELECT 1", Int::class.java)
            true
        } catch (e: Exception) {
            false
        }
    }
}
