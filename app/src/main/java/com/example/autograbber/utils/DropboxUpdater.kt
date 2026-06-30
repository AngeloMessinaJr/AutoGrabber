package com.example.autograbber.utils

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.core.content.FileProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.net.URL
import java.util.Locale

class DropboxUpdater(private val context: Context) {

    private val client = OkHttpClient()
    private val jsonUrl = "https://www.dropbox.com/scl/fi/htsqpbv584hnhp09y0y0i/update.json?rlkey=5qfrbsg195cogxuzxnrm5jvyo&st=yxbp6h86&dl=1"

    fun checkForUpdates(onResult: (versionName: String?, apkUrl: String?) -> Unit) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val jsonText = URL(jsonUrl).readText()
                val jsonObject = JSONObject(jsonText)
                
                val latestVersionCode = jsonObject.optInt("versionCode", 0)
                val latestVersionName = jsonObject.optString("versionName", "Unknown")
                val apkUrl = jsonObject.optString("apkUrl", "")

                if (apkUrl.isEmpty()) {
                    withContext(Dispatchers.Main) { onResult(null, null) }
                    return@launch
                }

                val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
                val currentVersionCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    packageInfo.longVersionCode
                } else {
                    @Suppress("DEPRECATION")
                    packageInfo.versionCode.toLong()
                }

                withContext(Dispatchers.Main) {
                    if (latestVersionCode > currentVersionCode) {
                        onResult(latestVersionName, apkUrl)
                    } else {
                        onResult(null, null)
                    }
                }
            } catch (e: Exception) {
                Log.e("DropboxUpdater", "Check for updates failed", e)
                withContext(Dispatchers.Main) { onResult(null, null) }
            }
        }
    }

    suspend fun getLatestVersionName(): String? = withContext(Dispatchers.IO) {
        try {
            val jsonText = URL(jsonUrl).readText()
            val jsonObject = JSONObject(jsonText)
            val name = jsonObject.optString("versionName", "")
            if (name.isEmpty()) null else name
        } catch (e: Exception) {
            null
        }
    }

    suspend fun downloadAndInstall(apkUrl: String, onProgress: (Float) -> Unit): Boolean = withContext(Dispatchers.IO) {
        // 1. Check Install Permission
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (!context.packageManager.canRequestPackageInstalls()) {
                val intent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
                    data = Uri.parse("package:${context.packageName}")
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
                return@withContext false
            }
        }

        try {
            val request = Request.Builder().url(apkUrl).build()
            val response = client.newCall(request).execute()
            if (!response.isSuccessful) return@withContext false

            val body = response.body ?: return@withContext false
            val contentLength = body.contentLength()
            
            val targetFile = File(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), "update.apk")
            if (targetFile.exists()) targetFile.delete()

            body.byteStream().use { input ->
                FileOutputStream(targetFile).use { output ->
                    val buffer = ByteArray(8192)
                    var bytesRead: Int
                    var totalRead: Long = 0
                    while (input.read(buffer).also { bytesRead = it } != -1) {
                        output.write(buffer, 0, bytesRead)
                        totalRead += bytesRead
                        if (contentLength > 0) {
                            withContext(Dispatchers.Main) {
                                onProgress(totalRead.toFloat() / contentLength)
                            }
                        }
                    }
                }
            }

            withContext(Dispatchers.Main) {
                installApk(targetFile)
            }
            return@withContext true
        } catch (e: Exception) {
            Log.e("DropboxUpdater", "Download failed", e)
            return@withContext false
        }
    }

    private fun installApk(apkFile: File) {
        try {
            if (!apkFile.exists()) {
                Log.e("DropboxUpdater", "APK file not found")
                return
            }

            val apkUri: Uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                apkFile
            )

            val installIntent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(apkUri, "application/vnd.android.package-archive")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(installIntent)
        } catch (e: Exception) {
            Log.e("DropboxUpdater", "Install failed", e)
            Toast.makeText(context, "Update installation failed. Please try again.", Toast.LENGTH_LONG).show()
        }
    }
}
