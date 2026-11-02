package com.iappsan.carlauncher.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore

/**
 * Propiedad de extensión sobre Context: `context.settingsDataStore`.
 * El delegado `by preferencesDataStore` garantiza una única instancia por proceso
 * (DataStore falla si se abren dos sobre el mismo fichero). La comparten todos los repositorios.
 */
val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")
