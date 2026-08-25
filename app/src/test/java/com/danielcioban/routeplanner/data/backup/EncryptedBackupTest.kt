package com.danielcioban.routeplanner.data.backup

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class EncryptedBackupTest {
    @Test
    fun roundTrip_preservesJson() {
        val json = """{"formatVersion":6,"name":"Driver"}"""
        val bytes = EncryptedBackup.encrypt(json, "secret-pass")
        assertTrue(EncryptedBackup.isEncrypted(bytes))
        assertFalse(EncryptedBackup.isEncrypted("{".toByteArray()))
        assertEquals(json, EncryptedBackup.decrypt(bytes, "secret-pass"))
    }

    @Test(expected = Exception::class)
    fun wrongPassword_fails() {
        val bytes = EncryptedBackup.encrypt("{\"a\":1}", "right")
        EncryptedBackup.decrypt(bytes, "wrong")
    }
}
