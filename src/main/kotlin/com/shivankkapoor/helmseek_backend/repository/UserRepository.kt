package com.shivankkapoor.helmseek_backend.repository

import com.shivankkapoor.helmseek_backend.model.User
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface UserRepository : JpaRepository<User, UUID> {
    fun findByUsername(username: String): User?
    fun existsByUsername(username: String): Boolean
}
