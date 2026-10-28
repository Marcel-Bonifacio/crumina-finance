# Crumina for Android

Native Kotlin / Jetpack Compose client for Crumina. It talks to the existing backend at
`https://crumina.tirtawijata.com` over a Bearer-token API; the server still does all
mailbox reading, statement parsing, FX and quotes.

The architecture, the authentication flow, and the security controls are in
[../docs/ANDROID.md](../docs/ANDROID.md).

## Build

The signed APK is built in CI (`.github/workflows/android.yml`). On push to the `android`
branch it builds a debug APK to validate compilation, then a release APK (signed if the
keystore secrets are present), and uploads the APKs as a run artifact. Pushing a version
tag (`vX.Y.Z`) also publishes the signed APK as a pre-release on GitHub Releases.

Required repository secrets: `ANDROID_KEYSTORE_BASE64`, `ANDROID_KEYSTORE_PASSWORD`,
`ANDROID_KEY_ALIAS`, `ANDROID_KEY_PASSWORD`. See ../docs/ANDROID.md for how to generate
the keystore and read its SHA-256 for `assetlinks.json`.

Locally (needs JDK 17 + Android SDK):

```bash
cd android
gradle wrapper --gradle-version 8.7   # first time only, generates ./gradlew
./gradlew assembleDebug
```

## Screens

Five tabs: Overview (net worth, insights, a 6-month spending trend, budgets, recurring
payments, a carbon estimate and goals), Accounts (including PDF statement upload),
Activity (including manual expenses with on-device receipt OCR), Portfolio, and
Settings (currency, language, statement unlock, sync, log out). English and Bahasa
Indonesia.
