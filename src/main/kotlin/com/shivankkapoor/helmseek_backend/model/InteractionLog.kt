package com.shivankkapoor.helmseek_backend.model

import jakarta.persistence.*
import org.hibernate.annotations.Generated
import org.hibernate.generator.EventType
import java.time.OffsetDateTime
import java.util.UUID

@Entity
@Table(name = "interaction_log")
class InteractionLog(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(updatable = false, nullable = false)
    val id: Long? = null,

    @Column(name = "user_id", nullable = true, updatable = false)
    val user: UUID?,

    @Column(name = "ip", nullable = false, updatable = false)
    val ip: String,

    @Column(name = "action")
    val action: String,

    @Generated(event = [EventType.INSERT])
    @Column(name = "created_at", nullable = false, updatable = false, insertable = false)
    val createdAt: OffsetDateTime? = null,

    @Column(name = "city", nullable = true, updatable = false)
    val city: String?,

    @Column(name = "country", nullable = true, updatable = false)
    val country: String?,
)
