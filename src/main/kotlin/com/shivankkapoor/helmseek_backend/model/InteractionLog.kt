package com.shivankkapoor.helmseek_backend.model

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.hibernate.annotations.Generated
import org.hibernate.generator.EventType
import java.time.OffsetDateTime
import java.util.UUID

@Entity
@Table(name = "interaction_log")
class InteractionLog(

    @Id
    @Generated(event = [EventType.INSERT])
    @Column(updatable = false, nullable = false, insertable = false)
    val id: UUID? = null,

    @Column(name = "user_id", nullable = true, updatable = false)
    val user: UUID?,

    @Column(name = "ip", nullable = false, updatable = false)
    val ip: String,

    @Column(name = "action")
    val action: String,

    @Generated(event = [EventType.INSERT])
    @Column(name = "created_at", nullable = false, updatable = false, insertable = false)
    val createdAt: OffsetDateTime? = null,
)