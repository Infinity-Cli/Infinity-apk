package com.infinity.companion

import com.infinity.companion.data.auth.TokenStorage

/**
 * Volatile [TokenStorage] implementation for JVM unit tests.
 */
class InMemoryTokenStorage : TokenStorage {
    override var accessToken: String? = null
    override var refreshToken: String? = null
    override var userId: String? = null
    override var userEmail: String? = null

    override fun clear() {
        accessToken = null
        refreshToken = null
        userId = null
        userEmail = null
    }
}
