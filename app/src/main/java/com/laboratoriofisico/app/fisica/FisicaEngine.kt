package com.laboratoriofisico.app.fisica

/**
 * Motor de cálculos físicos.
 * Implementación híbrida: despacho genérico por [FormulaId] hacia funciones tipadas en p05formulasfisicas.
 */
interface FisicaEngine {
    fun calcular(formulaId: String, entradas: Map<String, Double>): Double?
}
