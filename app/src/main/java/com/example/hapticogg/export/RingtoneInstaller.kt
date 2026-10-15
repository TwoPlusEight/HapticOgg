package com.example.hapticogg.export

import android.content.ContentValues
import android.content.Intent
import android.media.MediaScannerConnection
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.provider.Settings
import androidx.activity.ComponentActivity
import java.io.File

/**
 * Copies a converted OGG into the ringtone collection and makes it the default ringtone.
 * The returned text is the message the player dialog shows.
 */
class RingtoneInstaller(private val activity: ComponentActivity) {

    /** Sets [file] as the system ringtone and returns a message for the player dialog. */
    fun install(file: File): String {
        if (!file.exists()) return "文件不存在：${file.name}"
        if (!Settings.System.canWrite(activity)) {
            activity.startActivity(Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS).apply {
                data = Uri.parse("package:${activity.packageName}")
            })
            return "请先允许本应用修改系统设置，回到播放界面再点一次「设为铃声」。"
        }
        val name = file.name
        return try {
            val uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val values = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, name)
                    put(MediaStore.MediaColumns.MIME_TYPE, ExportSpec.MIME)
                    put(
                        MediaStore.MediaColumns.RELATIVE_PATH,
                        "${ExportSpec.RINGTONE_COLLECTION}/${ExportSpec.MUSIC_DIR}"
                    )
                    put(MediaStore.MediaColumns.IS_PENDING, 1)
                    put(MediaStore.Audio.AudioColumns.IS_RINGTONE, true)
                    put(MediaStore.Audio.AudioColumns.IS_NOTIFICATION, false)
                    put(MediaStore.Audio.AudioColumns.IS_ALARM, false)
                    put(MediaStore.Audio.AudioColumns.IS_MUSIC, true)
                }
                val inserted = activity.contentResolver
                    .insert(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, values)
                    ?: throw IllegalStateException("无法在媒体库创建铃声")
                activity.contentResolver.openOutputStream(inserted)?.use { out ->
                    file.inputStream().use { it.copyTo(out) }
                } ?: throw IllegalStateException("无法写入铃声")
                values.clear()
                values.put(MediaStore.MediaColumns.IS_PENDING, 0)
                activity.contentResolver.update(inserted, values, null, null)
                inserted
            } else {
                @Suppress("DEPRECATION")
                val dir = File(
                    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_RINGTONES),
                    ExportSpec.MUSIC_DIR
                )
                if (!dir.exists() && !dir.mkdirs()) throw IllegalStateException("无法创建铃声目录")
                val target = File(dir, name)
                file.inputStream().use { input -> target.outputStream().use { input.copyTo(it) } }
                MediaScannerConnection.scanFile(
                    activity, arrayOf(target.absolutePath), arrayOf(ExportSpec.MIME), null
                )
                Uri.fromFile(target)
            }
            RingtoneManager.setActualDefaultRingtoneUri(activity, RingtoneManager.TYPE_RINGTONE, uri)
            "已把「$name」设为系统铃声。"
        } catch (e: Exception) {
            "设置铃声失败：${e.message}"
        }
    }
}
