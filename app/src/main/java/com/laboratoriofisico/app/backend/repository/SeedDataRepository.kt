package com.laboratoriofisico.app.backend.repository

import androidx.room.withTransaction
import com.laboratoriofisico.app.backend.local.database.LaboratorioDatabase
import com.laboratoriofisico.app.backend.local.dao.ConfiguracionDao
import com.laboratoriofisico.app.backend.local.dao.DocumentoDao
import com.laboratoriofisico.app.backend.local.dao.ExperimentoDao
import com.laboratoriofisico.app.backend.local.dao.MedicionDao
import com.laboratoriofisico.app.backend.local.dao.ResultadoDao
import com.laboratoriofisico.app.backend.local.dao.SeccionDocumentoDao
import com.laboratoriofisico.app.backend.local.dao.UbicacionDao
import com.laboratoriofisico.app.backend.local.dao.UsuarioDao
import com.laboratoriofisico.app.core.debug.DebugSessionLog

class SeedDataRepository(
    private val database: LaboratorioDatabase,
    private val usuarioDao: UsuarioDao,
    private val ubicacionDao: UbicacionDao,
    private val experimentoDao: ExperimentoDao,
    private val medicionDao: MedicionDao,
    private val resultadoDao: ResultadoDao,
    private val configuracionDao: ConfiguracionDao,
    private val documentoDao: DocumentoDao,
    private val seccionDocumentoDao: SeccionDocumentoDao
) {
    suspend fun seedIfEmpty() {
        removeLegacyDemoData()
        if (configuracionDao.count() > 0) return

        database.withTransaction {
            if (configuracionDao.count() > 0) return@withTransaction
            // La configuración ahora es por usuario registrado; el invitado usa defaults en memoria.
            // #region agent log
            DebugSessionLog.log(
                hypothesisId = "H-SEED",
                location = "SeedDataRepository.seedIfEmpty",
                message = "Seed aplicado sin configuración global (ahora es por usuario)",
                data = mapOf("demoRemoved" to true)
            )
            // #endregion
        }
    }

    /** Elimina datos de ejemplo de Fase 1 que podían confundirse con mediciones reales. */
    suspend fun removeLegacyDemoData() {
        database.withTransaction {
            val demoExperimentos = experimentoDao.findDemoExperimentos()
            demoExperimentos.forEach { experimentoDao.delete(it) }

            documentoDao.findDemoDocumentos().forEach { doc ->
                seccionDocumentoDao.deleteByDocumento(doc.id)
                documentoDao.delete(doc)
            }

            usuarioDao.findDemoUsuario()?.let { demoUser ->
                if (usuarioDao.count() == 1) {
                    usuarioDao.delete(demoUser)
                }
            }

            val demoResultadosCount = resultadoDao.findDemoResultados().size
            resultadoDao.deleteDemoResultados()
            // #region agent log
            DebugSessionLog.log(
                hypothesisId = "H-SEED",
                location = "SeedDataRepository.removeLegacyDemoData",
                message = "Limpieza de seed demo",
                data = mapOf(
                    "experimentosDemo" to demoExperimentos.size,
                    "resultadosDemoEliminados" to demoResultadosCount
                )
            )
            // #endregion
        }
    }
}
