package com.laboratoriofisico.app.backend.auth

import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.tasks.await

/**
 * Auth guarda el correo y la contraseña.
 * Firestore guarda nombre, correo, rol (usuario o invitado), fecha y uid.
 */
class FirebaseCuentas(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) : CuentasFirebase {

    override suspend fun iniciarSesion(email: String, password: String): ResultadoCuentaFirebase {
        return try {
            val result = auth.signInWithEmailAndPassword(email, password).await()
            val user = result.user ?: return ResultadoCuentaFirebase.Error(AuthMessages.CREDENCIALES_INVALIDAS)
            if (!user.tieneContrasena()) {
                return ResultadoCuentaFirebase.NecesitaContrasena(user.uid, email)
            }
            ResultadoCuentaFirebase.Ok(
                leerOCrear(
                    uid = user.uid,
                    nombre = user.displayName?.takeIf { it.isNotBlank() } ?: nombreDesdeCorreo(email),
                    email = email,
                    rol = RolFirebase.USUARIO
                )
            )
        } catch (error: Exception) {
            ResultadoCuentaFirebase.Error(mensaje(error, registro = false))
        }
    }

    override suspend fun registrar(nombre: String, email: String, password: String): ResultadoCuentaFirebase {
        return try {
            val result = auth.createUserWithEmailAndPassword(email, password).await()
            val user = result.user ?: return ResultadoCuentaFirebase.Error(AuthMessages.ERROR_REGISTRO)
            ResultadoCuentaFirebase.Ok(
                guardar(
                    uid = user.uid,
                    nombre = nombre,
                    email = email,
                    rol = RolFirebase.USUARIO,
                    fechaCreacion = System.currentTimeMillis()
                )
            )
        } catch (error: Exception) {
            ResultadoCuentaFirebase.Error(mensaje(error, registro = true))
        }
    }

    override suspend fun entrarComoInvitado(): ResultadoCuentaFirebase {
        return try {
            val result = auth.signInAnonymously().await()
            val user = result.user ?: return ResultadoCuentaFirebase.Error(AuthMessages.ERROR_REGISTRO)
            ResultadoCuentaFirebase.Ok(
                guardar(
                    uid = user.uid,
                    nombre = "Invitado",
                    email = "",
                    rol = RolFirebase.INVITADO,
                    fechaCreacion = System.currentTimeMillis()
                )
            )
        } catch (error: Exception) {
            ResultadoCuentaFirebase.Error(mensaje(error, registro = true))
        }
    }

    override suspend fun establecerContrasena(email: String, password: String): ResultadoCuentaFirebase {
        val user = auth.currentUser
            ?: return ResultadoCuentaFirebase.Error(AuthMessages.CUENTA_SIN_CONTRASENA)
        return try {
            user.updatePassword(password).await()
            ResultadoCuentaFirebase.Ok(
                leerOCrear(
                    uid = user.uid,
                    nombre = user.displayName?.takeIf { it.isNotBlank() } ?: nombreDesdeCorreo(email),
                    email = email,
                    rol = RolFirebase.USUARIO
                )
            )
        } catch (error: Exception) {
            ResultadoCuentaFirebase.Error(mensaje(error, registro = false))
        }
    }

    override suspend fun cerrarSesion() {
        auth.signOut()
    }

    private suspend fun leerOCrear(
        uid: String,
        nombre: String,
        email: String,
        rol: String
    ): CuentaFirebase {
        val snapshot = firestore.collection(RolFirebase.COLECCION).document(uid).get().await()
        if (snapshot.exists()) {
            return CuentaFirebase(
                uid = uid,
                nombre = snapshot.getString("nombre")?.takeIf { it.isNotBlank() } ?: nombre,
                email = snapshot.getString("email")?.takeIf { it.isNotBlank() } ?: email,
                rol = snapshot.getString("rol")?.takeIf { it.isNotBlank() } ?: rol,
                fechaCreacion = snapshot.getLong("fechaCreacion") ?: System.currentTimeMillis()
            )
        }
        return guardar(uid, nombre, email, rol, System.currentTimeMillis())
    }

    private suspend fun guardar(
        uid: String,
        nombre: String,
        email: String,
        rol: String,
        fechaCreacion: Long
    ): CuentaFirebase {
        val datos = mapOf(
            "uid" to uid,
            "nombre" to nombre,
            "email" to email,
            "rol" to rol,
            "fechaCreacion" to fechaCreacion
        )
        firestore.collection(RolFirebase.COLECCION)
            .document(uid)
            .set(datos, SetOptions.merge())
            .await()
        return CuentaFirebase(uid, nombre, email, rol, fechaCreacion)
    }

    private fun FirebaseUser.tieneContrasena(): Boolean =
        providerData.any { it.providerId == "password" }

    private fun nombreDesdeCorreo(email: String): String =
        email.substringBefore("@").ifBlank { "Usuario" }

    private fun mensaje(error: Exception, registro: Boolean): String = when (error) {
        is FirebaseAuthUserCollisionException -> AuthMessages.CORREO_YA_REGISTRADO
        is FirebaseAuthInvalidUserException,
        is FirebaseAuthInvalidCredentialsException -> AuthMessages.CREDENCIALES_INVALIDAS
        is FirebaseNetworkException -> "Sin conexión. Revisa internet e intenta de nuevo."
        is FirebaseAuthException -> if (registro) AuthMessages.ERROR_REGISTRO else AuthMessages.CREDENCIALES_INVALIDAS
        else -> error.message ?: if (registro) AuthMessages.ERROR_REGISTRO else AuthMessages.CREDENCIALES_INVALIDAS
    }
}
