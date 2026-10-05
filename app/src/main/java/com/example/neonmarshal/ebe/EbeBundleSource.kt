package com.example.neonmarshal.ebe

import android.content.res.AssetManager
import java.io.File
import java.io.IOException

interface EbeBundleSource {
    fun read(path: String): String
}

class AssetEbeBundleSource(
    private val assets: AssetManager,
    private val root: String = ""
) : EbeBundleSource {
    override fun read(path: String): String {
        val resolved = listOf(root.trim('/'), path.trim('/'))
            .filter { it.isNotEmpty() }
            .joinToString("/")
        return assets.open(resolved).bufferedReader().use { it.readText() }
    }
}

class FileEbeBundleSource(
    private val root: File
) : EbeBundleSource {
    override fun read(path: String): String {
        val file = File(root, path)
        if (!file.isFile) {
            throw IOException("EBE bundle artifact not found: ${file.path}")
        }
        return file.readText()
    }
}
