# TripBudget

An offline-first Android app for planning group trips and sharing travel costs fairly.
Built with Kotlin and Jetpack Compose as a portfolio project.

> Status: **Phase 5b** – trip planner, expenses, budget, group settlement, encrypted storage, app lock and Hilt dependency injection.

## Features (so far)

- Create trips with dates, budget, currency and fellow travelers
- Trip list with budget and number of travelers
- Record expenses: amount, category, date, who paid, split between whom (cent-exact even split)
- Trip overview: remaining budget, daily budget, warning at 80 %, expenses grouped by day
- Day-by-day trip plan: activities with time, place and planned cost; planned costs shown in the budget; one tap turns an activity into a pre-filled expense
- Settle up: per-person balances (paid vs. share), suggested transfers (at most n−1), mark as paid / undo
- English and German UI, language switch in the app (Android 13+)
- Settings: app lock, screenshot protection, delete all data
- Cost-splitting engine (domain layer, fully unit-tested): even split with cent-exact
  remainder handling, per-person balances, settlement suggestions

## Tech stack

Kotlin · Jetpack Compose · Material 3 · Navigation Compose · Room · SQLCipher · DataStore · Biometric · Coroutines & Flow ·
MVVM · Hilt (dependency injection) · JUnit

## Architecture

```
ui/      Compose screens + ViewModels (unidirectional data flow)
domain/  Pure Kotlin: models, validation, cost splitting, repository interfaces
data/    Room entities, DAOs, repository implementation
di/      Hilt modules (database, repositories, clock, app lock)
```

## Security & privacy

- Encrypted database: SQLCipher (AES-256); random 256-bit passphrase protected by an Android Keystore key (AES-GCM)
- One-time, lossless migration of existing plaintext databases (`sqlcipher_export`)
- App lock with BiometricPrompt (biometrics or device credential), re-locks after 1 minute in background
- Optional `FLAG_SECURE` (no screenshots, hidden in recent apps)
- No internet permission – all data stays on the device
- Room database migrations: manual (v1→v2) and AutoMigrations (v2→v3→v4), schema history in `app/schemas/`
- No cloud backup / device transfer of app data (`data_extraction_rules.xml`)
- Strict input validation (amounts, text length, duplicates)

## Build

Open in Android Studio and run the `app` configuration. Unit tests: `./gradlew test`.
