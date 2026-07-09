package com.infinity.companion.data.auth

import android.content.Context
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * Response from `GET /auth/google`.
 */
@Serializable
data class AuthStartResponse(
    val authUrl: String,
    val state: String
)

/**
 * User object returned by the Infinity-api auth callback.
 */
@Serializable
data class AuthUser(
    val id: String,
    val email: String
)

/**
 * Response from `GET /auth/callback`.
 *
 * The live Infinity-api returns a single `token` field. This class also accepts
 * the future `accessToken`/`refreshToken` shape so the repository can store both.
 */
@Serializable
data class AuthCallbackResponse(
    val token: String? = null,
    val accessToken: String? = null,
    val refreshToken: String? = null,
    val user: AuthUser
)

@Serializable
private data class RefreshRequest(val refreshToken: String)

@Serializable
private data class RefreshResponse(
    val token: String? = null,
    val accessToken: String? = null,
    val refreshToken: String? = null
)

/**
 * Repository that coordinates Supabase/Google sign-in through the Infinity-api
 * endpoints and persists the resulting tokens securely.
 */
class AuthRepository(
    private val tokenStorage: TokenStorage,
    private val httpClient: HttpClient,
    private val baseUrl: String
) {

    constructor(
        context: Context,
        baseUrl: String = DEFAULT_BASE_URL
    ) : this(
        tokenStorage = SecureTokenStorage(context),
        httpClient = HttpClient(CIO) {
            install(ContentNegotiation) {
                json(Json { ignoreUnknownKeys = true })
            }
        },
        baseUrl = baseUrl
    )

    /**
     * Starts a Google OAuth sign-in by asking the Infinity-api for an auth URL
     * and signed state. A future UI layer can launch [AuthStartResponse.authUrl]
     * in a Chrome Custom Tab/WebView.
     */
    suspend fun startGoogleSignIn(): Result<AuthStartResponse> = runCatching {
        val response = httpClient.get("$baseUrl/auth/google")
        response.throwOnError()
        response.body<AuthStartResponse>()
    }

    /**
     * Exchanges the Google authorization `code` and `state` for Infinity tokens.
     *
     * The tokens are persisted in [tokenStorage] and the authenticated user is
     * returned on success.
     */
    suspend fun handleCallback(code: String, state: String): Result<AuthUser> = runCatching {
        val response = httpClient.get("$baseUrl/auth/callback") {
            parameter("code", code)
            parameter("state", state)
        }
        response.throwOnError()
        val body = response.body<AuthCallbackResponse>()
        val accessToken = body.accessToken ?: body.token
            ?: error("No access token in callback response")

        tokenStorage.accessToken = accessToken
        tokenStorage.refreshToken = body.refreshToken
        tokenStorage.userId = body.user.id
        tokenStorage.userEmail = body.user.email

        body.user
    }

    /**
     * Refreshes the access token using the stored refresh token.
     */
    suspend fun refreshToken(): Result<String> = runCatching {
        val refreshToken = tokenStorage.refreshToken
            ?: error("No refresh token available")

        val response = httpClient.post("$baseUrl/auth/refresh") {
            contentType(ContentType.Application.Json)
            setBody(RefreshRequest(refreshToken))
        }
        response.throwOnError()
        val refreshed = response.body<RefreshResponse>()
        val newAccessToken = refreshed.accessToken ?: refreshed.token
            ?: error("No token in refresh response")

        tokenStorage.accessToken = newAccessToken
        refreshed.accessToken?.let { tokenStorage.accessToken = it }
        refreshed.refreshToken?.let { tokenStorage.refreshToken = it }

        newAccessToken
    }

    /**
     * Ktor-style HTTP error check that throws on non-2xx responses.
     */
    private fun HttpResponse.throwOnError(): HttpResponse {
        if (!status.isSuccess()) {
            error("Request failed with HTTP ${status.value}")
        }
        return this
    }

    /**
     * Returns the current access token, or null if not yet signed in.
     * Used by [PairingRepository] and [RelaySocketManager] to authenticate
     * HTTP and WebSocket requests.
     */
    fun getAccessToken(): String? = tokenStorage.accessToken

    companion object {
        const val DEFAULT_BASE_URL = "[REDACTED-IP_ADDRESS]:3000"
    }
}