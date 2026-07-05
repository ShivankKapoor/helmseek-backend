package com.shivankkapoor.helmseek_backend.repository

import com.shivankkapoor.helmseek_backend.model.User
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import java.util.UUID

interface UserRepository : JpaRepository<User, UUID> {
    fun findByUsername(username: String): User?
    fun existsByUsername(username: String): Boolean

    @Modifying
    @Query("UPDATE User u SET u.hideQuote = false WHERE u.hideQuote = true")
    fun resetHideQuote(): Int
}
