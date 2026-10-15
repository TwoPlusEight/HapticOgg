# Haptic OGG

Convert ordinary audio into a three-channel Vorbis `.ogg`: the third channel carries a
low-frequency haptic track (45–180 Hz at 0.65 volume by default) and the file is tagged with
`android_haptic=1`, so the system can drive haptics while it plays the file. All processing is
local and the app has no network permission.

切换为 [中文](README.md) 版本.

## Requirements

| | |
| --- | --- |
| Android Studio | Giraffe or newer (or just the Gradle wrapper) |
| Android SDK | 35 (`compileSdk`/`targetSdk` 35, `minSdk` 26 → Android 8.0+) |
| JDK | 17 |
| Gradle / AGP | wrapper 9.8.1 / AGP 9.4.1 |

## Build

```bash
./gradlew assembleDebug        # Windows: .\gradlew.bat assembleDebug
```

The APK lands in `app/build/outputs/apk/debug/app-debug.apk` (~126 MB, the dependency ships every
codec). Add `--offline` when the Gradle and dependency caches are warm. The only dependency is
`com.arthenica:ffmpeg-kit-full-gpl:6.0-2`: upstream is retired, so a failing sync needs a
maintained fork or a self-built FFmpeg for Android with `libvorbis`. This is the GPL build - check
the licence obligations before distributing an APK.

## Usage

1. **First launch** opens the 导出音频授权 screen: may the app save every converted file into the
   system music directory?
   - *Agree* → results go to `Music/HapticOgg` (on Android 9 and older the storage permission is
     requested first).
   - *Decline* → pick a folder later with “修改导出文件位置”. Leaving the screen with the back
     gesture counts as a decline.
2. **1 · 选择音频** → “选择音频（可多选）”. Picking again **adds** to the list; every row has a
   移除 action and 清空选择 appears while something is selected (adding, removing and clearing
   confirm with a toast). The system picker has no cancel button, so leaving it keeps the current
   selection and a wrong pick is fixed in the app instead of by walking back out.
3. **2 · 导出位置** → “修改导出文件位置” offers the system music directory or any folder. The
   folder grant is persisted, the picker starts at the current folder, and leaving it without
   choosing changes nothing.
4. **3 · 转换与测试** → “开始转换” converts one file after another in the background; the status
   line shows `正在转换 x/n：<file>` and the report lists successes plus per-file error details
   (FFmpeg output included). With nothing selected the button only shows a toast.
   - In folder mode without a folder yet, the app asks first: pick one now (conversion starts
     right after), switch to the music directory, or cancel.
5. **已转换文件** lists the results, newest first, 5 at a time; tap 显示更多 and keep scrolling to
   page through the rest. Tap a row to play it (play/pause, seek, set as ringtone); swipe it left
   past ~45 % of its width to get a delete confirmation.
6. “测试振动（播放）” opens the player for the most recent conversion.

Playback keeps the haptic channels enabled, so Android 12+ generates the vibration from the file
itself; devices without haptic playback support (rotor motors, for example) can only audition the
audio. In-app playback can vibrate weaker than a real ringtone.

## Where files are written

| What | Where |
| --- | --- |
| Local copy (the list is built from these) | `Android/data/com.example.hapticogg/files/converted/`, gone when the app is uninstalled |
| Exported copy, music library | `Music/HapticOgg/` (MediaStore on Android 10+, public directory plus MediaScanner below that) |
| Exported copy, picked folder | same name inside the folder you picked (SAF) |
| Ringtone copy | `Ringtones/HapticOgg/` |

Names are `<source name without extension>_<yyyyMMdd_HHmmss>.ogg`; `\ / : * ? " < > |` and control
characters become `_`, the base is trimmed to 80 characters and falls back to `audio`. Collisions
in the public directory and in picked folders get `_2`, `_3`; through MediaStore the platform
decides. Deleting a row also removes the local copy, the leftover `<name>.env` envelope and every
exported copy the app can match (same name, or the same `_yyyyMMdd_HHmmss.ogg` suffix, including
the ringtone folder).

## Permissions

- `VIBRATE`, `WRITE_SETTINGS`: setting the system ringtone.
- `WRITE_EXTERNAL_STORAGE` (`maxSdkVersion="28"`): only to write the public music directory on
  Android 9 and older.

## Notes and troubleshooting

- Whether an incoming call plays the haptic track depends on the vendor and the system version:
  three channels plus `android_haptic=1` do not guarantee it. Ringtone settings and haptic audio
  support are two different things.
- Synced vibration needs Android 12+ and `AudioManager.isHapticPlaybackSupported()` (linear
  motor); rotor motors can only audition the audio.
- Vibration happens during playback only; if it stops mid-track, pause and play again.
- If “设为铃声” does nothing, grant the “modify system settings” permission first: the app opens
  that settings page, then tap “设为铃声” again.
- The “导出副本在 / 导出副本已不存在” text on a row is a name match against the media library and the
  picked folder; entries deleted by hand, or owned by another app, show as missing and cannot be
  removed from here.
- If the build or the Gradle sync fails, start with the dependency: FFmpegKit upstream is retired
  and needs a working mirror or fork.

## Code layout (quick reference)

Packages by responsibility: `ui/` (screens and list widgets), `conversion/` (FFmpeg command and
batch runner), `export/` (music library, picked folder, ringtone), `data/` (preferences and local
files).
