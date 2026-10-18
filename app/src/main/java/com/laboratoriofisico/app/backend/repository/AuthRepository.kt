package com.laboratoriofisico.app.backend.repository

import android.database.sqlite.SQLiteConstraintException
import com.laboratoriofisico.app.backend.auth.AppRole
import com.laboratoriofisico.app.backend.auth.AuthMessages
import com.laboratoriofisico.app.backend.auth.AuthValidation
import com.laboratoriofisico.app.backend.auth.CuentaFirebase
import com.laboratoriofisico.app.backend.auth.CuentasFirebase
import com.laboratoriofisico.app.backend.auth.ResultadoCuentaFirebase
import com.laboratoriofisico.app.backend.auth.RolFirebase
import com.laboratoriofisico.app.backend.local.dao.UsuarioDao
import com.laboratoriofisico.app.backend.local.entity.UsuarioEntity
import com.laboratoriofisico.app.backend.session.AuthSessionManager
import com.laboratoriofisico.app.core.debug.DebugSessionLog

sealed class AuthResult {
    data class Success(val userId: Long) : AuthResult()
    data class NeedsPasswordReset(val userId: Long, val email: String) : AuthResult()
    data class Error(val message: String) : AuthResult()
}

/**
 * El login vive en Firebase: Authentication valida el correo y la contraseña,
 * Firestore guarda nombre, correo, rol (usuario o invitado) y fecha.
 * Room solo conserva el id local que el resto de la app ya usa.
 */
class AuthRepository(
    private val usuarioDao: UsuarioDao,
    private val sessionManager: AuthSessionManager,
    private val cuentas: CuentasFirebase
) {
    suspend fun login(email: String, password: String): AuthResult {
        val emailNormalizado = AuthValidation.normalizeEmail(email)

        AuthValidation.validateEmail(emailNormalizado)?.let {
            return AuthResult.Error(it)
        }
        if (password.isBlank()) {
            return AuthResult.Error("La contraseña es obligatoria")
        }

        return when (val remoto = cuentas.iniciarSesion(emailNormalizado, password)) {
            is ResultadoCuentaFirebase.Ok -> {
                if (remoto.cuenta.rol == RolFirebase.INVITADO) {
                    sessionManager.enterAsGuest()
                    return AuthResult.Success(0L)
                }
                val userId = guardarUsuarioLocal(remoto.cuenta)
                sessionManager.setLoggedInUser(userId)
                DebugSessionLog.log(
                    hypothesisId = "H-LOGIN-D",
                    location = "AuthRepository.login",
                    message = "login Firebase correcto",
                    data = mapOf("userId" to userId, "rol" to remoto.cuenta.rol),
                    runId = "firebase-login"
                )
                AuthResult.Success(userId)
            }
            is ResultadoCuentaFirebase.NecesitaContrasena -> {
                val userId = usuarioDao.findByEmail(emailNormalizado)?.id
                    ?: usuarioDao.insert(
                        UsuarioEntity(
                            nombre = nombreDesdeCorreo(emailNormalizado),
                            email = emailNormalizado,
                            rol = AppRole.REGISTRADO.storageValue,
                            fechaCreacion = System.currentTimeMillis(),
                            passwordHash = ""
                        )
                    )
                AuthResult.NeedsPasswordReset(userId, emailNormalizado)
            }
            is ResultadoCuentaFirebase.Error -> AuthResult.Error(remoto.message)
        }
    }

    suspend fun register(nombre: String, email: String, password: String): AuthResult {
        val nombreNormalizado = nombre.trim()
        val emailNormalizado = AuthValidation.normalizeEmail(email)

        AuthValidation.validateNombre(nombreNormalizado)?.let {
            return AuthResult.Error(it)
        }
        AuthValidation.validateEmail(emailNormalizado)?.let {
            return AuthResult.Error(it)
        }
        AuthValidation.validatePassword(password)?.let {
            return AuthResult.Error(it)
        }

        return when (
            val remoto = cuentas.registrar(nombreNormalizado, emailNormalizado, password)
        ) {
            is ResultadoCuentaFirebase.Ok -> {
                val userId = try {
                    guardarUsuarioLocal(remoto.cuenta.copy(nombre = nombreNormalizado))
                } catch (_: SQLiteConstraintException) {
                    return AuthResult.Error(AuthMessages.CORREO_YA_REGISTRADO)
                } catch (_: Exception) {
                    return AuthResult.Error(AuthMessages.ERROR_REGISTRO)
                }
                sessionManager.setLoggedInUser(userId)
                AuthResult.Success(userId)
            }
            is ResultadoCuentaFirebase.NecesitaContrasena ->
                AuthResult.Error(AuthMessages.CUENTA_SIN_CONTRASENA)
            is ResultadoCuentaFirebase.Error -> AuthResult.Error(remoto.message)
        }
    }

    suspend fun setNewPassword(userId: Long, password: String): AuthResult {
        AuthValidation.validatePassword(password)?.let {
            return AuthResult.Error(it)
        }
        val usuario = usuarioDao.getById(userId)
            ?: return AuthResult.Error(AuthMessages.CREDENCIALES_INVALIDAS)
        return when (val remoto = cuentas.establecerContrasena(usuario.email, password)) {
            is ResultadoCuentaFirebase.Ok -> {
                usuarioDao.updatePasswordHash(userId, MARCADOR_FIREBASE)
                sessionManager.setLoggedInUser(usuario.id)
                AuthResult.Success(usuario.id)
            }
            is ResultadoCuentaFirebase.NecesitaContrasena ->
                AuthResult.NeedsPasswordReset(userId, usuario.email)
            is ResultadoCuentaFirebase.Error -> AuthResult.Error(remoto.message)
        }
    }

    suspend fun enterAsGuest() {
        when (val remoto = cuentas.entrarComoInvitado()) {
            is ResultadoCuentaFirebase.Ok -> sessionManager.enterAsGuest()
            is ResultadoCuentaFirebase.NecesitaContrasena ->
                error(AuthMessages.ERROR_REGISTRO)
            is ResultadoCuentaFirebase.Error -> error(remoto.message)
        }
        DebugSessionLog.log(
            hypothesisId = "H-LOGIN-E",
            location = "AuthRepository.enterAsGuest",
            message = "sesión invitado en Firebase",
            data = mapOf("role" to RolFirebase.INVITADO),
            runId = "firebase-login"
        )
    }

    suspend fun logout() {
        cuentas.cerrarSesion()
        sessionManager.logout()
    }

    private suspend fun guardarUsuarioLocal(cuenta: CuentaFirebase): Long {
        val existente = usuarioDao.findByEmail(cuenta.email)
        if (existente != null) {
            usuarioDao.updateProfile(
                existente.id,
                cuenta.nombre,
                cuenta.email,
                AppRole.REGISTRADO.storageValue
            )
            if (existente.passwordHash.isBlank()) {
                usuarioDao.updatePasswordHash(existente.id, MARCADOR_FIREBASE)
            }
            return existente.id
        }
        return usuarioDao.insert(
            UsuarioEntity(
                nombre = cuenta.nombre,
                email = cuenta.email,
                rol = AppRole.REGISTRADO.storageValue,
                fechaCreacion = cuenta.fechaCreacion,
                passwordHash = MARCADOR_FIREBASE
            )
        )
    }

    private fun nombreDesdeCorreo(email: String): String =
        email.substringBefore("@").ifBlank { "Usuario" }

    private companion object {
        const val MARCADOR_FIREBASE = "firebase-auth"
    }
}
