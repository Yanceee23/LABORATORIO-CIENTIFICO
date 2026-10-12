package com.laboratoriofisico.app.estudio

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.laboratoriofisico.app.backend.model.TipoResumen
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.estudioDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "estudio_textos"
)

class EstudioTextoLocalRepository(context: Context) {
    private val dataStore = context.applicationContext.estudioDataStore

    suspend fun saveResumen(documentoId: Long, tipo: TipoResumen, texto: String) {
        dataStore.edit { prefs ->
            prefs[stringPreferencesKey(resumenKey(documentoId, tipo))] = texto
        }
    }

    suspend fun loadResumen(documentoId: Long, tipo: TipoResumen): String? =
        dataStore.data.map { prefs ->
            prefs[stringPreferencesKey(resumenKey(documentoId, tipo))]
        }.first()

    suspend fun saveNotas(documentoId: Long, texto: String) {
        dataStore.edit { prefs ->
            prefs[stringPreferencesKey(notasKey(documentoId))] = texto
        }
    }

    suspend fun loadNotas(documentoId: Long): String? =
        dataStore.data.map { prefs ->
            prefs[stringPreferencesKey(notasKey(documentoId))]
        }.first()

    private fun resumenKey(documentoId: Long, tipo: TipoResumen): String =
        "resumen_${documentoId}_${tipo.name}"

    private fun notasKey(documentoId: Long): String = "notas_$documentoId"
}
