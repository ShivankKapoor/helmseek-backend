package com.shivankkapoor.helmseek_backend.service

import com.shivankkapoor.helmseek_backend.dto.request.WeatherCacheRequestDTO
import com.shivankkapoor.helmseek_backend.model.User
import com.shivankkapoor.helmseek_backend.model.WeatherHistory
import com.shivankkapoor.helmseek_backend.repository.WeatherHistoryRepository
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Service

@Service
class WeatherHistoryService (private val weatherHistoryRepository: WeatherHistoryRepository) {

    companion object {
        private val log = LoggerFactory.getLogger(WeatherHistoryService::class.java)
    }

    @Async
    fun recordWeatherHistory(user: User, dto: WeatherCacheRequestDTO){
        val history = WeatherHistory(
            zip = user.weatherZip.trim().uppercase(),
            city = user.weatherCity,
            lat = user.weatherLat,
            lng = user.weatherLng,
            temperature = dto.cachedTemperature,
            weatherCode = dto.cachedWeatherCode,
            windDirection = dto.cachedWindDirection,
            windSpeed = dto.cachedWindSpeed,
            weatherDescription = dto.cachedWeatherDescription,
            isDay = dto.cachedIsDay,
            recordedBy = user.id
        )
        try{
            weatherHistoryRepository.save(history)
        }catch(e: Exception){
            log.error("Error while saving weather in history table",e)
        }
    }
}