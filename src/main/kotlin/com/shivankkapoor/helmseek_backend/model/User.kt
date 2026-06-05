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
@Table(name = "users")
class User(

    @Id
    @Generated(event = [EventType.INSERT])
    @Column(updatable = false, nullable = false, insertable = false)
    val id: UUID? = null,

    @Column(nullable = false, unique = true)
    var username: String,

    @Column(nullable = false)
    var password: String,

    @Generated(event = [EventType.INSERT])
    @Column(name = "created_at", nullable = false, updatable = false, insertable = false)
    val createdAt: OffsetDateTime? = null,

    @Column(name = "last_read")
    var lastRead: OffsetDateTime? = null,

    // Theme
    @Column(name = "theme_mode", nullable = false)
    var themeMode: String = "light",

    @Column(name = "selected_color", nullable = false)
    var selectedColor: String = "#1a73e8,#155ab6",

    // Hero widget
    @Column(name = "hero_enabled", nullable = false)
    var heroEnabled: Boolean = true,

    @Column(name = "hero_mode", nullable = false)
    var heroMode: String = "greeting",

    @Column(name = "hero_clock_format", nullable = false)
    var heroClockFormat: String = "12h",

    @Column(name = "hero_show_seconds", nullable = false)
    var heroShowSeconds: Boolean = false,

    @Column(name = "hero_greeting_name", nullable = false)
    var heroGreetingName: String = "",

    // Weather widget
    @Column(name = "weather_enabled", nullable = false)
    var weatherEnabled: Boolean = false,

    @Column(name = "weather_zip", nullable = false)
    var weatherZip: String = "",

    @Column(name = "weather_corner", nullable = false)
    var weatherCorner: String = "top-right",

    @Column(name = "weather_city", nullable = false)
    var weatherCity: String = "",

    @Column(name = "weather_lat", nullable = false)
    var weatherLat: Double = 0.0,

    @Column(name = "weather_lng", nullable = false)
    var weatherLng: Double = 0.0,

    // Quick links
    @Column(name = "quick_links_enabled", nullable = false)
    var quickLinksEnabled: Boolean = false,

    @Column(name = "quick_links", nullable = false)
    var quickLinks: String = "[]",

    // Cached weather (nullable — not set until frontend pushes weather data)
    @Column(name = "cached_temperature")
    var cachedTemperature: Int? = null,

    @Column(name = "cached_weather_code")
    var cachedWeatherCode: Int? = null,

    @Column(name = "cached_wind_direction")
    var cachedWindDirection: Int? = null,

    @Column(name = "cached_wind_speed")
    var cachedWindSpeed: Double? = null,

    @Column(name = "cached_weather_description")
    var cachedWeatherDescription: String? = null,

    @Column(name = "cached_is_day")
    var cachedIsDay: Boolean? = null,

    @Column(name = "last_weather_update")
    var lastWeatherUpdate: OffsetDateTime? = null
)
