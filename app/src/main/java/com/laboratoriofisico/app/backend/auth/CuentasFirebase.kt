package com.laboratoriofisico.app.backend.auth

/**
 * Persona guardada en Firestore, colección `usuarios`.
 * La contraseña no se guarda aquí: la tiene Firebase Authentication.
 */
data class CuentaFirebase(
    val uid: String,
    val nombre: String,
    val email: String,
    val rol: String,
    val fechaCreacion: Long
)

sealed class ResultadoCuentaFirebase {
    data class Ok(val cuenta: CuentaFirebase) : ResultadoCuentaFirebase()
    data class NecesitaContrasena(val uid: String, val email: String) : ResultadoCuentaFirebase()
    data class Error(val message: String) : ResultadoCuentaFirebase()
}

object RolFirebase {
    const val COLECCION = "usuarios"
    const val USUARIO = "usuario"
    const val INVITADO = "invitado"
}

/**
 * Login, registro e invitado. La implementación real escribe en Firebase.
 */
interface CuentasFirebase {
    suspend fun iniciarSesion(email: String, password: String): ResultadoCuentaFirebase

    suspend fun registrar(nombre: String, email: String, password: String): ResultadoCuentaFirebase

    suspend fun entrarComoInvitado(): ResultadoCuentaFirebase

    suspend fun establecerContrasena(email: String, password: String): ResultadoCuentaFirebase

    suspend fun cerrarSesion()
}
