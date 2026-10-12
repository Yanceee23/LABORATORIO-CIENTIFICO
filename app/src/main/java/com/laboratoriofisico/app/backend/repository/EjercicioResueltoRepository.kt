package com.laboratoriofisico.app.backend.repository

import com.laboratoriofisico.app.backend.auth.AuthMessages
import com.laboratoriofisico.app.backend.local.dao.EjercicioResueltoDao
import com.laboratoriofisico.app.backend.local.dao.JuegoEjercicioProgresoDao
import com.laboratoriofisico.app.backend.local.entity.EjercicioResueltoEntity
import com.laboratoriofisico.app.backend.local.entity.JuegoEjercicioProgresoEntity
import com.laboratoriofisico.app.backend.model.ConfianzaEjercicio
import com.laboratoriofisico.app.backend.model.EjercicioResuelto
import com.laboratoriofisico.app.backend.model.EjercicioResultadoValor
import com.laboratoriofisico.app.backend.model.JuegoEjercicioProgreso
import com.laboratoriofisico.app.backend.model.MateriaEjercicio
import com.laboratoriofisico.app.backend.model.VerificacionEjercicio
import com.laboratoriofisico.app.backend.session.SessionRepository
import com.laboratoriofisico.app.tutor.ejercicio.EjercicioResueltoJsonCodec
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

@OptIn(ExperimentalCoroutinesApi::class)
class EjercicioResueltoRepository(
    private val dao: EjercicioResueltoDao,
    private val progresoDao: JuegoEjercicioProgresoDao,
    private val sessionRepository: SessionRepository
) {
    fun observeMisEjercicios(materia: String? = null): Flow<List<EjercicioResuelto>> =
        sessionRepository.sessionUserId.flatMapLatest { userId ->
            if (userId == null) {
                flowOf(emptyList())
            } else if (materia.isNullOrBlank()) {
                dao.observeByUsuario(userId).map { list -> list.map { it.toDomain() } }
            } else {
                dao.observeByUsuarioAndMateria(userId, materia)
                    .map { list -> list.map { it.toDomain() } }
            }
        }

    fun observeProgresoActual(): Flow<Map<Long, JuegoEjercicioProgreso>> =
        sessionRepository.sessionUserId.flatMapLatest { userId ->
            if (userId == null) {
                flowOf(emptyMap())
            } else {
                progresoDao.observeByUsuario(userId).map { list ->
                    list.map { it.toDomain() }.associateBy { it.ejercicioId }
                }
            }
        }

    suspend fun getByIdForCurrentUser(id: Long): EjercicioResuelto? {
        val userId = sessionRepository.getLoggedInUserId() ?: return null
        return dao.getByIdAndUsuario(id, userId)?.toDomain()
    }

    suspend fun getProgreso(ejercicioId: Long): JuegoEjercicioProgreso? {
        val userId = sessionRepository.getLoggedInUserId() ?: return null
        return progresoDao.getByUsuarioAndEjercicio(userId, ejercicioId)?.toDomain()
    }

    suspend fun insert(ejercicio: EjercicioResuelto): Long {
        val userId = sessionRepository.getLoggedInUserId()
            ?: error(AuthMessages.INVITADO_BLOQUEADO)
        return dao.insert(ejercicio.copy(usuarioId = userId).toEntity())
    }

    suspend fun deleteById(id: Long) {
        val userId = sessionRepository.getLoggedInUserId()
            ?: error(AuthMessages.INVITADO_BLOQUEADO)
        dao.deleteByIdAndUsuario(id, userId)
    }

    suspend fun guardarProgreso(progreso: JuegoEjercicioProgreso): JuegoEjercicioProgreso {
        val userId = sessionRepository.getLoggedInUserId()
            ?: error(AuthMessages.INVITADO_BLOQUEADO)
        val existing = progresoDao.getByUsuarioAndEjercicio(userId, progreso.ejercicioId)
        val entity = progreso.copy(
            id = existing?.id ?: progreso.id,
            usuarioId = userId,
            fechaActualizacion = System.currentTimeMillis()
        ).toEntity()
        val id = progresoDao.upsert(entity)
        return entity.toDomain().copy(id = if (entity.id == 0L) id else entity.id)
    }

    private fun EjercicioResueltoEntity.toDomain(): EjercicioResuelto = EjercicioResuelto(
        id = id,
        usuarioId = usuarioId,
        fotoPath = fotoPath,
        materia = MateriaEjercicio.from(materia),
        tema = tema,
        enunciado = enunciado,
        datos = EjercicioResueltoJsonCodec.decodeDatos(datosJson),
        incognita = incognita,
        formula = formula,
        formulaLegible = formulaLegible,
        variables = EjercicioResueltoJsonCodec.decodeVariables(variablesJson),
        explicacionFormula = explicacionFormula,
        pasos = EjercicioResueltoJsonCodec.decodePasos(pasosJson),
        resultado = EjercicioResultadoValor(resultadoValor, resultadoUnidad),
        verificacion = VerificacionEjercicio.from(verificacion),
        confianza = ConfianzaEjercicio.from(confianza),
        fechaCreacion = fechaCreacion
    )

    private fun EjercicioResuelto.toEntity(): EjercicioResueltoEntity = EjercicioResueltoEntity(
        id = id,
        usuarioId = usuarioId,
        fotoPath = fotoPath,
        materia = materia.key,
        tema = tema,
        enunciado = enunciado,
        datosJson = EjercicioResueltoJsonCodec.encodeDatos(datos),
        incognita = incognita,
        formula = formula,
        formulaLegible = formulaLegible,
        variablesJson = EjercicioResueltoJsonCodec.encodeVariables(variables),
        explicacionFormula = explicacionFormula,
        pasosJson = EjercicioResueltoJsonCodec.encodePasos(pasos),
        resultadoValor = resultado.valor,
        resultadoUnidad = resultado.unidad,
        verificacion = verificacion.key,
        confianza = confianza.key,
        fechaCreacion = fechaCreacion
    )

    private fun JuegoEjercicioProgresoEntity.toDomain(): JuegoEjercicioProgreso =
        JuegoEjercicioProgreso(
            id = id,
            usuarioId = usuarioId,
            ejercicioId = ejercicioId,
            intentos = intentos,
            estrellas = estrellas,
            pistasUsadas = pistasUsadas,
            completo = completo,
            fechaActualizacion = fechaActualizacion
        )

    private fun JuegoEjercicioProgreso.toEntity(): JuegoEjercicioProgresoEntity =
        JuegoEjercicioProgresoEntity(
            id = id,
            usuarioId = usuarioId,
            ejercicioId = ejercicioId,
            intentos = intentos,
            estrellas = estrellas,
            pistasUsadas = pistasUsadas,
            completo = completo,
            fechaActualizacion = fechaActualizacion
        )
}
