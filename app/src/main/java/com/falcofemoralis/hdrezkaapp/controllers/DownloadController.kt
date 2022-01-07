package com.falcofemoralis.hdrezkaapp.controllers

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.StrictMode
import android.os.StrictMode.ThreadPolicy
import androidx.core.content.FileProvider
import com.falcofemoralis.hdrezkaapp.BuildConfig
import com.falcofemoralis.hdrezkaapp.constants.DownloadType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.net.URI
import java.net.URL


class DownloadController(private val context: Context, private val apkUrl: String, private val callback: (type: DownloadType, value: Float) -> Unit) {
    companion object {
        private const val FILE_NAME = "HDrezka-app.apk"
        private const val FILE_BASE_PATH = "file://"
        private const val PROVIDER_PATH = ".provider"
        private const val APP_INSTALL_PATH = "\"application/vnd.android.package-archive\""
    }

    fun enqueueDownload() {
        try {
            GlobalScope.launch {
                val policy = ThreadPolicy.Builder().permitAll().build()
                StrictMode.setThreadPolicy(policy)

                val destinationPath = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS).toString() + "/" + FILE_NAME
                val file = File(destinationPath)

                if (file.exists()) {
                    file.delete()
                }

                try {
                    val url = URL(apkUrl)
                    val connection = url.openConnection()
                    connection.connect()

                    val uri: URI = URI.create(apkUrl)
                    uri.toURL().openStream().use { inputStream ->
                        val inputStream: InputStream = url.openStream()
                        val outputStream = FileOutputStream(destinationPath)
                        val data = ByteArray(1024)
                        var count: Int = 0
                        var total: Long = 0

                        val totalLength = (connection.contentLength / 1024F / 1024F) // In megabytes
                        withContext(Dispatchers.Main) {
                            callback(DownloadType.SET_MAX, totalLength)
                        }

                        while (inputStream.read(data).also { count = it } != -1) {
                            total += count

                            withContext(Dispatchers.Main) {
                                callback(DownloadType.SET_PROGRESS, (total / 1024F / 1024F))
                            }
                            outputStream.write(data, 0, count);
                        }

                        inputStream.close()
                        outputStream.close()

                        withContext(Dispatchers.Main) {
                            callback(DownloadType.SUCCESS, -1F)
                            showInstallOption(destinationPath, Uri.parse("$FILE_BASE_PATH$destinationPath"))
                        }

                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        callback(DownloadType.FAILED, -1F)
                    }

                    e.printStackTrace()
                }
            }
        } catch (e: java.lang.Exception) {
            callback(DownloadType.FAILED, -1F)

            e.printStackTrace()
        }
    }

    private fun showInstallOption(destination: String, uri: Uri) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            val contentUri = FileProvider.getUriForFile(
                context,
                BuildConfig.APPLICATION_ID + PROVIDER_PATH,
                File(destination)
            )
            val install = Intent(Intent.ACTION_VIEW)
            install.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            install.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            install.putExtra(Intent.EXTRA_NOT_UNKNOWN_SOURCE, true)
            install.data = contentUri
            context.startActivity(install)
        } else {
            val install = Intent(Intent.ACTION_VIEW)
            install.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
            install.setDataAndType(uri, APP_INSTALL_PATH)
            context.startActivity(install)
        }
    }
}