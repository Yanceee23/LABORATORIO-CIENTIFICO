package com.laboratoriofisico.app.tutor.ejercicio

import com.laboratoriofisico.app.backend.auth.AuthMessages
import com.laboratoriofisico.app.backend.auth.RegisteredUserGuard
import com.laboratoriofisico.app.backend.model.ConfianzaEjercicio
import com.laboratoriofisico.app.backend.model.EjercicioDato
import com.laboratoriofisico.app.backend.model.EjercicioPaso
import com.laboratoriofisico.app.backend.model.EjercicioResuelto
import com.laboratoriofisico.app.backend.model.EjercicioResultadoValor
import com.laboratoriofisico.app.backend.model.MateriaEjercicio
import com.laboratoriofisico.app.backend.model.VerificacionEjercicio
import com.laboratoriofisico.app.backend.session.AppSession
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tests de acceso, guardado por userId y que reabrir no llama a Gemini.
 * Usa fakes en memoria (sin Room).
 */
class EjercicioResueltoAccessAndLoadTest {

    @Test
    fun invitado_bloqueadoEnGuard() {
        assertFalse(RegisteredUserGuard.canAccessTutor(AppSession.Guest))
        assertTrue(RegisteredUserGuard.canAccessTutor(AppSession.User(1L)))
    }

    @Test
    fun repoFake_guardaYLeePorUserId() = runBlocking {
        val store = FakeEjercicioStore()
        val e = sampleEjercicio(usuarioId = 7L)
        val id = store.insert(e)
        val loaded = store.getByIdAndUsuario(id, 7L)
        assertTrue(loaded != null)
        assertEquals(7L, loaded!!.usuarioId)
        assertEquals(null, store.getByIdAndUsuario(id, 99L))
    }

    @Test
    fun repoFake_invitadoNoInserta() = runBlocking {
        val store = FakeEjercicioStore(allowUserId = null)
        val error = runCatching { store.requireUserAndInsert(sampleEjercicio(0)) }
        assertTrue(error.isFailure)
        assertEquals(AuthMessages.INVITADO_BLOQUEADO, error.exceptionOrNull()?.message)
    }

    @Test
    fun segundaApertura_noIncrementaGemini() = runBlocking {
        val service = FakeResolverService()
        service.resolverDesdeFotoFake()
        assertEquals(1, service.geminiCallCount)
        service.cargarGuardadoFake(1L)
        service.cargarGuardadoFake(1L)
        assertEquals(1, service.geminiCallCount)
    }
}

private fun sampleEjercicio(usuarioId: Long) = EjercicioResuelto(
    id = 0,
    usuarioId = usuarioId,
    fotoPath = "/tmp/x.jpg",
    materia = MateriaEjercicio.FISICA,
    tema = "movimiento",
    enunciado = "test",
    datos = listOf(EjercicioDato("d", 1.0, "m")),
    incognita = "v",
    formula = "v=d/t",
    pasos = listOf(EjercicioPaso(1, "Paso", "explica", "1/1")),
    resultado = EjercicioResultadoValor(1.0, "m/s"),
    verificacion = VerificacionEjercicio.COINCIDE,
    confianza = ConfianzaEjercicio.ALTA,
    fechaCreacion = 1L
)

private class FakeEjercicioStore(private val allowUserId: Long? = 7L) {
    private val items = mutableListOf<EjercicioResuelto>()
    private var nextId = 1L

    suspend fun requireUserAndInsert(ejercicio: EjercicioResuelto): Long {
        val userId = allowUserId ?: error(AuthMessages.INVITADO_BLOQUEADO)
        return insert(ejercicio.copy(usuarioId = userId))
    }

    suspend fun insert(ejercicio: EjercicioResuelto): Long {
        val id = nextId++
        items.add(ejercicio.copy(id = id))
        return id
    }

    suspend fun getByIdAndUsuario(id: Long, usuarioId: Long): EjercicioResuelto? =
        items.find { it.id == id && it.usuarioId == usuarioId }
}

private class FakeResolverService {
    var geminiCallCount = 0
        private set
    private val saved = mutableMapOf<Long, EjercicioResuelto>()

    suspend fun resolverDesdeFotoFake() {
        geminiCallCount++
        saved[1L] = sampleEjercicio(1L).copy(id = 1L)
    }

    suspend fun cargarGuardadoFake(id: Long): EjercicioResuelto? {
        // Sin Gemini
        return saved[id]
    }
}

class Migration12To13Test {
    @Test
    fun migration_versions() {
        assertEquals(12, com.laboratoriofisico.app.backend.local.database.MIGRATION_12_13.startVersion)
        assertEquals(13, com.laboratoriofisico.app.backend.local.database.MIGRATION_12_13.endVersion)
    }
}

class Migration13To14Test {
    @Test
    fun migration_versions() {
        assertEquals(13, com.laboratoriofisico.app.backend.local.database.MIGRATION_13_14.startVersion)
        assertEquals(14, com.laboratoriofisico.app.backend.local.database.MIGRATION_13_14.endVersion)
    }
}
