# Looma – Root Screen Recorder

Looma is an Android screen recorder that uses **root access** to invoke
`screenrecord` directly, giving you:

- **Auto-detected** or manually selected resolution
- Configurable frame rate (15–120 fps)
- Configurable bitrate
- Output saved to `/sdcard/SR/` by default
- MIUI/HyperOS-inspired UI via [Miuix](https://github.com/compose-miuix-ui/miuix)

## Build

Open in Android Studio and sync.  Then:

```bash
./gradlew assembleDebug
```

Install:

```bash
adb install app/build/outputs/apk/debug/app-debug.apk
```

## Dependencies

- Jetpack Compose + Material 3
- [Miuix](https://github.com/compose-miuix-ui/miuix) – HyperOS-style components
- Kotlin Coroutines

## Root

The app runs `su` to invoke `screenrecord`.  Grant root when prompted on
first launch.  The app will refuse to record without root.
