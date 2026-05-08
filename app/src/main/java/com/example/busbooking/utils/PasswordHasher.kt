package com.example.busbooking.utils

import at.favre.lib.crypto.bcrypt.BCrypt

object PasswordHasher {
    private const val COST = 12
    private const val BCRYPT_PREFIX = "${'$'}2"

    fun hash(password: String): String {
        return BCrypt.withDefaults().hashToString(COST, password.toCharArray())
    }

    fun verify(password: String, hashed: String): Boolean {
        return try {
            BCrypt.verifyer().verify(password.toCharArray(), hashed).verified
        } catch (e: IllegalArgumentException) {
            false
        }
    }

    fun isBcryptHash(value: String): Boolean {
        return value.startsWith(BCRYPT_PREFIX)
    }
}

