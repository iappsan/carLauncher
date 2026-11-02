package com.iappsan.carlauncher.theme

import android.content.Context
import android.graphics.BitmapFactory
import android.graphics.Typeface
import android.util.Log
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.font.FontFamily
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import com.iappsan.carlauncher.data.settingsDataStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withContext
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import java.io.File
import java.io.IOException

/** Un tema disponible para elegir. [spec] es `null` si su theme.json no se pudo leer. */
data class ThemeEntry(
    val id: String,
    val spec: ThemeSpec?,
    val error: String? = null,
)

/**
 * Motor de temas: lista, carga y recuerda el tema elegido.
 *
 * Fuentes de temas:
 * - Embebidos: `assets/themes/<carpeta>/` → id `builtin:<carpeta>`.
 * - Del usuario: `getExternalFilesDir("themes")/<carpeta>/` → id `file:<carpeta>`.
 *   (= /sdcard/Android/data/com.iappsan.carlauncher/files/themes, accesible por USB/adb sin permisos.)
 */
class ThemeRepository(
    private val context: Context,
    /** Si toca la variante de noche; viene de los ajustes del launcher. */
    isNight: Flow<Boolean>,
) {

    private val dataStore = context.settingsDataStore
    private val reloadSignal = MutableStateFlow(0)

    // allowComments/allowTrailingComma están marcadas como experimentales: hay que aceptarlo con @OptIn.
    @OptIn(ExperimentalSerializationApi::class)
    private val json = Json {
        ignoreUnknownKeys = true // campos de versiones futuras no rompen temas viejos
        isLenient = true
        allowTrailingComma = true
        allowComments = true // se pueden dejar comentarios // en el theme.json
    }

    /** Carpeta donde el usuario copia sus temas (puede ser null si no hay almacenamiento). */
    val userThemesDir: File? get() = context.getExternalFilesDir(THEMES_DIR)

    val selectedThemeId: Flow<String> = dataStore.data
        .catch { e -> if (e is IOException) emit(emptyPreferences()) else throw e }
        .map { it[KEY_THEME] ?: DEFAULT_ID }
        .distinctUntilChanged()

    /** Tema actual; se recarga al cambiar la selección, el modo día/noche o al llamar a [reload]. */
    val currentTheme: Flow<LoadedTheme> =
        combine(selectedThemeId, isNight, reloadSignal) { id, night, _ -> load(id, night) }
            .flowOn(Dispatchers.IO)

    suspend fun select(id: String) {
        dataStore.edit { it[KEY_THEME] = id }
    }

    /** Vuelve a leer el tema actual del disco (p. ej. tras copiar una versión nueva por adb). */
    fun reload() = reloadSignal.update { it + 1 }

    suspend fun listThemes(): List<ThemeEntry> = withContext(Dispatchers.IO) {
        val builtin = context.assets.list(THEMES_DIR).orEmpty().sorted().map { "$BUILTIN_PREFIX$it" }
        val user = userThemesDir?.listFiles { f -> File(f, THEME_FILE).isFile }
            .orEmpty()
            .map { "$FILE_PREFIX${it.name}" }
            .sortedBy { it.lowercase() }
        (builtin + user).map { id ->
            try {
                ThemeEntry(id, parse(sourceFor(id)))
            } catch (e: Exception) {
                ThemeEntry(id, spec = null, error = describe(e))
            }
        }
    }

    /** Carga un tema; si falla del todo, vuelve al tema por defecto (con el error como aviso). */
    private fun load(id: String, isNight: Boolean): LoadedTheme = try {
        loadOrThrow(id, isNight)
    } catch (e: Exception) {
        Log.w(TAG, "No se pudo cargar el tema $id", e)
        val fallback = if (id != DEFAULT_ID) runCatching { loadOrThrow(DEFAULT_ID, isNight) }.getOrNull() else null
        (fallback ?: LoadedTheme.Builtin).let { it.copy(warnings = listOf("Tema \"$id\": ${describe(e)}") + it.warnings) }
    }

    private fun loadOrThrow(id: String, isNight: Boolean): LoadedTheme {
        val source = sourceFor(id)
        val spec = parse(source)
        val warnings = mutableListOf<String>()
        val basic = resolveBasic(id, spec, isNight, warnings)
        val metrics = context.resources.displayMetrics
        val iconPx = (96 * metrics.density).toInt()

        return basic.copy(
            // Solo se decodifica el fondo de la variante activa
            backgroundImage = spec.backgroundFor(isNight).image?.let { path ->
                decodeSampled(source, path, metrics.widthPixels, metrics.heightPixels, warnings)
            },
            fontFamily = loadFont(source, spec.font, warnings),
            icons = spec.icons.mapNotNull { (key, path) ->
                decodeSampled(source, path, iconPx, iconPx, warnings)?.let { key to it }
            }.toMap(),
            warnings = warnings,
        )
    }

    private fun sourceFor(id: String): ThemeSource = when {
        id.startsWith(BUILTIN_PREFIX) ->
            ThemeSource.Asset(context.assets, "$THEMES_DIR/${id.removePrefix(BUILTIN_PREFIX)}")
        id.startsWith(FILE_PREFIX) -> {
            val dir = userThemesDir ?: throw IOException("Almacenamiento no disponible")
            ThemeSource.Directory(File(dir, id.removePrefix(FILE_PREFIX)))
        }
        else -> throw IllegalArgumentException("Id de tema desconocido: $id")
    }

    private fun parse(source: ThemeSource): ThemeSpec {
        val text = source.open(THEME_FILE).bufferedReader().use { it.readText() }
        return json.decodeFromString<ThemeSpec>(text)
    }

    private fun loadFont(source: ThemeSource, font: FontSpec, warnings: MutableList<String>): FontFamily {
        font.file?.let { path ->
            try {
                return FontFamily(source.typeface(path))
            } catch (e: Exception) {
                warnings += "No se pudo cargar la fuente \"$path\": ${describe(e)}"
            }
        }
        return when (font.family) {
            "sans-serif" -> FontFamily.SansSerif
            "serif" -> FontFamily.Serif
            "monospace" -> FontFamily.Monospace
            else -> FontFamily(Typeface.create(font.family, Typeface.NORMAL))
        }
    }

    /**
     * Decodifica una imagen reduciéndola (potencias de 2) hasta cerca del tamaño pedido,
     * para no cargar en memoria un fondo de 4000px en una pantalla de 1024.
     */
    private fun decodeSampled(
        source: ThemeSource,
        path: String,
        reqWidth: Int,
        reqHeight: Int,
        warnings: MutableList<String>,
    ): ImageBitmap? = try {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        source.open(path).use { BitmapFactory.decodeStream(it, null, bounds) }
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) {
            warnings += "Imagen no válida: \"$path\""
            null
        } else {
            var sample = 1
            while (bounds.outWidth / (sample * 2) >= reqWidth && bounds.outHeight / (sample * 2) >= reqHeight) {
                sample *= 2
            }
            val options = BitmapFactory.Options().apply { inSampleSize = sample }
            source.open(path).use { BitmapFactory.decodeStream(it, null, options) }?.asImageBitmap()
        }
    } catch (e: Exception) {
        warnings += "No se pudo leer la imagen \"$path\": ${describe(e)}"
        null
    }

    private fun describe(e: Exception): String = when (e) {
        is SerializationException -> "theme.json mal formado (${e.message?.lineSequence()?.firstOrNull()})"
        is java.io.FileNotFoundException -> "no existe ${e.message?.substringAfterLast('/')}"
        else -> e.message ?: e.javaClass.simpleName
    }

    companion object {
        private const val TAG = "ThemeRepository"
        private const val THEMES_DIR = "themes"
        private const val THEME_FILE = "theme.json"
        private const val BUILTIN_PREFIX = "builtin:"
        private const val FILE_PREFIX = "file:"
        const val DEFAULT_ID = "${BUILTIN_PREFIX}default"
        private val KEY_THEME = stringPreferencesKey("theme_id")
    }
}
