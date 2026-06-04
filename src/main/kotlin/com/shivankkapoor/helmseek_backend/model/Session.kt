package com.shivankkapoor.helmseek_backend.model

import jakarta.persistence.*
import org.hibernate.annotations.Generated
import org.hibernate.generator.EventType
import java.time.OffsetDateTime
import java.util.UUID

@Entity
@Table(name = "sessions")
class Session(

    @Id
    @Generated(event = [EventType.INSERT])
    @Column(updatable = false, nullable = false, insertable = false)
    val id: UUID? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, updatable = false)
    val user: User,

    @Generated(event = [EventType.INSERT])
    @Column(name = "created_at", nullable = false, updatable = false, insertable = false)
    val createdAt: OffsetDateTime? = null,

    @Generated(event = [EventType.INSERT])
    @Column(name = "expires_at", nullable = false, updatable = false, insertable = false)
    val expiresAt: OffsetDateTime? = null
)
