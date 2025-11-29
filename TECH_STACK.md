# FoodYou Tech Stack

## Core Architecture
*   **Language**: [Kotlin](https://kotlinlang.org/) (v2.0+)
*   **Platform Strategy**: **Kotlin Multiplatform (KMP)**. The project is set up to share code between **Android** and **iOS** (targets `iosX64`, `iosArm64`, `iosSimulatorArm64` are defined).
*   **Build System**: Gradle (Kotlin DSL).

## User Interface
*   **Framework**: **Compose Multiplatform** (Jetpack Compose for Android, Compose for iOS).
*   **Design System**: **Material Design 3** (Material You).
*   **Theming**: [Material Kolor](https://github.com/jordond/MaterialKolor) for dynamic, seed-based color schemes.
*   **Navigation**: Navigation Compose.

## Data & Networking
*   **Database**: **Room** (v2.8+) with SQLite for local storage.
*   **Networking**: **Ktor Client** (v3.0+) for API requests.
*   **Serialization**: `kotlinx.serialization` (JSON).
*   **Image Loading**: **Coil 3** (Multiplatform).
*   **Date & Time**: `kotlinx-datetime`.

## State Management & DI
*   **Dependency Injection**: **Koin** (v4.0+).
*   **Concurrency**: Kotlin Coroutines & Flow.

## Other Key Libraries
*   **Barcode Scanning**: ZXing (via `zxing-android-embedded` and a shared wrapper).
*   **Permissions**: Accompanist Permissions.
*   **File Handling**: FileKit.
