package ru.doGood.Lynk.feature.dashboard.utils

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Environment
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File

class ApkDownloader(private val context: Context) {

    fun downloadFile(
        url: String,
        fileName: String,
        scope: CoroutineScope? = null,
        onProgress: ((Float) -> Unit)? = null,
        onComplete: ((File) -> Unit)? = null
    ): Long {
        val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        val uri = Uri.parse(url)

        val file = File(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), fileName)
        if (file.exists()) {
            file.delete()
        }

        val request = DownloadManager.Request(uri)
            .setMimeType("application/vnd.android.package-archive")
            .setTitle(fileName)
            .setDescription("Downloading update...")
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            .setDestinationInExternalFilesDir(context, Environment.DIRECTORY_DOWNLOADS, fileName)

        val downloadId = downloadManager.enqueue(request)

        var pollingJob: Job? = null

        val onDownloadCompleteReceiver = object : BroadcastReceiver() {
            override fun onReceive(recvContext: Context?, intent: Intent?) {
                val id = intent?.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1L) ?: -1L
                if (id == downloadId) {
                    try {
                        context.unregisterReceiver(this)
                    } catch (_: Exception) {}

                    pollingJob?.cancel()

                    val downloadedFile = File(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), fileName)
                    if (downloadedFile.exists() && downloadedFile.length() > 0) {
                        onProgress?.invoke(1.0f)
                        installApk(downloadedFile)
                        onComplete?.invoke(downloadedFile)
                    }
                }
            }
        }

        val filter = IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE)
        ContextCompat.registerReceiver(context, onDownloadCompleteReceiver, filter, ContextCompat.RECEIVER_EXPORTED)

        if (scope != null && onProgress != null) {
            pollingJob = scope.launch(Dispatchers.IO) {
                var downloading = true
                while (downloading && isActive) {
                    val query = DownloadManager.Query().setFilterById(downloadId)
                    val cursor = downloadManager.query(query)
                    if (cursor != null) {
                        cursor.use { c ->
                            if (c.moveToFirst()) {
                                val bytesDownloadedIdx = c.getColumnIndex(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR)
                                val bytesTotalIdx = c.getColumnIndex(DownloadManager.COLUMN_TOTAL_SIZE_BYTES)
                                val statusIdx = c.getColumnIndex(DownloadManager.COLUMN_STATUS)

                                if (bytesDownloadedIdx != -1 && bytesTotalIdx != -1) {
                                    val downloadedBytes = c.getLong(bytesDownloadedIdx)
                                    val totalBytes = c.getLong(bytesTotalIdx)
                                    if (totalBytes > 0) {
                                        val progress = (downloadedBytes.toFloat() / totalBytes.toFloat()).coerceIn(0f, 1f)
                                        onProgress(progress)
                                    }
                                }

                                if (statusIdx != -1) {
                                    val status = c.getInt(statusIdx)
                                    if (status == DownloadManager.STATUS_SUCCESSFUL || status == DownloadManager.STATUS_FAILED) {
                                        downloading = false
                                    }
                                }
                            }
                        }
                    }
                    delay(400)
                }
            }
        }

        return downloadId
    }

    fun queryDownloadProgress(downloadId: Long): Float? {
        val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as? DownloadManager ?: return null
        val query = DownloadManager.Query().setFilterById(downloadId)
        val cursor = downloadManager.query(query) ?: return null
        return cursor.use { c ->
            if (c.moveToFirst()) {
                val bytesDownloadedIdx = c.getColumnIndex(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR)
                val bytesTotalIdx = c.getColumnIndex(DownloadManager.COLUMN_TOTAL_SIZE_BYTES)
                if (bytesDownloadedIdx != -1 && bytesTotalIdx != -1) {
                    val downloadedBytes = c.getLong(bytesDownloadedIdx)
                    val totalBytes = c.getLong(bytesTotalIdx)
                    if (totalBytes > 0) {
                        (downloadedBytes.toFloat() / totalBytes.toFloat()).coerceIn(0f, 1f)
                    } else 0f
                } else null
            } else null
        }
    }

    fun installApk(file: File) {
        if (!file.exists()) return
        val apkUri: Uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        val installIntent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(apkUri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(installIntent)
    }

    fun installDownloadedApk(fileName: String = "Lynk-update.apk"): Boolean {
        val file = File(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), fileName)
        if (file.exists() && file.length() > 0) {
            installApk(file)
            return true
        }
        return false
    }
}
