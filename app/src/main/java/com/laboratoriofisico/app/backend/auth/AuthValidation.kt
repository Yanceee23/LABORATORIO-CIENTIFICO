package com.laboratoriofisico.app.backend.auth

object AuthValidation {
    const val MIN_PASSWORD_LENGTH = 6

    private val EMAIL_REGEX = Regex("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")

    fun normalizeEmail(email: String): String = email.trim().lowercase()

    fun validateEmail(email: String): String? {
        val normalized = normalizeEmail(email)
        return when {
            normalized.isBlank() -> "El correo es obligatorio"
            !EMAIL_REGEX.matches(normalized) -> "Ingresa un correo válido (ejemplo: usuario@dominio.com)"
            else -> null
        }
    }

    fun validatePassword(password: String): String? = when {
        password.isBlank() -> "La contraseña es obligatoria"
        password.length < MIN_PASSWORD_LENGTH ->
            "La contraseña debe tener al menos $MIN_PASSWORD_LENGTH caracteres"
        else -> null
    }

    fun validateNombre(nombre: String): String? = when {
        nombre.isBlank() -> "El nombre es obligatorio"
        else -> null
    }

    fun validateConfirmPassword(password: String, confirmar: String): String? = when {
        confirmar.isBlank() -> "Confirma tu contraseña"
        password != confirmar -> "Las contraseñas no coinciden"
        else -> null
    }
}

object AuthMessages {
    /** Mensaje genérico a propósito: no revela si falló el correo o la contraseña. */
    const val CREDENCIALES_INVALIDAS = "Usuario o credencial incorrecta"
    const val CUENTA_SIN_CONTRASENA = "Esta cuenta necesita una contraseña nueva"
    const val CORREO_YA_REGISTRADO = "Este correo ya está registrado"
    const val CAMPOS_OBLIGATORIOS = "Completa todos los campos obligatorios"
    const val ERROR_REGISTRO = "No se pudo crear la cuenta. Intenta de nuevo."
    const val INVITADO_BLOQUEADO = "Esta función requiere una cuenta. Crea una cuenta o inicia sesión."
}
