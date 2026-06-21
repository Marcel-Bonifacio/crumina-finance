# Crumina for Android

Native Kotlin / Jetpack Compose client for Crumina. It talks to the existing backend at
`https://crumina.tirtawijata.com` over a Bearer-token API; the server still does all
mailbox reading, statement parsing, FX and quotes.

Full design, the authentication redesign, and the Google security controls are in
[../docs/ANDROID.md](../docs/ANDROID.md).

## Build

The signed APK is built in CI (`.github/workflows/android.yml`). On push to the `android`
branch it builds a debug APK to validate compilation, then a signed release APK if the
keystore secrets are present, and uploads the APK as a run artifact.

Required repository secrets: `ANDROID_KEYSTORE_BASE64`, `ANDROID_KEYSTORE_PASSWORD`,
`ANDROID_KEY_ALIAS`, `ANDROID_KEY_PASSWORD`. See ../docs/ANDROID.md for how to generate
the keystore and read its SHA-256 for `assetlinks.json`.

Locally (needs JDK 17 + Android SDK):

```bash
cd android
gradle wrapper --gradle-version 8.7   # first time only, generates ./gradlew
./gradlew assembleDebug
```

## Status

Foundation: secure auth (Custom Tab + verified App Link), Keystore-backed token storage,
the network layer, and an Overview screen reading `/api/data`. Accounts, Activity,
Insights, Budgets, Portfolio and Settings are the next increments.
