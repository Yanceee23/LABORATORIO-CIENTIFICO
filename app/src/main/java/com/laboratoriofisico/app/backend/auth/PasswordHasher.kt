package com.laboratoriofisico.app.backend.auth

import at.favre.lib.crypto.bcrypt.BCrypt

object PasswordHasher {
    private const val COST = 12

    fun hash(password: String): String =
        BCrypt.withDefaults().hashToString(COST, password.toCharArray())

    fun verify(password: String, passwordHash: String): Boolean {
        if (passwordHash.isBlank()) return false
        return BCrypt.verifyer().verify(password.toCharArray(), passwordHash).verified
    }
}
