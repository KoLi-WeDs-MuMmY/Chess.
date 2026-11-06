# Grandmaster Slate — Offline AI Chess & Match Analysis

An Android chess application built with Kotlin, Jetpack Compose, and Room Database. Play against a 4-tier on-device AI engine (**Easy**, **Medium**, **Hard**, and **Impossible**), track your win streaks across difficulty levels, and review completed matches with an interactive centipawn evaluation bar, accuracy scores, and best-move arrows.

## Building & Downloading the APK on GitHub

This repository includes an automated GitHub Actions workflow at `.github/workflows/android-build.yml`:

1. Push this repository to GitHub (or trigger the workflow manually from the **Actions** tab via **Run workflow**).
2. GitHub Actions will automatically:
   - Configure JDK 21, the Android SDK, and Gradle (`9.3.1` via `gradle-wrapper.properties`).
   - Generate the required `debug.keystore` for debug signing.
   - Run the unit test suite (`:app:testDebugUnitTest`).
   - Compile the installable debug APK (`:app:assembleDebug`).
3. Once the workflow run completes, open the workflow summary page and download **`Grandmaster-Slate-debug-apk`** under the **Artifacts** section.
