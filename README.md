# Airline Online Check-In App

Android mobile application for online check-in built with Kotlin & Jetpack Compose.

---

## Tech Stack

| Layer          | Technology                        |
|----------------|-----------------------------------|
| Language       | Kotlin                            |
| UI             | Jetpack Compose                   |
| Architecture   | MVVM + Clean Architecture         |
| Local DB       | Room                              |
| Cloud / Auth   | Firebase (Firestore + Auth)       |
| DI             | Hilt                              |
| Async          | Kotlin Coroutines + StateFlow     |
| Navigation     | Jetpack Navigation Compose        |
| OCR            | Google ML Kit                     |
| QR Code        | ZXing                             |

---

## Project Structure

```
app/src/main/java/com/airline/checkin/
├── data/
│   ├── local/
│   │   ├── dao/          # Room DAOs
│   │   ├── entity/       # Room Entities
│   │   └── AppDatabase.kt
│   ├── remote/
│   │   └── firebase/     # FirebaseService
│   └── repository/       # Repositories
├── domain/
│   └── model/            # Data classes
├── ui/
│   ├── auth/             # Login, Register (Member 1)
│   ├── checkin/          # Check-in flow (Member 2)
│   ├── seat/             # Seat map (Member 3)
│   ├── boardingpass/     # Boarding pass (Member 4)
│   ├── common/           # Shared UI components
│   └── AppNavGraph.kt    # Navigation
├── di/                   # Hilt modules
└── MainActivity.kt
```

---

## Getting Started

### Prerequisites
- Android Studio Hedgehog or later
- JDK 17 (bundled with Android Studio)
- Android SDK API 26+
- Git

### Setup

1. Clone the repository
```bash
git clone https://github.com/YOUR_USERNAME/airline-checkin-app.git
```

2. Open in Android Studio
   - Launch Android Studio
   - Click **Open**
   - Select the cloned folder
   - Wait for Gradle sync to finish

3. Add Firebase config
   - Go to [Firebase Console](https://console.firebase.google.com)
   - Create a project → Add Android app
   - Package name: `com.airline.checkin`
   - Download `google-services.json`
   - Place it in the `app/` folder

4. Run the app
   - Connect a device or start an emulator (API 26+)
   - Click **Run** or press `Shift + F10`

---

## Branch Strategy

| Branch                  | Purpose                  | Owner        |
|-------------------------|--------------------------|--------------|
| `main`                  | Stable code              | Team Lead    |
| `dev`                   | Integration branch       | Everyone     |
| `feature/auth`          | Auth screens             | Member 1     |
| `feature/checkin`       | Check-in flow            | Member 2     |
| `feature/seatmap`       | Seat map                 | Member 3     |
| `feature/boardingpass`  | Boarding pass & QR       | Member 4     |

**Rules:**
- Never push directly to `main`
- Create your branch from `main`
- Open a Pull Request to `dev` when your feature is ready
- At least 1 member must review before merging

---

## Commit Convention

```
feat:      new feature or screen
fix:       bug fix
ui:        UI / layout changes
refactor:  code restructure, no behaviour change
docs:      documentation only
test:      adding or fixing tests
chore:     gradle, dependencies, config
```

**Examples:**
```
feat: add passport OCR scan screen
fix: boarding pass not loading offline
ui: update seat map colors for premium seats
chore: add ZXing dependency
```

---

## Team

| Member   | Role                              |
|----------|-----------------------------------|
| Member 1 | Team Lead — Architecture & Auth   |
| Member 2 | Check-In Flow & Booking Lookup    |
| Member 3 | Seat Map & UI/UX                  |
| Member 4 | Boarding Pass, QR Code & Research |
