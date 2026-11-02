package com.iappsan.carlauncher.theme

import android.content.res.AssetManager
import android.graphics.Typeface
import java.io.File
import java.io.InputStream

/** De dónde se leen los ficheros de un tema: assets de la APK o una carpeta en almacenamiento. */
sealed interface ThemeSource {
    fun open(path: String): InputStream
    fun typeface(path: String): Typeface

    class Asset(private val assets: AssetManager, private val dir: String) : ThemeSource {
        override fun open(path: String): InputStream = assets.open("$dir/${checked(path)}")
        override fun typeface(path: String): Typeface = Typeface.createFromAsset(assets, "$dir/${checked(path)}")
    }

    class Directory(private val dir: File) : ThemeSource {
        override fun open(path: String): InputStream = File(dir, checked(path)).inputStream()
        override fun typeface(path: String): Typeface = Typeface.createFromFile(File(dir, checked(path)))
    }
}

/** Las rutas de un tema siempre son relativas a su carpeta: nada de `..` ni rutas absolutas. */
private fun checked(path: String): String {
    require(!path.startsWith("/") && ".." !in path.split('/', '\\')) { "Ruta no permitida: $path" }
    return path
}
