package com.nothingness.bruhpatcher.core

import android.content.Context
import java.io.File
import java.io.InputStream

object UserFeatureImporter {

    fun import(context: Context, input: InputStream, name: String): File {
        val dir = File(context.filesDir, "features_user")
        if (!dir.exists()) dir.mkdirs()

        val dest = File(dir, name)
        input.use { ins ->
            dest.outputStream().use { os -> ins.copyTo(os) }
        }
        dest.setExecutable(true)
        return dest
    }
}
