package com.infinity.companion.data.crypto

import java.security.KeyFactory
import java.security.KeyPair
import java.security.PublicKey
import java.security.spec.X509EncodedKeySpec
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * Hybrid E2EE payload exchanged through the Infinity-api relay.
 *
 * Mirrors the `HybridCipher` shape in Infinity-api/src/ws/e2ee.test.ts.
 */
@Serializable
data class EncryptedPayload(
    val iv: String,
    val ciphertext: String,
    val encryptedKey: String
)

/**
 * End-to-end encryption helper for the Infinity Android companion.
 *
 * The Android key pair is backed by [KeystoreCrypto] (and therefore the
 * Android Keystore when available). Outgoing messages are hybrid-encrypted
 * with RSA-OAEP + AES-GCM so they can only be read by the paired desktop
 * client.
 */
class E2EECrypto private constructor(private val keystoreCrypto: KeystoreCrypto) {

    /**
     * Returns this device's public key as a PEM-encoded RSA public key.
     */
    fun getPublicKeyPem(): String {
        val base64 = keystoreCrypto.getPublicKeyBase64()
        return buildString {
            appendLine("-----BEGIN PUBLIC KEY-----")
            appendLine(base64)
            append("-----END PUBLIC KEY-----")
        }
    }

    /**
     * Returns this device's public key as a Base64-encoded SPKI string.
     */
    fun getPublicKeyBase64(): String = keystoreCrypto.getPublicKeyBase64()

    /**
     * Decrypts an [EncryptedPayload] that was sent to this device.
     */
    fun decrypt(payload: EncryptedPayload): String {
        val encryptedAesKey = Base64.getDecoder().decode(payload.encryptedKey)
        val aesKeyBytes = keystoreCrypto.decrypt(encryptedAesKey)
        val aesKey = restoreAesKey(aesKeyBytes)

        val iv = Base64.getDecoder().decode(payload.iv)
        val ciphertext = Base64.getDecoder().decode(payload.ciphertext)

        val cipher = Cipher.getInstance(AES_TRANSFORM)
        cipher.init(Cipher.DECRYPT_MODE, aesKey, GCMParameterSpec(GCM_TAG_LENGTH, iv))
        val plaintext = cipher.doFinal(ciphertext)
        return String(plaintext, Charsets.UTF_8)
    }

    /**
     * Hybrid-encrypts [plaintext] for the paired desktop's public key.
     *
     * [publicKeyPemOrBase64] may be a PEM-encoded public key or a raw Base64
     * SPKI string.
     */
    fun encrypt(plaintext: String, publicKeyPemOrBase64: String): EncryptedPayload {
        val publicKey = parsePublicKey(publicKeyPemOrBase64)

        val aesKey = generateAesKey()
        val iv = ByteArray(IV_LENGTH).also { java.security.SecureRandom().nextBytes(it) }

        val cipher = Cipher.getInstance(AES_TRANSFORM)
        cipher.init(Cipher.ENCRYPT_MODE, aesKey, GCMParameterSpec(GCM_TAG_LENGTH, iv))
        val ciphertext = cipher.doFinal(plaintext.toByteArray(Charsets.UTF_8))

        val rsaCipher = Cipher.getInstance(RSA_TRANSFORM)
        rsaCipher.init(Cipher.ENCRYPT_MODE, publicKey)
        val encryptedKey = rsaCipher.doFinal(aesKey.encoded)

        return EncryptedPayload(
            iv = Base64.getEncoder().encodeToString(iv),
            ciphertext = Base64.getEncoder().encodeToString(ciphertext),
            encryptedKey = Base64.getEncoder().encodeToString(encryptedKey)
        )
    }

    companion object {
        private const val AES_ALGORITHM = "AES"
        private const val AES_TRANSFORM = "AES/GCM/NoPadding"
        private const val RSA_TRANSFORM = "RSA/ECB/OAEPWithSHA-256AndMGF1Padding"
        private const val IV_LENGTH = 12
        private const val GCM_TAG_LENGTH = 128

        /**
         * Creates an [E2EECrypto] backed by the Android Keystore.
         */
        @JvmStatic
        fun create(): E2EECrypto {
            return E2EECrypto(KeystoreCrypto.create())
        }

        /**
         * Test-only factory that uses an in-memory RSA key pair.
         */
        @JvmStatic
        fun createWithKeyPair(keyPair: KeyPair): E2EECrypto {
            return E2EECrypto(KeystoreCrypto.createWithKeyPair(keyPair))
        }

        private fun generateAesKey(): SecretKey {
            val generator = KeyGenerator.getInstance(AES_ALGORITHM)
            generator.init(256)
            return generator.generateKey()
        }

        private fun restoreAesKey(bytes: ByteArray): SecretKey {
            return SecretKeySpec(bytes, AES_ALGORITHM)
        }

        private fun parsePublicKey(input: String): PublicKey {
            val base64 = input
                .replace("-----BEGIN PUBLIC KEY-----", "")
                .replace("-----END PUBLIC KEY-----", "")
                .replace(Regex("\\s+"), "")
                .trim()
            val decoded = Base64.getDecoder().decode(base64)
            val spec = X509EncodedKeySpec(decoded)
            return KeyFactory.getInstance("RSA").generatePublic(spec)
        }
    }
}
