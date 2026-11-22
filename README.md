# My Video Downloader

A starter Android app built with Kotlin and Jetpack Compose.

## Build
1. Install Android Studio (a recent stable version).
2. Open this project folder.
3. Allow Gradle sync to finish.
4. Connect an Android device or start an emulator.
5. Select Run.

## Features
- Paste direct HTTP/HTTPS video file links.
- Built-in WebView browser.
- Uses Android DownloadManager for background downloads and system notifications.
- Basic in-session download history.

## Important limitations
This is a safe starter implementation, not a universal extractor. Many social networks use authenticated APIs, segmented HLS/DASH streams, blob URLs, or DRM. A WebView download listener only sees downloads exposed by the website as ordinary downloadable files. Add official platform APIs or licensed provider integrations where permitted. Do not bypass DRM, paywalls, authentication, or access controls. Only save content you own or have permission to download.

## Notes
- Downloads are saved to the device's public Downloads folder.
- The library currently tracks downloads only for the current app session; Android's Downloads app/notification shade is the persistent source of truth.
- Test on a physical device as well as an emulator.


## Build without installing Android Studio (GitHub Actions)
1. Create a GitHub account if you do not already have one.
2. Create a new empty repository on GitHub.
3. Upload the contents of this project ZIP to the repository (keep the `.github/workflows/build-apk.yml` file and its folders).
4. Open the repository's **Actions** tab. The workflow named **Build Android APK** should start automatically after a push; alternatively choose **Run workflow**.
5. When the run completes, open it and download the `my-video-downloader-debug-apk` artifact. Extract it to get `app-debug.apk`.
6. Transfer the APK to your Android phone, open it, and allow installation from that source if Android asks.

This workflow builds the debug APK in GitHub's cloud; it does not require Android Studio on your phone or computer.
