package com.laboratoriofisico.app.backend.repository

import com.laboratoriofisico.app.backend.auth.AppRole
import com.laboratoriofisico.app.backend.auth.AuthMessages
import com.laboratoriofisico.app.backend.local.dao.UsuarioDao
import com.laboratoriofisico.app.backend.local.entity.UsuarioEntity
import com.laboratoriofisico.app.backend.session.AuthSessionManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AuthRepositoryTest {

    private val fakeDao = FakeUsuarioDao()
    private val sessionManager = FakeSessionManager()
    private val repository = AuthRepository(fakeDao, sessionManager)

    @Test
    fun register_newEmail_succeeds() = runBlocking {
        val result = repository.register("Ana", "ana@test.com", "secreta1")
        assertTrue(result is AuthResult.Success)
        assertEquals(AppRole.REGISTRADO.storageValue, fakeDao.getById((result as AuthResult.Success).userId)?.rol)
    }

    @Test
    fun register_duplicateEmail_returnsSpecificMessage() = runBlocking {
        repository.register("Ana", "dup@test.com", "secreta1")
        val result = repository.register("Otra", "dup@test.com", "secreta2")
        assertTrue(result is AuthResult.Error)
        assertEquals(AuthMessages.CORREO_YA_REGISTRADO, (result as AuthResult.Error).message)
    }

    @Test
    fun login_correctCredentials_succeeds() = runBlocking {
        repository.register("Ana", "ok@test.com", "secreta1")
        sessionManager.logout()
        val result = repository.login("ok@test.com", "secreta1")
        assertTrue(result is AuthResult.Success)
        assertEquals((result as AuthResult.Success).userId, sessionManager.getActiveUserId())
    }

    @Test
    fun login_wrongPassword_returnsGenericMessage() = runBlocking {
        repository.register("Ana", "login@test.com", "secreta1")
        sessionManager.logout()
        val result = repository.login("login@test.com", "mala")
        assertTrue(result is AuthResult.Error)
        assertEquals(AuthMessages.CREDENCIALES_INVALIDAS, (result as AuthResult.Error).message)
    }

    @Test
    fun login_unknownEmail_returnsGenericMessage() = runBlocking {
        val result = repository.login("noexiste@test.com", "secreta1")
        assertTrue(result is AuthResult.Error)
        assertEquals(AuthMessages.CREDENCIALES_INVALIDAS, (result as AuthResult.Error).message)
    }

    @Test
    fun login_whenSessionAlreadyActive_allowsReentry() = runBlocking {
        val registered = repository.register("Ana", "activa@test.com", "secreta1")
        assertTrue(registered is AuthResult.Success)
        val again = repository.login("activa@test.com", "secreta1")
        assertTrue(again is AuthResult.Success)
        assertEquals((registered as AuthResult.Success).userId, (again as AuthResult.Success).userId)
    }

    @Test
    fun login_afterLogout_allowsNewSession() = runBlocking {
        repository.register("Ana", "otra@test.com", "secreta1")
        repository.logout()
        val result = repository.login("otra@test.com", "secreta1")
        assertTrue(result is AuthResult.Success)
    }

    @Test
    fun login_emptyPasswordHash_returnsNeedsPasswordReset() = runBlocking {
        fakeDao.insert(
            UsuarioEntity(
                nombre = "Demo",
                email = "demo@laboratoriofisico.app",
                rol = AppRole.REGISTRADO.storageValue,
                fechaCreacion = System.currentTimeMillis(),
                passwordHash = ""
            )
        )
        val result = repository.login("demo@laboratoriofisico.app", "cualquier")
        assertTrue(result is AuthResult.NeedsPasswordReset)
        assertEquals("demo@laboratoriofisico.app", (result as AuthResult.NeedsPasswordReset).email)
    }

    @Test
    fun enterAsGuest_setsGuestSessionWithoutUserId() = runBlocking {
        repository.enterAsGuest()
        assertNull(sessionManager.getActiveUserId())
        assertTrue(sessionManager.isGuest)
    }

    @Test
    fun setNewPassword_thenLoginSucceeds() = runBlocking {
        val id = fakeDao.insert(
            UsuarioEntity(
                nombre = "SinPass",
                email = "sinpass@test.com",
                rol = AppRole.REGISTRADO.storageValue,
                fechaCreacion = System.currentTimeMillis(),
                passwordHash = ""
            )
        )
        val set = repository.setNewPassword(id, "nueva123")
        assertTrue(set is AuthResult.Success)
        sessionManager.logout()
        val login = repository.login("sinpass@test.com", "nueva123")
        assertTrue(login is AuthResult.Success)
    }
}

private class FakeSessionManager : AuthSessionManager {
    private var loggedInUserId: Long? = null
    private var sessionActive: Boolean = false
    var isGuest: Boolean = false
        private set

    override suspend fun setLoggedInUser(userId: Long) {
        loggedInUserId = userId
        sessionActive = true
        isGuest = false
    }

    override suspend fun logout() {
        loggedInUserId = null
        sessionActive = false
        isGuest = false
    }

    override suspend fun getActiveUserId(): Long? =
        loggedInUserId?.takeIf { sessionActive && it > 0L }

    override suspend fun hasActiveSessionFor(userId: Long): Boolean =
        getActiveUserId() == userId

    override suspend fun enterAsGuest() {
        loggedInUserId = null
        sessionActive = true
        isGuest = true
    }
}

private class FakeUsuarioDao : UsuarioDao {
    private val users = mutableListOf<UsuarioEntity>()
    private var nextId = 1L

    override fun observeById(id: Long): Flow<UsuarioEntity?> {
        throw UnsupportedOperationException()
    }

    override suspend fun getById(id: Long): UsuarioEntity? =
        users.find { it.id == id }

    override suspend fun findByEmail(email: String): UsuarioEntity? =
        users.find { it.email == email }

    override suspend fun count(): Int = users.size

    override suspend fun insert(usuario: UsuarioEntity): Long {
        if (users.any { it.email == usuario.email }) {
            throw android.database.sqlite.SQLiteConstraintException("UNIQUE constraint failed")
        }
        val id = nextId++
        users.add(usuario.copy(id = id))
        return id
    }

    override suspend fun insertOrReplace(usuario: UsuarioEntity): Long = insert(usuario)

    override suspend fun updateProfile(id: Long, nombre: String, email: String, rol: String) {
        throw UnsupportedOperationException()
    }

    override suspend fun updatePasswordHash(id: Long, passwordHash: String) {
        val index = users.indexOfFirst { it.id == id }
        if (index >= 0) {
            users[index] = users[index].copy(passwordHash = passwordHash)
        }
    }

    override suspend fun findFirstUsuario(): UsuarioEntity? =
        users.minByOrNull { it.id }

    override suspend fun getAll(): List<UsuarioEntity> = users.toList()

    override suspend fun findDemoUsuario(): UsuarioEntity? =
        users.find { it.email == "demo@laboratoriofisico.app" }

    override suspend fun delete(usuario: UsuarioEntity) {
        users.removeAll { it.id == usuario.id }
    }
}
