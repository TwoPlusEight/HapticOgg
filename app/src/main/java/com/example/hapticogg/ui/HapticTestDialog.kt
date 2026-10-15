package com.example.hapticogg.ui

import android.app.AlertDialog
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.TextView
import androidx.activity.ComponentActivity
import java.io.File
import java.util.Locale

/**
 * Popup player for a converted OGG. Playback keeps haptic channels enabled, so the system
 * generates the haptics itself - the same path a real ringtone takes - and the file can be
 * set as the system ringtone right from here.
 */
class HapticTestDialog(
    private val activity: ComponentActivity,
    private val audio: File,
    private val onSetRingtone: (File) -> String
) {
    private val hapticsSupported: Boolean =
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
            runCatching { AudioManager.isHapticPlaybackSupported() }.getOrDefault(false)

    private val handler = Handler(Looper.getMainLooper())
    private var dialog: AlertDialog? = null
    private var player: MediaPlayer? = null
    private var playButton: Button? = null
    private var seekBar: SeekBar? = null
    private var timeLabel: TextView? = null
    private var statusText: TextView? = null
    private var resumePosition = 0
    private var resumePlaying = false

    fun show() {
        val column = LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(Fluent.dp(activity, 20), Fluent.dp(activity, 16), Fluent.dp(activity, 20), Fluent.dp(activity, 4))
        }
        column.addView(Fluent.body(activity, audio.name))
        column.addView(
            Fluent.caption(
                activity,
                if (hapticsSupported) {
                    "如果振动意外消失暂停重新播放即可。"
                } else {
                    "本设备用的不是线性马达，无法同步振动，因此只能试听音频。"
                }
            ),
            Fluent.rowParams(activity, 6, 12)
        )

        seekBar = SeekBar(activity).apply {
            max = 1000
            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(bar: SeekBar?, progress: Int, fromUser: Boolean) {
                    if (fromUser) updateTime(progress)
                }

                override fun onStartTrackingTouch(bar: SeekBar?) {}

                override fun onStopTrackingTouch(bar: SeekBar?) {
                    val target = bar?.progress ?: return
                    player?.seekTo(target.toLong(), MediaPlayer.SEEK_CLOSEST)
                    updateTime(target)
                }
            })
        }
        column.addView(seekBar, wrap())

        timeLabel = Fluent.caption(activity, "00:00 / 00:00")
        column.addView(timeLabel, Fluent.rowParams(activity, 2))

        val row = LinearLayout(activity).apply { orientation = LinearLayout.HORIZONTAL }
        playButton = Fluent.primaryButton(activity, "播放") { togglePlay() }.apply {
            isEnabled = false
        }
        val ringtoneButton = Fluent.secondaryButton(activity, "设为铃声") {
            statusText?.text = onSetRingtone(audio)
        }
        row.addView(playButton, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        row.addView(
            ringtoneButton,
            LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply {
                leftMargin = Fluent.dp(activity, 8)
            }
        )
        column.addView(row, Fluent.rowParams(activity, 12))

        statusText = Fluent.caption(activity, "")
        column.addView(statusText, Fluent.rowParams(activity, 10))

        preparePlayer()

        dialog = AlertDialog.Builder(activity)
            .setTitle("播放测试")
            .setView(column)
            .setNegativeButton("关闭", null)
            .create()
            .also { it.setOnDismissListener { release() } }
        dialog?.show()
    }

    private fun preparePlayer() {
        val media = MediaPlayer()
        val attributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_MEDIA)
            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            // Unmuted means the system generates the haptics itself, like it does for a ringtone.
            attributes.setHapticChannelsMuted(false)
        }
        media.setAudioAttributes(attributes.build())
        media.setOnPreparedListener { mp ->
            seekBar?.max = mp.duration.coerceAtLeast(1)
            playButton?.isEnabled = true
            if (resumePosition > 0) {
                mp.seekTo(resumePosition.toLong(), MediaPlayer.SEEK_CLOSEST)
                seekBar?.progress = resumePosition
                updateTime(resumePosition)
            } else {
                updateTime(0)
            }
            if (resumePlaying) {
                resumePlaying = false
                mp.start()
                playButton?.text = "暂停"
                startTicker()
            }
        }
        media.setOnCompletionListener {
            playButton?.text = "播放"
            player?.seekTo(0L, MediaPlayer.SEEK_CLOSEST)
            seekBar?.progress = 0
            updateTime(0)
        }
        media.setOnErrorListener { _, _, _ ->
            playButton?.isEnabled = false
            statusText?.text = "无法播放这个文件。"
            true
        }
        try {
            media.setDataSource(audio.absolutePath)
            media.prepareAsync()
            player = media
        } catch (e: Exception) {
            media.release()
            statusText?.text = "无法播放这个文件：${e.message}"
        }
    }

    private fun togglePlay() {
        val media = player ?: return
        if (media.isPlaying) {
            media.pause()
            playButton?.text = "播放"
            handler.removeCallbacks(ticker)
        } else {
            media.start()
            playButton?.text = "暂停"
            startTicker()
        }
    }

    /** Only runs while audio is playing, so the dialog stays idle the rest of the time. */
    private val ticker = object : Runnable {
        override fun run() {
            val media = player
            if (media != null && media.isPlaying) {
                val position = safePosition(media)
                seekBar?.progress = position
                updateTime(position)
                handler.postDelayed(this, TICK_MS)
            }
        }
    }

    private fun startTicker() {
        handler.removeCallbacks(ticker)
        handler.post(ticker)
    }

    private fun safePosition(media: MediaPlayer): Int = try {
        media.currentPosition
    } catch (_: IllegalStateException) {
        0
    }

    private fun updateTime(positionMs: Int) {
        timeLabel?.text = "${format(positionMs)} / ${format(durationMs())}"
    }

    private fun durationMs(): Int = try {
        player?.duration ?: 0
    } catch (_: IllegalStateException) {
        0
    }

    private fun format(ms: Int): String {
        val seconds = ms.coerceAtLeast(0) / 1000
        return String.format(Locale.US, "%02d:%02d", seconds / 60, seconds % 60)
    }

    private fun release() {
        handler.removeCallbacks(ticker)
        player?.release()
        player = null
    }

    private fun wrap() = LinearLayout.LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
    )

    private companion object {
        const val TICK_MS = 200L
    }
}
