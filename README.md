# iroha-demo-android

This sample app now targets the latest Android platform requirements (Android 15 / API 35 as of November 2025) and has been fully migrated to AndroidX + Material Components.

## Build requirements

- Android Studio Ladybug or newer with AGP `8.5.0` support
- JDK 17+
- Android 15 (API 35) SDK and build-tools installed
- Gradle Wrapper 8.7 (already configured via `./gradlew`)

## Notes

- Legacy Fabric Crashlytics has been removed. A lightweight `CrashReporter` wrapper is in place so a modern crash backend (e.g., Firebase Crashlytics) can be plugged in later without touching the presenters again.
- Minimum supported OS level is now Android 7.0 (API 24) to comply with the modern toolchain requirements.
