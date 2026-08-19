package com.shivankkapoor.helmseek_backend.service

import com.shivankkapoor.helmseek_backend.dto.request.WeatherCacheRequestDTO
import com.shivankkapoor.helmseek_backend.model.User
import com.shivankkapoor.helmseek_backend.model.WeatherHistory
import com.github.benmanes.caffeine.cache.Cache
import com.github.benmanes.caffeine.cache.Caffeine
import com.shivankkapoor.helmseek_backend.repository.WeatherHistoryRepository
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Service
import java.util.UUID
import java.util.concurrent.TimeUnit

@Service
class WeatherHistoryService (private val weatherHistoryRepository: WeatherHistoryRepository) {

    companion object {
        private val log = LoggerFactory.getLogger(WeatherHistoryService::class.java)
    }

    private val recentlyRecorded: Cache<UUID, Boolean> = Caffeine.newBuilder()
        .expireAfterWrite(10, TimeUnit.MINUTES)
        .build()

    @Async
    fun recordWeatherHistory(user: User, dto: WeatherCacheRequestDTO){
        val userId = user.id
        if (userId != null && recentlyRecorded.asMap().putIfAbsent(userId, true) != null) {
            log.info("Skipping weather history record for username={}, already recorded within dedup window", user.username)
            return
        }
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
            if (userId != null) recentlyRecorded.invalidate(userId)
        }
    }
}