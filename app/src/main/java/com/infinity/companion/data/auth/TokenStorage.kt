package com.infinity.companion.data.auth

/**
 * Abstraction over secure token persistence so the repository can be unit-tested
 * without Android Keystore/EncryptedSharedPreferences.
 */
interface TokenStorage {
    var accessToken: String?
    var refreshToken: String?
    var userId: String?
    var userEmail: String?

    fun clear()
}
