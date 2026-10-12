package com.laboratoriofisico.app.backend.repository

import android.database.sqlite.SQLiteConstraintException
import com.laboratoriofisico.app.backend.auth.AppRole
import com.laboratoriofisico.app.backend.auth.AuthMessages
import com.laboratoriofisico.app.backend.auth.AuthValidation
import com.laboratoriofisico.app.backend.auth.PasswordHasher
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
 * Autenticación contra la tabla Room `usuarios` (SQLite real).
 * Invitado: solo sesión, sin fila en la base.
 */
class AuthRepository(
    private val usuarioDao: UsuarioDao,
    private val sessionManager: AuthSessionManager
) {
    suspend fun login(email: String, password: String): AuthResult {
        val emailNormalizado = AuthValidation.normalizeEmail(email)

        AuthValidation.validateEmail(emailNormalizado)?.let {
            return AuthResult.Error(it)
        }
        if (password.isBlank()) {
            return AuthResult.Error("La contraseña es obligatoria")
        }

        val usuario = usuarioDao.findByEmail(emailNormalizado)
        // #region agent log
        DebugSessionLog.log(
            hypothesisId = "H-LOGIN-A",
            location = "AuthRepository.login",
            message = "resultado búsqueda usuario",
            data = mapOf(
                "emailNorm" to emailNormalizado,
                "found" to (usuario != null),
                "hashBlank" to (usuario?.passwordHash?.isBlank() == true),
                "userId" to (usuario?.id ?: -1L)
            ),
            runId = "roles-pre"
        )
        // #endregion
        if (usuario == null) {
            return AuthResult.Error(AuthMessages.CREDENCIALES_INVALIDAS)
        }

        if (usuario.passwordHash.isBlank()) {
            // #region agent log
            DebugSessionLog.log(
                hypothesisId = "H-LOGIN-B",
                location = "AuthRepository.login",
                message = "cuenta sin hash de contraseña",
                data = mapOf("userId" to usuario.id),
                runId = "roles-pre"
            )
            // #endregion
            return AuthResult.NeedsPasswordReset(usuario.id, usuario.email)
        }

        if (!PasswordHasher.verify(password, usuario.passwordHash)) {
            // #region agent log
            DebugSessionLog.log(
                hypothesisId = "H-LOGIN-C",
                location = "AuthRepository.login",
                message = "hash no coincide",
                data = mapOf("userId" to usuario.id),
                runId = "roles-pre"
            )
            // #endregion
            return AuthResult.Error(AuthMessages.CREDENCIALES_INVALIDAS)
        }

        // Si ya hay sesión del mismo usuario, se reutiliza (sin bloquear).
        sessionManager.setLoggedInUser(usuario.id)
        // #region agent log
        DebugSessionLog.log(
            hypothesisId = "H-LOGIN-D",
            location = "AuthRepository.login",
            message = "login correcto; sesión establecida",
            data = mapOf("userId" to usuario.id),
            runId = "roles-pre"
        )
        // #endregion
        return AuthResult.Success(usuario.id)
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

        if (usuarioDao.findByEmail(emailNormalizado) != null) {
            return AuthResult.Error(AuthMessages.CORREO_YA_REGISTRADO)
        }

        val userId = try {
            usuarioDao.insert(
                UsuarioEntity(
                    nombre = nombreNormalizado,
                    email = emailNormalizado,
                    rol = AppRole.REGISTRADO.storageValue,
                    fechaCreacion = System.currentTimeMillis(),
                    passwordHash = PasswordHasher.hash(password)
                )
            )
        } catch (_: SQLiteConstraintException) {
            return AuthResult.Error(AuthMessages.CORREO_YA_REGISTRADO)
        } catch (_: Exception) {
            return AuthResult.Error(AuthMessages.ERROR_REGISTRO)
        }

        sessionManager.setLoggedInUser(userId)
        return AuthResult.Success(userId)
    }

    suspend fun setNewPassword(userId: Long, password: String): AuthResult {
        AuthValidation.validatePassword(password)?.let {
            return AuthResult.Error(it)
        }
        val usuario = usuarioDao.getById(userId)
            ?: return AuthResult.Error(AuthMessages.CREDENCIALES_INVALIDAS)
        usuarioDao.updatePasswordHash(userId, PasswordHasher.hash(password))
        sessionManager.setLoggedInUser(usuario.id)
        return AuthResult.Success(usuario.id)
    }

    suspend fun enterAsGuest() {
        sessionManager.enterAsGuest()
        // #region agent log
        DebugSessionLog.log(
            hypothesisId = "H-LOGIN-E",
            location = "AuthRepository.enterAsGuest",
            message = "sesión invitado activa",
            data = mapOf("role" to AppRole.INVITADO.storageValue),
            runId = "roles-pre"
        )
        // #endregion
    }

    suspend fun logout() {
        sessionManager.logout()
    }
}
