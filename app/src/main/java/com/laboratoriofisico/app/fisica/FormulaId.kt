package com.laboratoriofisico.app.fisica

enum class FormulaId(val id: String) {
    V_IGUAL_D_T("movimiento.vIgualDT"),
    A_IGUAL_DELTA_V_DELTA_T("movimiento.aIgualDeltaVDeltaT"),
    D_IGUAL_V_T("movimiento.dIgualVT"),
    F_IGUAL_1_SOBRE_T("ondas.fIgual1SobreT"),
    T_IGUAL_1_SOBRE_F("ondas.tIgual1SobreF"),
    V_IGUAL_LAMBDA_F("ondas.vIgualLambdaF"),
    G_PENDULO("pendulo.gPendulo"),
    B_INTENSIDAD("magnetismo.bIntensidad"),
    THETA_INCIDENCIA_REFLEXION("reflexion.thetaIncidenciaReflexion");

    companion object {
        fun from(id: String): FormulaId? = entries.firstOrNull { it.id == id }
    }
}
