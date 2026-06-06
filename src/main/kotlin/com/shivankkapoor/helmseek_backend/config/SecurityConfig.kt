package com.shivankkapoor.helmseek_backend.config

import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.crypto.argon2.Argon2PasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.config.annotation.web.invoke
import org.springframework.security.web.SecurityFilterChain
import org.springframework.web.cors.CorsConfiguration
import org.springframework.web.cors.CorsConfigurationSource
import org.springframework.web.cors.DefaultCorsProcessor
import org.springframework.web.cors.UrlBasedCorsConfigurationSource
import org.springframework.web.filter.CorsFilter

@Configuration
@EnableWebSecurity
class SecurityConfig {

    @Value("\${app.allowed-origin}")
    private lateinit var allowedOrigin: String

    @Bean
    fun filterChain(http: HttpSecurity): SecurityFilterChain {
        http {
            cors { }
            csrf { disable() }
            sessionManagement { sessionCreationPolicy = SessionCreationPolicy.STATELESS }
            formLogin { disable() }
            httpBasic { disable() }
            headers {
                contentSecurityPolicy {
                    policyDirectives = "default-src 'none'; connect-src $allowedOrigin; frame-ancestors 'none'"
                }
            }
            authorizeHttpRequests {
                authorize(anyRequest, permitAll)
            }
        }
        return http.build()
    }

    @Bean
    fun corsFilter(): CorsFilter {
        val filter = CorsFilter(corsConfigurationSource())
        filter.setCorsProcessor(LoggingCorsProcessor())
        return filter
    }

    @Bean
    fun passwordEncoder(): PasswordEncoder = Argon2PasswordEncoder.defaultsForSpringSecurity_v5_8()

    @Bean
    fun corsConfigurationSource(): CorsConfigurationSource {
        val config = CorsConfiguration()
        config.allowedOrigins = listOf(allowedOrigin)
        config.allowedMethods = listOf("GET", "POST", "OPTIONS")
        config.allowedHeaders = listOf("Content-Type")
        config.allowCredentials = true
        val source = UrlBasedCorsConfigurationSource()
        source.registerCorsConfiguration("/**", config)
        return source
    }

    private class LoggingCorsProcessor : DefaultCorsProcessor() {
        companion object {
            private val log = LoggerFactory.getLogger(LoggingCorsProcessor::class.java)
        }

        override fun processRequest(config: CorsConfiguration?, request: HttpServletRequest, response: HttpServletResponse): Boolean {
            val allowed = super.processRequest(config, request, response)
            if (!allowed) {
                log.warn("CORS rejection origin={} method={} uri={}", request.getHeader("Origin"), request.method, request.requestURI)
            }
            return allowed
        }
    }
}
