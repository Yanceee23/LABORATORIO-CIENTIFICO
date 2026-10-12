package com.laboratoriofisico.app.backend.repository

import com.laboratoriofisico.app.backend.model.Medicion
import com.laboratoriofisico.app.backend.model.Resultado
import com.laboratoriofisico.app.backend.session.SessionRepository
import com.laboratoriofisico.app.fisica.FormulaId
import com.laboratoriofisico.app.fisica.FisicaEngine
import com.laboratoriofisico.app.fisica.ResultadoMetadata
import com.laboratoriofisico.app.sensores.model.SensorType

class CapturaMagnetometroRepository(
    private val medicionRepository: MedicionRepository,
    private val resultadoRepository: ResultadoRepository,
    private val fisicaEngine: FisicaEngine,
    private val sessionRepository: SessionRepository
) {
    suspend fun procesarCaptura(
        experimentoId: Long,
        bx: Double,
        by: Double,
        bz: Double
    ): Resultado? {
        val usuarioId = sessionRepository.getLoggedInUserId() ?: return null
        val timestamp = System.currentTimeMillis()
        val medicionId = medicionRepository.insert(
            Medicion(
                experimentoId = experimentoId,
                usuarioId = usuarioId,
                nombre = "Magnetómetro (Bx=$bx, By=$by, Bz=$bz)",
                valor = bx,
                unidad = SensorType.MAGNETOMETER.unit,
                timestamp = timestamp
            )
        )

        val entradas = mapOf("Bx" to bx, "By" to by, "Bz" to bz)
        val intensidad = fisicaEngine.calcular(FormulaId.B_INTENSIDAD.id, entradas) ?: return null

        val resultado = Resultado(
            medicionId = medicionId,
            usuarioId = usuarioId,
            valorCalculado = intensidad,
            unidad = SensorType.MAGNETOMETER.unit,
            notas = ResultadoMetadata.build(
                formulaId = FormulaId.B_INTENSIDAD,
                sensor = SensorType.MAGNETOMETER,
                extras = entradas
            ),
            timestamp = timestamp + 1
        )
        resultadoRepository.insert(resultado)
        return resultado
    }
}
