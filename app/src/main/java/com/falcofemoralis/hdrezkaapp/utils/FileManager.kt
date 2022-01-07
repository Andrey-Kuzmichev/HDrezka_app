package com.falcofemoralis.hdrezkaapp.utils

import android.content.Context
import java.io.*

object FileManager {
    fun readFile(src: String, context: Context): String? {
        return try {
            val file = File(context.filesDir.toString(), src)
            val data = StringBuilder()
            val br = BufferedReader(FileReader(file))
            var text: String?
            while (br.readLine().also { text = it } != null) {
                data.append(text)
            }
            br.close()
            data.toString()
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun writeFile(src: String, content: String, append: Boolean, context: Context) {
        val file = File(context.filesDir.toString(), src)
        val bw = BufferedWriter(FileWriter(file, append))
        bw.write(content)
        bw.close()
    }
}