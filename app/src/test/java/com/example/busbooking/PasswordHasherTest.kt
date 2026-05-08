package com.example.busbooking

import com.example.busbooking.utils.PasswordHasher
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PasswordHasherTest {
    @Test
    fun hashAndVerify() {
        val password = "P@ssw0rd!"
        val hash = PasswordHasher.hash(password)

        assertTrue(PasswordHasher.isBcryptHash(hash))
        assertTrue(PasswordHasher.verify(password, hash))
        assertFalse(PasswordHasher.verify("wrong", hash))
    }
}

