package com.komal.ai.updater

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.util.Log
import androidx.core.content.FileProvider
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File

data class GitHubRelease(
    @SerializedName("tag_name") val tagName: String,
    @SerializedName("name") val name: String?,
    @SerializedName("body") val body: String?,
    @SerializedName("html_url") val htmlUrl: String,
    @SerializedName("assets") val assets: List<GitHubReleaseAsset>?
)

data class GitHubReleaseAsset(
    @SerializedName("name") val name: String,
    @SerializedName("browser_download_url") val downloadUrl: String,
    @SerializedName("size") val size: Long
)

class AppUpdateManager(private val context: Context) {

    private val client = OkHttpClient()
    private val gson = Gson()
    val currentVersion = "v1.1.0"

    data class UpdateInfo(
        val isUpdateAvailable: Boolean,
        val latestVersion: String,
        val releaseNotes: String,
        val downloadUrl: String
    )

    /**
     * Checks if a new release is available on GitHub
     */
    suspend fun checkForUpdates(): Result<UpdateInfo?> = withContext(Dispatchers.IO) {
        try {
            val url = "https://api.github.com/repos/raufzen/KomalAI/releases/latest"
            val request = Request.Builder()
                .url(url)
                .header("Accept", "application/vnd.github.v3+json")
                .build()

            val response = client.newCall(request).execute()
            val json = response.body?.string() ?: ""

            if (!response.isSuccessful) {
                return@withContext Result.failure(Exception("GitHub API HTTP ${response.code}"))
            }

            val release = gson.fromJson(json, GitHubRelease::class.java)
            val latestTag = release.tagName.trim()

            // Find apk asset download URL
            val apkAsset = release.assets?.firstOrNull { it.name.endsWith(".apk", ignoreCase = true) }
            val downloadUrl = apkAsset?.downloadUrl ?: release.htmlUrl

            val isNewer = isVersionNewer(latestTag, currentVersion)

            val info = UpdateInfo(
                isUpdateAvailable = isNewer,
                latestVersion = latestTag,
                releaseNotes = release.body ?: "Bug fixes and performance improvements",
                downloadUrl = downloadUrl
            )
            Result.success(info)
        } catch (e: Exception) {
            Log.e("AppUpdateManager", "Error checking for updates", e)
            Result.failure(e)
        }
    }

    private fun isVersionNewer(latest: String, current: String): Boolean {
        val cleanLatest = latest.removePrefix("v").split(".").mapNotNull { it.toIntOrNull() }
        val cleanCurrent = current.removePrefix("v").split(".").mapNotNull { it.toIntOrNull() }

        for (i in 0 until minOf(cleanLatest.size, cleanCurrent.size)) {
            if (cleanLatest[i] > cleanCurrent[i]) return true
            if (cleanLatest[i] < cleanCurrent[i]) return false
        }
        return cleanLatest.size > cleanCurrent.size
    }

    /**
     * Downloads APK and prompts system installer
     */
    fun downloadAndInstallApk(downloadUrl: String, onProgress: (String) -> Unit) {
        try {
            val file = File(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), "KomalAI-Update.apk")
            if (file.exists()) file.delete()

            val request = DownloadManager.Request(Uri.parse(downloadUrl)).apply {
                setTitle("Komal AI Update")
                setDescription("Downloading latest version...")
                setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                setDestinationUri(Uri.fromFile(file))
                setMimeType("application/vnd.android.package-archive")
            }

            val manager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
            val downloadId = manager.enqueue(request)
            onProgress("Downloading update...")

            val receiver = object : BroadcastReceiver() {
                override fun onReceive(c: Context?, intent: Intent?) {
                    val id = intent?.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1L)
                    if (id == downloadId) {
                        context.unregisterReceiver(this)
                        installApk(file)
                    }
                }
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.registerReceiver(receiver, IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE), Context.RECEIVER_EXPORTED)
            } else {
                context.registerReceiver(receiver, IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE))
            }
        } catch (e: Exception) {
            Log.e("AppUpdateManager", "Download failed", e)
            // Fallback to opening browser
            val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(downloadUrl)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(browserIntent)
        }
    }

    private fun installApk(file: File) {
        try {
            val apkUri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )

            val installIntent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(apkUri, "application/vnd.android.package-archive")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(installIntent)
        } catch (e: Exception) {
            Log.e("AppUpdateManager", "Failed to launch package installer", e)
        }
    }
}
