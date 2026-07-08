package com.infinity.companion

import com.infinity.companion.data.auth.AuthRepository
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AuthRepositoryTest {

    private val json = Json { ignoreUnknownKeys = true }

    private fun createRepository(
        engine: MockEngine,
        storage: InMemoryTokenStorage = InMemoryTokenStorage()
    ): AuthRepository {
        val client = HttpClient(engine) {
            install(ContentNegotiation) { json(json) }
        }
        return AuthRepository(storage, client, "https://api.example.com")
    }

    @Test
    fun startGoogleSignIn_returnsAuthUrlAndState() = runTest {
        val engine = MockEngine { request ->
            if (request.url.encodedPath == "/auth/google") {
                respond(
                    content = """{"authUrl":"https://accounts.google.com/oauth?client=test","state":"signed-state-123"}""",
                    status = HttpStatusCode.OK,
                    headers = headersOf("Content-Type", ContentType.Application.Json.toString())
                )
            } else {
                respond("Not found", HttpStatusCode.NotFound)
            }
        }

        val repository = createRepository(engine)
        val result = repository.startGoogleSignIn()

        assertTrue(result.isSuccess)
        val response = result.getOrThrow()
        assertEquals("https://accounts.google.com/oauth?client=test", response.authUrl)
        assertEquals("signed-state-123", response.state)
    }

    @Test
    fun handleCallback_storesTokenAndReturnsUser() = runTest {
        val engine = MockEngine { request ->
            if (request.url.encodedPath == "/auth/callback") {
                respond(
                    content = """{"token":"infinity-jwt","user":{"id":"user-1","email":"alice@example.com"}}""",
                    status = HttpStatusCode.OK,
                    headers = headersOf("Content-Type", ContentType.Application.Json.toString())
                )
            } else {
                respond("Not found", HttpStatusCode.NotFound)
            }
        }

        val storage = InMemoryTokenStorage()
        val repository = createRepository(engine, storage)
        val result = repository.handleCallback("auth-code", "signed-state-123")

        assertTrue(result.isSuccess)
        val user = result.getOrThrow()
        assertEquals("user-1", user.id)
        assertEquals("alice@example.com", user.email)
        assertEquals("infinity-jwt", storage.accessToken)
        assertEquals("alice@example.com", storage.userEmail)
        assertTrue(repository.isAuthenticated())
    }

    @Test
    fun handleCallback_withAccessAndRefreshTokens_storesBoth() = runTest {
        val engine = MockEngine { request ->
            if (request.url.encodedPath == "/auth/callback") {
                respond(
                    content = """{"accessToken":"access-1","refreshToken":"refresh-1","user":{"id":"user-2","email":"bob@example.com"}}""",
                    status = HttpStatusCode.OK,
                    headers = headersOf("Content-Type", ContentType.Application.Json.toString())
                )
            } else {
                respond("Not found", HttpStatusCode.NotFound)
            }
        }

        val storage = InMemoryTokenStorage()
        val repository = createRepository(engine, storage)
        repository.handleCallback("auth-code", "signed-state-123")

        assertEquals("access-1", storage.accessToken)
        assertEquals("refresh-1", storage.refreshToken)
    }

    @Test
    fun refreshTokens_postsRefreshTokenAndStoresNewTokens() = runTest {
        val engine = MockEngine { request ->
            when (request.url.encodedPath) {
                "/auth/refresh" -> {
                    respond(
                        content = """{"accessToken":"new-access","refreshToken":"new-refresh"}""",
                        status = HttpStatusCode.OK,
                        headers = headersOf("Content-Type", ContentType.Application.Json.toString())
                    )
                }
                else -> respond("Not found", HttpStatusCode.NotFound)
            }
        }

        val storage = InMemoryTokenStorage().apply { refreshToken = "old-refresh" }
        val repository = createRepository(engine, storage)
        val result = repository.refreshTokens()

        assertTrue(result.isSuccess)
        assertEquals("new-access", storage.accessToken)
        assertEquals("new-refresh", storage.refreshToken)
    }

    @Test
    fun refreshTokens_withoutStoredToken_fails() = runTest {
        val engine = MockEngine {
            respond("Bad request", HttpStatusCode.BadRequest)
        }

        val repository = createRepository(engine)
        val result = repository.refreshTokens()

        assertTrue(result.isFailure)
        assertFalse(repository.isAuthenticated())
    }

    @Test
    fun clearAuth_clearsStoredTokens() = runTest {
        val storage = InMemoryTokenStorage().apply {
            accessToken = "access"
            refreshToken = "refresh"
        }
        val engine = MockEngine { respond("", HttpStatusCode.OK) }
        val repository = createRepository(engine, storage)

        repository.clearAuth()

        assertEquals(null, storage.accessToken)
        assertEquals(null, storage.refreshToken)
        assertFalse(repository.isAuthenticated())
    }
}
