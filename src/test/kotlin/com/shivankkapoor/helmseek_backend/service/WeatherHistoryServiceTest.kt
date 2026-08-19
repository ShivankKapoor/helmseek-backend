package com.shivankkapoor.helmseek_backend.service

import com.shivankkapoor.helmseek_backend.dto.request.WeatherCacheRequestDTO
import com.shivankkapoor.helmseek_backend.model.User
import com.shivankkapoor.helmseek_backend.model.WeatherHistory
import com.shivankkapoor.helmseek_backend.repository.WeatherHistoryRepository
import org.junit.jupiter.api.Test
import org.mockito.kotlin.*
import java.util.UUID

class WeatherHistoryServiceTest {

    private val weatherHistoryRepository = mock<WeatherHistoryRepository>()
    private val weatherHistoryService = WeatherHistoryService(weatherHistoryRepository)

    private val userId = UUID.randomUUID()
    private val testUser = User(
        id = userId,
        username = "testuser",
        password = "hashed",
        weatherZip = "  90210 ",
        weatherCity = "Beverly Hills",
        weatherLat = 34.0901,
        weatherLng = -118.4065
    )

    private val validWeatherDto = WeatherCacheRequestDTO(
        cachedTemperature = 72,
        cachedWeatherCode = 1,
        cachedWindDirection = 180,
        cachedWindSpeed = 12.5,
        cachedWeatherDescription = "Partly cloudy",
        cachedIsDay = true
    )

    init {
        // CrudRepository.save() is @NonNull; with -Xjsr305=strict an unstubbed (null-returning)
        // mock trips a Kotlin null-check that the service's catch block would otherwise treat
        // as a real save failure. Stub it to behave like a real save.
        whenever(weatherHistoryRepository.save(any<WeatherHistory>())).thenAnswer { it.arguments[0] }
    }

    @Test
    fun `recordWeatherHistory saves a row with fields mapped from user and dto`() {
        weatherHistoryService.recordWeatherHistory(testUser, validWeatherDto)

        val captor = argumentCaptor<WeatherHistory>()
        verify(weatherHistoryRepository).save(captor.capture())
        val saved = captor.firstValue

        assert(saved.city == "Beverly Hills")
        assert(saved.lat == 34.0901)
        assert(saved.lng == -118.4065)
        assert(saved.temperature == 72)
        assert(saved.weatherCode == 1)
        assert(saved.windDirection == 180)
        assert(saved.windSpeed == 12.5)
        assert(saved.weatherDescription == "Partly cloudy")
        assert(saved.isDay)
        assert(saved.recordedBy == userId)
    }

    @Test
    fun `recordWeatherHistory normalizes zip`() {
        testUser.weatherZip = "  sw1a 1aa  "

        weatherHistoryService.recordWeatherHistory(testUser, validWeatherDto)

        val captor = argumentCaptor<WeatherHistory>()
        verify(weatherHistoryRepository).save(captor.capture())

        assert(captor.firstValue.zip == "SW1A 1AA")
    }

    @Test
    fun `recordWeatherHistory swallows repository failures`() {
        whenever(weatherHistoryRepository.save(any<WeatherHistory>())).thenThrow(RuntimeException("db down"))

        weatherHistoryService.recordWeatherHistory(testUser, validWeatherDto)
    }

    @Test
    fun `recordWeatherHistory skips save for the same user within the dedup window`() {
        weatherHistoryService.recordWeatherHistory(testUser, validWeatherDto)
        weatherHistoryService.recordWeatherHistory(testUser, validWeatherDto)

        verify(weatherHistoryRepository, times(1)).save(any<WeatherHistory>())
    }

    @Test
    fun `recordWeatherHistory saves for a different user`() {
        val otherUser = User(
            id = UUID.randomUUID(),
            username = "otheruser",
            password = "hashed",
            weatherZip = "75019",
            weatherCity = "Coppell",
            weatherLat = 32.9545,
            weatherLng = -97.0150
        )

        weatherHistoryService.recordWeatherHistory(testUser, validWeatherDto)
        weatherHistoryService.recordWeatherHistory(otherUser, validWeatherDto)

        verify(weatherHistoryRepository, times(2)).save(any<WeatherHistory>())
    }

    @Test
    fun `recordWeatherHistory always saves for anonymous users`() {
        val anonymousUser = User(
            id = null,
            username = "anon",
            password = "hashed",
            weatherZip = "75019",
            weatherCity = "Coppell",
            weatherLat = 32.9545,
            weatherLng = -97.0150
        )

        weatherHistoryService.recordWeatherHistory(anonymousUser, validWeatherDto)
        weatherHistoryService.recordWeatherHistory(anonymousUser, validWeatherDto)

        verify(weatherHistoryRepository, times(2)).save(any<WeatherHistory>())
    }

    @Test
    fun `recordWeatherHistory allows immediate retry after a failed save`() {
        whenever(weatherHistoryRepository.save(any<WeatherHistory>()))
            .thenThrow(RuntimeException("db down"))
            .thenAnswer { it.arguments[0] }

        weatherHistoryService.recordWeatherHistory(testUser, validWeatherDto)
        weatherHistoryService.recordWeatherHistory(testUser, validWeatherDto)

        verify(weatherHistoryRepository, times(2)).save(any<WeatherHistory>())
    }
}
