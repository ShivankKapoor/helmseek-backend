package com.shivankkapoor.helmseek_backend.dto.aldrop

import java.util.UUID

/** Request/response bodies for the aldrop auth service. Only the fields helmseek uses. */

data class AldropLoginRequest(
    val username: String,
    val password: String,
    val ipAddress: String?,
    val userAgent: String?
)

data class AldropLoginResponse(
    val token: String? = null,
    /** Always null for helmseek — its platform is provisioned with totpAvailable=false. */
    val totpToken: String? = null
)

data class AldropValidateRequest(
    val token: String,
    val ipAddress: String?,
    val userAgent: String?
)

data class AldropValidateResponse(
    val userId: UUID? = null,
    val username: String? = null
)

data class AldropLogoutRequest(val token: String, val ipAddress: String? = null)
