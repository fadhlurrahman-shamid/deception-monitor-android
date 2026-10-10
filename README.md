# Web and Mobile Deception Based Threat Monitoring System (Android)

Final Year Project, Bachelor of Information Technology (Hons) in Cybersecurity, Universiti Poly-Tech Malaysia.

This repository holds the Android decoy staff app. It looks like a real company staff app with simulated data. Every login attempt and screen visit is sent to the decoy API in the `deception-monitor-web` repository, where it is recorded and scored on the security dashboard.

## Tech stack

Java, Android Studio, Gradle (Kotlin DSL).

## Setup

1. Open the project in Android Studio.
2. Add these lines to `local.properties` (this file is not committed):

   ```
   decoy.apiBaseUrl=https://<your-domain>/
   decoy.apiKey=<same value as DECOY_API_KEY on the server>
   ```

3. Build and run the app. Without these values the app falls back to `http://10.0.2.2/deception-monitor/` for the Android emulator.
