package com.infinity.companion

import com.infinity.companion.data.crypto.KeystoreCrypto
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.security.KeyPairGenerator
import java.util.Base64

class KeystoreCryptoTest {

    private fun createCrypto(): KeystoreCrypto {
        val generator = KeyPairGenerator.getInstance("RSA")
        generator.initialize(2048)
        return KeystoreCrypto.createWithKeyPair(generator.generateKeyPair())
    }

    @Test
    fun getPublicKeyBase64_returnsNonEmptySpki() {
        val crypto = createCrypto()
        val base64 = crypto.getPublicKeyBase64()

        assertTrue("Public key Base64 should not be blank", base64.isNotBlank())
        val decoded = Base64.getDecoder().decode(base64)
        assertTrue("Decoded SPKI should not be empty", decoded.isNotEmpty())
    }

    @Test
    fun encryptDecrypt_roundTrip() {
        val crypto = createCrypto()
        val plain = "hello infinity keystore".toByteArray(Charsets.UTF_8)

        val encrypted = crypto.encrypt(plain)
        assertFalse("Encrypted bytes should differ from plain bytes", encrypted.contentEquals(plain))

        val decrypted = crypto.decrypt(encrypted)
        assertArrayEquals(plain, decrypted)
    }

    @Test
    fun signVerify_roundTrip() {
        val crypto = createCrypto()
        val data = "data to sign".toByteArray(Charsets.UTF_8)

        val signature = crypto.sign(data)
        assertTrue("Signature should not be empty", signature.isNotEmpty())
        assertTrue("Signature should verify", crypto.verify(data, signature))
    }

    @Test
    fun verify_withTamperedData_returnsFalse() {
        val crypto = createCrypto()
        val data = "original data".toByteArray(Charsets.UTF_8)
        val signature = crypto.sign(data)

        val tampered = "tampered data".toByteArray(Charsets.UTF_8)
        assertFalse(crypto.verify(tampered, signature))
    }
}
