package com.example.neonmarshal.pixelgen

import android.content.res.AssetManager
import java.io.File
import java.io.IOException

interface PixelGenBundleSource {
    fun read(path: String): String
}

class AssetPixelGenBundleSource(
    private val assets: AssetManager,
    private val root: String = ""
) : PixelGenBundleSource {
    override fun read(path: String): String {
        val resolved = listOf(root.trim('/'), path.trim('/'))
            .filter { it.isNotEmpty() }
            .joinToString("/")
        return assets.open(resolved).bufferedReader().use { it.readText() }
    }
}

class FilePixelGenBundleSource(
    private val root: File
) : PixelGenBundleSource {
    override fun read(path: String): String {
        val file = File(root, path)
        if (!file.isFile) {
            throw IOException("PixelGen bundle artifact not found: ${file.path}")
        }
        return file.readText()
    }
}
