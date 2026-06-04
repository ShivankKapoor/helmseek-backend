package com.shivankkapoor.helmseek_backend.dto.request

import jakarta.validation.constraints.DecimalMin
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.Size

data class WeatherCacheRequestDTO(

    @field:Min(-100) @field:Max(150)
    val cachedTemperature: Int,

    @field:Min(0) @field:Max(99)
    val cachedWeatherCode: Int,

    @field:Min(0) @field:Max(360)
    val cachedWindDirection: Int,

    @field:DecimalMin("0.0")
    val cachedWindSpeed: Double,

    @field:Size(max = 50)
    val cachedWeatherDescription: String,

    val cachedIsDay: Boolean
)
