# TripBudget

An offline-first Android app for planning group trips and sharing travel costs fairly.
Built with Kotlin and Jetpack Compose as a portfolio project.

> Status: **Phase 1** – trip list and "create trip" with travelers.

## Features (so far)

- Create trips with dates, budget, currency and fellow travelers
- Trip list with budget and number of travelers
- English and German UI (per-app language on Android 13+)
- Cost-splitting engine (domain layer, fully unit-tested): even split with cent-exact
  remainder handling, per-person balances, settlement suggestions

## Tech stack

Kotlin · Jetpack Compose · Material 3 · Navigation Compose · Room · Coroutines & Flow ·
MVVM · manual dependency injection (Hilt planned) · JUnit

## Architecture

```
ui/      Compose screens + ViewModels (unidirectional data flow)
domain/  Pure Kotlin: models, validation, cost splitting, repository interfaces
data/    Room entities, DAOs, repository implementation
di/      AppContainer (manual dependency injection)
```

## Security & privacy

- No internet permission – all data stays on the device
- No cloud backup / device transfer of app data (`data_extraction_rules.xml`)
- Strict input validation (amounts, text length, duplicates)
- Planned: encrypted database (SQLCipher + Android Keystore), biometric app lock

## Build

Open in Android Studio and run the `app` configuration. Unit tests: `./gradlew test`.
