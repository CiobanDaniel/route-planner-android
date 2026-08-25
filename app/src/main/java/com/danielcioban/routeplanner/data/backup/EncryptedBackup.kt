package com.danielcioban.routeplanner.data.backup

import java.security.NoSuchAlgorithmException
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

/** Password-wrapped JSON backup. Magic `RPENC1` + salt + IV + AES-GCM ciphertext. */
object EncryptedBackup {
    const val MAGIC = "RPENC1"
    private const val SALT_LEN = 16
    private const val IV_LEN = 12
    private const val ITERATIONS = 120_000
    private const val KEY_BITS = 256
    private const val GCM_TAG_BITS = 128

    fun isEncrypted(bytes: ByteArray): Boolean {
        if (bytes.size < MAGIC.length) return false
        return bytes.copyOfRange(0, MAGIC.length).toString(Charsets.US_ASCII) == MAGIC
    }

    fun isEncrypted(text: String): Boolean = text.startsWith(MAGIC)

    fun encrypt(plainJson: String, password: String): ByteArray {
        require(password.isNotEmpty()) { "Password required" }
        val salt = ByteArray(SALT_LEN).also { SecureRandom().nextBytes(it) }
        val iv = ByteArray(IV_LEN).also { SecureRandom().nextBytes(it) }
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(
            Cipher.ENCRYPT_MODE,
            key(password, salt, preferredKdf()),
            GCMParameterSpec(GCM_TAG_BITS, iv),
        )
        val cipherText = cipher.doFinal(plainJson.toByteArray(Charsets.UTF_8))
        return MAGIC.toByteArray(Charsets.US_ASCII) + salt + iv + cipherText
    }

    fun decrypt(bytes: ByteArray, password: String): String {
        require(isEncrypted(bytes)) { "Not an encrypted backup" }
        require(password.isNotEmpty()) { "Password required" }
        val offset = MAGIC.length
        require(bytes.size > offset + SALT_LEN + IV_LEN) { "Truncated backup" }
        val salt = bytes.copyOfRange(offset, offset + SALT_LEN)
        val iv = bytes.copyOfRange(offset + SALT_LEN, offset + SALT_LEN + IV_LEN)
        val cipherText = bytes.copyOfRange(offset + SALT_LEN + IV_LEN, bytes.size)
        var last: Exception? = null
        for (algorithm in KDF_CANDIDATES) {
            try {
                val cipher = Cipher.getInstance("AES/GCM/NoPadding")
                cipher.init(
                    Cipher.DECRYPT_MODE,
                    key(password, salt, algorithm),
                    GCMParameterSpec(GCM_TAG_BITS, iv),
                )
                return cipher.doFinal(cipherText).toString(Charsets.UTF_8)
            } catch (error: Exception) {
                last = error
            }
        }
        throw last ?: IllegalStateException("Decrypt failed")
    }

    private fun key(password: String, salt: ByteArray, algorithm: String): SecretKeySpec {
        val spec = PBEKeySpec(password.toCharArray(), salt, ITERATIONS, KEY_BITS)
        val factory = SecretKeyFactory.getInstance(algorithm)
        return SecretKeySpec(factory.generateSecret(spec).encoded, "AES")
    }

    private fun preferredKdf(): String {
        return try {
            SecretKeyFactory.getInstance(KDF_SHA256)
            KDF_SHA256
        } catch (_: NoSuchAlgorithmException) {
            KDF_SHA1
        }
    }

    private const val KDF_SHA256 = "PBKDF2WithHmacSHA256"
    private const val KDF_SHA1 = "PBKDF2WithHmacSHA1"
    private val KDF_CANDIDATES = listOf(KDF_SHA256, KDF_SHA1)
}
