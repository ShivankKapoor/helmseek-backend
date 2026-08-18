package com.shivankkapoor.helmseek_backend.model

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.hibernate.annotations.Generated
import org.hibernate.generator.EventType
import java.time.OffsetDateTime
import java.util.UUID

@Entity
@Table(name = "weather_history")
class WeatherHistory(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(updatable = false, nullable = false)
    val id: Long? = null,

    @Column(name = "zip", nullable = false)
    var zip: String,

    @Column(name = "city", nullable = false)
    var city: String,

    @Column(name = "lat", nullable = false)
    var lat: Double,

    @Column(name = "lng", nullable = false)
    var lng: Double,

    @Column(name = "temperature", nullable = false)
    var temperature: Int,

    @Column(name = "weather_code", nullable = false)
    var weatherCode: Int,

    @Column(name = "wind_direction", nullable = false)
    var windDirection: Int,

    @Column(name = "wind_speed", nullable = false)
    var windSpeed: Double,

    @Column(name = "weather_description", nullable = false)
    var weatherDescription: String,

    @Column(name = "is_day", nullable = false)
    var isDay: Boolean,

    @Column(name = "recorded_by", nullable = true)
    var recordedBy: UUID? = null,

    @Generated(event = [EventType.INSERT])
    @Column(name = "recorded_at", nullable = false, updatable = false, insertable = false)
    val recordedAt: OffsetDateTime? = null
)
