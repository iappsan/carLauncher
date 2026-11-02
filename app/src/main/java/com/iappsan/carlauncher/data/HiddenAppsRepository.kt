package com.iappsan.carlauncher.data

import android.content.ComponentName
import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringSetPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import java.io.IOException

/**
 * Apps ocultas del cajón. Se guarda el componente (`paquete/actividad`) y no solo el paquete,
 * porque algunas apps tienen varias entradas y se puede querer ocultar solo una.
 */
class HiddenAppsRepository(context: Context) {

    private val dataStore = context.settingsDataStore

    /** Componentes ocultos, en formato `ComponentName.flattenToString()`. */
    val hidden: Flow<Set<String>> = dataStore.data
        .catch { e -> if (e is IOException) emit(emptyPreferences()) else throw e }
        .map { it[KEY_HIDDEN] ?: emptySet() }
        .distinctUntilChanged()

    suspend fun setHidden(component: ComponentName, hidden: Boolean) {
        val key = component.flattenToString()
        dataStore.edit { prefs ->
            val current = prefs[KEY_HIDDEN] ?: emptySet()
            // `+` / `-` sobre un Set devuelven un Set nuevo (los Set de Kotlin son inmutables por defecto).
            prefs[KEY_HIDDEN] = if (hidden) current + key else current - key
        }
    }

    private companion object {
        val KEY_HIDDEN = stringSetPreferencesKey("hidden_apps")
    }
}
