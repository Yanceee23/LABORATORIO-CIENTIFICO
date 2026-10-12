package com.laboratoriofisico.app.backend.auth

import com.laboratoriofisico.app.backend.session.AppSession
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RolePermissionsTest {

    @Test
    fun registrado_tieneAccesoCompleto() {
        val role = AppRole.REGISTRADO
        assertTrue(RolePermissions.canAccessLaboratorio(role))
        assertTrue(RolePermissions.canAccessTutor(role))
        assertTrue(RolePermissions.canAccessDocumentos(role))
        assertTrue(RolePermissions.canAccessEstudio(role))
        assertTrue(RolePermissions.canSaveMeasurements(role))
        assertTrue(RolePermissions.canViewMeasurementHistory(role))
    }

    @Test
    fun invitado_bloqueadoEnLaboratorioYTutor() {
        val role = AppRole.INVITADO
        assertFalse(RolePermissions.canAccessLaboratorio(role))
        assertFalse(RolePermissions.canAccessTutor(role))
        assertFalse(RolePermissions.canAccessDocumentos(role))
        assertFalse(RolePermissions.canAccessEstudio(role))
        assertFalse(RolePermissions.canSaveMeasurements(role))
        assertFalse(RolePermissions.canViewMeasurementHistory(role))
    }

    @Test
    fun guard_invitado_bloqueadoEnLaboratorio() {
        assertFalse(RegisteredUserGuard.canAccessLaboratorio(AppSession.Guest))
        assertTrue(RegisteredUserGuard.canAccessLaboratorio(AppSession.User(1L)))
    }

    @Test
    fun fromStorage_legacyEstudiante_esRegistrado() {
        assertEquals(AppRole.REGISTRADO, AppRole.fromStorage("estudiante"))
        assertEquals(AppRole.REGISTRADO, AppRole.fromStorage("registrado"))
        assertEquals(AppRole.INVITADO, AppRole.fromStorage("invitado"))
    }
}
