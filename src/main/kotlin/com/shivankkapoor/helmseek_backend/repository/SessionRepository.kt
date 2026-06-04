package com.shivankkapoor.helmseek_backend.repository

import com.shivankkapoor.helmseek_backend.model.Session
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import java.time.OffsetDateTime
import java.util.UUID

interface SessionRepository : JpaRepository<Session, UUID> {
    fun findByIdAndExpiresAtAfter(id: UUID, now: OffsetDateTime): Session?

    @Modifying
    @Query("DELETE FROM Session s WHERE s.expiresAt < :now")
    fun deleteExpired(now: OffsetDateTime): Int
}
