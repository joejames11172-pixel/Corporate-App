HERESHIFT 0.4.0 DEVELOPMENT — 2026-10-17
SOURCE: Development/Apps/HereShift/Android App on corporate Google Drive

CONTENTS
- Android Studio / Gradle Kotlin Android project; package com.peostectica.hereshift
- API min 26, compile & target 36, Android Gradle Plugin 8.10.1, Kotlin 2.1.20, Gradle 8.13
- Explicitly user-initiated mock location, foreground service + Stop action
- 40 approved theme pairs, 20 audio themes (12 WAV cues each)
- HereShift light-icon #15, dark-icon #20 (original editable SVG sources plus current raster app resources)
- No network, billing, analytics, contacts, microphone, or remote-control functionality

BUILD LOCALLY ON MODERN PC WITH ANDROID SDK
1. Install Android Studio on a supported Windows/Linux/macOS computer. Alpine i686 cannot build this Android project.
2. Install Android SDK Platform 36, build tools 36.0.0, Java 17, Gradle 8.13.
3. Open root directory (directory containing settings.gradle.kts). Let Gradle sync.
4. Run `gradle :app:assembleDebug` or choose Build APK(s) in Android Studio.
5. Debug APK: app/build/outputs/apk/debug/app-debug.apk. NOT yet verified as generated.
6. On own Android phone enable USB debugging and install the debug APK for consented testing.
7. To use location simulation enable Developer options > Select mock location app > HereShift.
8. Confirm visible ACTIVE notification, test point, Stop restores provider, and normal location returns. Do not test bypass of wagering geolocation protections.

BUILD IN CLOUD
A ready-to-run GitHub Actions workflow is in .github/workflows/build-debug-apk.yml. It builds a DEBUG APK on x86_64 Ubuntu with SDK Platform 36 and no release signing secrets. A dedicated authorized repository containing the project root is necessary; not yet connected. Do not publish proprietary source in unrelated repositories without CEO permission.

CRITICAL RELEASING / NOT YET COMPLETE
- No APK has been built or tested on a real Android phone.
- Native UI and foreground-service behavior have NOT been compiled against the Android SDK in this workspace.
- Independent auditors must accept production files; Play Store signing and production credentials need owner authorization.
- Mock locations are visible as mock and cannot guarantee that other applications accept them.
- Verify exact Play policy declarations, app permissions and Privacy Policy against the compiled APK.
- A launch-ready release needs official privacy/contact/store assets, enrollment, Play Console forms, signed AAB, and device tests.

APPLICATION SECURITY
No code to circumvent integrity or regulatory geolocation protection. Mock locations require Android Developer Options user authorization and honor Stop requests. Devices may require location permission under foreground service constraints even though no actual GPS fix is read by this app.