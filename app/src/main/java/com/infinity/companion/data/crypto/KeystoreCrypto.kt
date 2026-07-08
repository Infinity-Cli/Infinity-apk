package com.infinity.companion.data.crypto

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.KeyStore
import java.security.Signature
import java.util.Base64
import javax.crypto.Cipher

/**
 * Hardware-backed (when available) RSA crypto helper backed by the Android Keystore.
 *
 * The companion object provides a production factory backed by the
 * `AndroidKeyStore`, plus a test-only factory that accepts an in-memory key pair.
 */
class KeystoreCrypto private constructor(private val keyPair: KeyPair) {

    /**
     * Returns the public key encoded as a Base64 SPKI string.
     */
    fun getPublicKeyBase64(): String {
        return Base64.getEncoder().encodeToString(keyPair.public.encoded)
    }

    /**
     * Returns the public key as a PEM-encoded RSA public key.
     */
    fun getPublicKeyPem(): String {
        val base64 = getPublicKeyBase64()
        return buildString {
            appendLine("-----BEGIN PUBLIC KEY-----")
            appendLine(base64)
            append("-----END PUBLIC KEY-----")
        }
    }

    /**
     * Encrypts [plainBytes] with RSA-OAEP (SHA-256).
     */
    fun encrypt(plainBytes: ByteArray): ByteArray {
        val cipher = Cipher.getInstance(TRANSFORM_ENCRYPT)
        cipher.init(Cipher.ENCRYPT_MODE, keyPair.public)
        return cipher.doFinal(plainBytes)
    }

    /**
     * Decrypts [cipherBytes] with the Android Keystore private key.
     */
    fun decrypt(cipherBytes: ByteArray): ByteArray {
        val cipher = Cipher.getInstance(TRANSFORM_ENCRYPT)
        cipher.init(Cipher.DECRYPT_MODE, keyPair.private)
        return cipher.doFinal(cipherBytes)
    }

    /**
     * Signs [data] with RSA PKCS#1 v1.5 and SHA-256.
     */
    fun sign(data: ByteArray): ByteArray {
        val signature = Signature.getInstance(SIGNATURE_ALGORITHM)
        signature.initSign(keyPair.private)
        signature.update(data)
        return signature.sign()
    }

    /**
     * Verifies [signatureBytes] over [data] with the public key.
     */
    fun verify(data: ByteArray, signatureBytes: ByteArray): Boolean {
        val signature = Signature.getInstance(SIGNATURE_ALGORITHM)
        signature.initVerify(keyPair.public)
        signature.update(data)
        return signature.verify(signatureBytes)
    }

    companion object {
        private const val KEY_ALIAS = "infinity_rsa_key"
        private const val ANDROID_KEYSTORE = "AndroidKeyStore"
        private const val TRANSFORM_ENCRYPT = "RSA/ECB/OAEPWithSHA-256AndMGF1Padding"
        private const val SIGNATURE_ALGORITHM = "SHA256withRSA"

        /**
         * Creates or loads an RSA key pair in the Android Keystore.
         */
        @JvmStatic
        fun create(): KeystoreCrypto {
            val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
            val entry = keyStore.getEntry(KEY_ALIAS, null) as? KeyStore.PrivateKeyEntry
            val keyPair = if (entry != null) {
                KeyPair(entry.certificate.publicKey, entry.privateKey)
            } else {
                generateKeyPair()
            }
            return KeystoreCrypto(keyPair)
        }

        /**
         * Test-only factory that bypasses the Android Keystore.
         */
        @JvmStatic
        fun createWithKeyPair(keyPair: KeyPair): KeystoreCrypto {
            return KeystoreCrypto(keyPair)
        }

        private fun generateKeyPair(): KeyPair {
            val generator = KeyPairGenerator.getInstance("RSA", ANDROID_KEYSTORE)
            val purposes = KeyProperties.PURPOSE_DECRYPT or
                    KeyProperties.PURPOSE_SIGN or
                    KeyProperties.PURPOSE_VERIFY
            val spec = KeyGenParameterSpec.Builder(KEY_ALIAS, purposes)
                .setKeySize(2048)
                .setDigests(KeyProperties.DIGEST_SHA256)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_RSA_OAEP)
                .setSignaturePaddings(KeyProperties.SIGNATURE_PADDING_RSA_PKCS1)
                .setUserAuthenticationRequired(false)
                .build()
            generator.initialize(spec)
            return generator.generateKeyPair()
        }
    }
}
