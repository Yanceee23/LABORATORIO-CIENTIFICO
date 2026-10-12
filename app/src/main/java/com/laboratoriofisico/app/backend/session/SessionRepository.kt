package com.laboratoriofisico.app.backend.session

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.sessionDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "session_preferences"
)

/**
 * Sesión local persistida en DataStore.
 * Modos: none | guest | user. El invitado no deja userId en Room.
 */
class SessionRepository(
    private val dataStore: DataStore<Preferences>
) : AuthSessionManager {
    constructor(context: Context) : this(context.applicationContext.sessionDataStore)

    val session: Flow<AppSession> = dataStore.data.map { preferences ->
        when (preferences[SESSION_MODE] ?: inferLegacyMode(preferences)) {
            MODE_GUEST -> AppSession.Guest
            MODE_USER -> {
                val userId = preferences[LOGGED_IN_USER_ID] ?: 0L
                val active = preferences[SESSION_ACTIVE] ?: true
                if (userId > 0L && active) AppSession.User(userId) else AppSession.None
            }
            else -> AppSession.None
        }
    }

    /** Compatibilidad: flujos antiguos que solo leían el userId registrado. */
    val sessionUserId: Flow<Long?> = session.map { it.userIdOrNull }

    suspend fun getSession(): AppSession = session.first()

    suspend fun getLoggedInUserId(): Long? = getSession().userIdOrNull

    suspend fun isGuest(): Boolean = getSession() is AppSession.Guest

    suspend fun requireRegisteredUserId(): Long =
        getLoggedInUserId()
            ?: error("Se requiere una cuenta registrada para esta acción.")

    override suspend fun getActiveUserId(): Long? = getLoggedInUserId()

    override suspend fun hasActiveSessionFor(userId: Long): Boolean {
        if (userId <= 0L) return false
        return getActiveUserId() == userId
    }

    override suspend fun setLoggedInUser(userId: Long) {
        require(userId > 0L) { "userId de sesión inválido" }
        dataStore.edit { preferences ->
            preferences[SESSION_MODE] = MODE_USER
            preferences[LOGGED_IN_USER_ID] = userId
            preferences[SESSION_ACTIVE] = true
        }
    }

    override suspend fun enterAsGuest() {
        dataStore.edit { preferences ->
            preferences[SESSION_MODE] = MODE_GUEST
            preferences.remove(LOGGED_IN_USER_ID)
            preferences[SESSION_ACTIVE] = true
        }
    }

    override suspend fun logout() {
        dataStore.edit { preferences ->
            preferences[SESSION_MODE] = MODE_NONE
            preferences.remove(LOGGED_IN_USER_ID)
            preferences[SESSION_ACTIVE] = false
        }
    }

    private fun inferLegacyMode(preferences: Preferences): String {
        val userId = preferences[LOGGED_IN_USER_ID] ?: 0L
        val active = preferences[SESSION_ACTIVE] ?: true
        return if (userId > 0L && active) MODE_USER else MODE_NONE
    }

    companion object {
        private val LOGGED_IN_USER_ID = longPreferencesKey("logged_in_user_id")
        private val SESSION_ACTIVE = booleanPreferencesKey("session_active")
        private val SESSION_MODE = stringPreferencesKey("session_mode")

        private const val MODE_NONE = "none"
        private const val MODE_GUEST = "guest"
        private const val MODE_USER = "user"
    }
}
