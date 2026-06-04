package com.shivankkapoor.helmseek_backend.dto

import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Size
import java.time.OffsetDateTime

data class UserConfigDTO(

    // Theme
    @field:Pattern(regexp = "light|dark")
    val themeMode: String,

    @field:Pattern(regexp = "#[0-9a-fA-F]{6},#[0-9a-fA-F]{6}")
    val selectedColor: String,

    // Hero widget
    val heroEnabled: Boolean,

    @field:Pattern(regexp = "greeting|clock|both|none")
    val heroMode: String,

    @field:Pattern(regexp = "12h|24h")
    val heroClockFormat: String,

    val heroShowSeconds: Boolean,

    @field:Size(max = 50)
    val heroGreetingName: String,

    // Weather widget
    val weatherEnabled: Boolean,

    @field:Size(max = 10)
    val weatherZip: String,

    @field:Pattern(regexp = "top-right|top-left|bottom-right|bottom-left")
    val weatherCorner: String,

    @field:Size(max = 100)
    val weatherCity: String,

    val weatherLat: Double,
    val weatherLng: Double,

    // Quick links
    val quickLinksEnabled: Boolean,

    @field:Size(max = 5000)
    val quickLinks: String,

    // Cached weather (read-only — updated via POST /user/weather, ignored on POST /user/config)
    val cachedTemperature: Int? = null,
    val cachedWeatherCode: Int? = null,
    val cachedWindDirection: Int? = null,
    val cachedWindSpeed: Double? = null,
    val cachedWeatherDescription: String? = null,
    val cachedIsDay: Boolean? = null,
    val lastWeatherUpdate: OffsetDateTime? = null
)
