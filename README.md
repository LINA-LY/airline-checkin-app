# Airline Online Check-In App

Android mobile application for online check-in built with Kotlin.

---

## Tech Stack

| Layer | Technology |
|---|---|
| Language | Kotlin |
| UI | XML Layouts + Jetpack Navigation |
| Architecture | MVVM + Clean Architecture |
| Local DB | Room |
| Cloud / Auth | Firebase |
| DI | Hilt |
| Async | Kotlin Coroutines + StateFlow |
| Networking | Retrofit + OkHttp |
| OCR | Google ML Kit |
| QR Code | ZXing |

---

## Project Structure

```
app/src/main/java/com/airline/checkin/
├── data/
│   ├── local/          # Room DB, DAOs, Entities
│   ├── remote/         # Firebase, Retrofit
│   └── repository/     # Repositories
├── domain/
│   ├── model/          # Data classes
│   └── usecase/        # Use cases (optional)
├── ui/
│   ├── auth/           # Login, Register screens
│   ├── checkin/        # Check-in flow screens
│   ├── seat/           # Seat map screen
│   ├── boardingpass/   # Boarding pass screen
│   └── common/         # Shared UI components
└── di/                 # Hilt modules
```

---

## Getting Started

### Prerequisites
- Android Studio Hedgehog or later
- JDK 17
- Android SDK (min API 26)
- Git

### Setup
1. Clone the repository
   ```
   git clone https://github.com/YOUR_ORG/airline-checkin-app.git
   ```
2. Open in Android Studio
3. Add your `google-services.json` from Firebase Console into `app/`
4. Sync Gradle
5. Run on emulator or device (API 26+)

---

## Branch Strategy

| Branch | Purpose |
|---|---|
| `main` | Stable, production-ready code |
| `dev` | Integration branch |
| `feature/auth` | Authentication feature |
| `feature/checkin` | Check-in flow |
| `feature/seatmap` | Seat selection |
| `feature/boardingpass` | Boarding pass & QR |

**Never push directly to `main`.**
Always open a Pull Request to `dev` and request a review.

---

## Commit Message Convention

```
feat:     new feature
fix:      bug fix
ui:       UI changes
refactor: code refactor, no feature change
docs:     documentation only
test:     adding tests
chore:    build, config, dependency updates
```

Example: `feat: add passport OCR scan screen`

---

