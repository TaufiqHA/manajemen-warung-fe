package com.example.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import java.io.File
import java.io.FileOutputStream

class LogoManager(private val context: Context) {
    private val logoDir = File(context.filesDir, "company")
    private val logoFile = File(logoDir, "logo.png")

    init {
        if (!logoDir.exists()) logoDir.mkdirs()
    }

    fun saveLogo(bitmap: Bitmap): String? {
        return try {
            val out = FileOutputStream(logoFile)
            // Compress ke PNG agar transparansi (lingkaran) terjaga
            bitmap.compress(Bitmap.CompressFormat.PNG, 90, out)
            out.flush()
            out.close()
            logoFile.absolutePath
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun getLogoPath(): String? = if (logoFile.exists()) logoFile.absolutePath else null

    fun deleteLogo(): Boolean {
        return if (logoFile.exists()) logoFile.delete() else false
    }
}
