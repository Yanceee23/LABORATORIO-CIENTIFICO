package com.laboratoriofisico.app.backend.auth

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AuthValidationTest {

    @Test
    fun validateEmail_rejectsInvalidFormats() {
        assertTrue(AuthValidation.validateEmail("") != null)
        assertTrue(AuthValidation.validateEmail("sinarroba") != null)
        assertTrue(AuthValidation.validateEmail("a@b") != null)
        assertNull(AuthValidation.validateEmail("usuario@dominio.com"))
    }

    @Test
    fun validatePassword_requiresMinimumLength() {
        assertTrue(AuthValidation.validatePassword("123") != null)
        assertNull(AuthValidation.validatePassword("123456"))
    }

    @Test
    fun authMessages_credentialErrorIsGeneric() {
        assertEquals("Usuario o credencial incorrecta", AuthMessages.CREDENCIALES_INVALIDAS)
    }

    @Test
    fun authMessages_emptyHashNeedsNewPassword() {
        assertEquals("Esta cuenta necesita una contraseña nueva", AuthMessages.CUENTA_SIN_CONTRASENA)
    }

    @Test
    fun authMessages_invitadoBlocked() {
        assertEquals(
            "Esta función requiere una cuenta. Crea una cuenta o inicia sesión.",
            AuthMessages.INVITADO_BLOQUEADO
        )
    }

    @Test
    fun normalizeEmail_lowercases() {
        assertEquals("user@mail.com", AuthValidation.normalizeEmail("  User@Mail.COM "))
    }
}

class PasswordHasherTest {

    @Test
    fun verify_blankHash_returnsFalseWithoutCrash() {
        assertFalse(PasswordHasher.verify("cualquier", ""))
        assertFalse(PasswordHasher.verify("cualquier", "   "))
    }

    @Test
    fun hashAndVerify_roundTrip() {
        val hash = PasswordHasher.hash("miClave123")
        assertTrue(PasswordHasher.verify("miClave123", hash))
        assertFalse(PasswordHasher.verify("otraClave", hash))
    }
}
