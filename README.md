# Airline Online Check-In App

A comprehensive Android mobile application for booking flights, managing passengers, and seamless online check-in. Built entirely with modern Android development practices using **Kotlin** and **Jetpack Compose**.

---

## Features

- **Flight Booking System**: Search for flights with advanced date-range filtering, view detailed itineraries (including multi-stop logic), and select your preferred cabin class.
- **Passenger Management**: Add multiple passengers to a booking, securely save traveler profiles to your account, and auto-fill details for future flights.
- **Dynamic Seat Selection**: Interactive graphical seat map allowing users to select seats per passenger with real-time price calculation based on cabin class and seat type.
- **Offline-First Boarding Passes**: Generate boarding passes dynamically. Passes are securely cached locally using Room, allowing users to view their passes and QR codes even without an active internet connection.
- **PDF Generation**: Export boarding passes directly to PDF format for easy sharing or printing.
- **Baggage & Special Requests**: Declare checked/carry-on bags and request special assistance directly within the app during the booking process.

---

## Tech Stack

| Layer          | Technology                        |
|----------------|-----------------------------------|
| **Language**   | Kotlin                            |
| **UI**         | Jetpack Compose, Material 3       |
| **Architecture**| MVVM + Clean Architecture        |
| **Local DB**   | Room (Offline Caching)            |
| **Cloud / Auth**| Firebase (Firestore + Auth)      |
| **DI**         | Dagger Hilt                       |
| **Async**      | Kotlin Coroutines + StateFlow     |
| **Navigation** | Jetpack Navigation Compose        |
| **QR Code**    | ZXing Core                        |

---

## Project Structure

```text
app/src/main/java/com/airline/checkin/
├── data/
│   ├── local/            # Room DAOs, Entities, and AppDatabase
│   ├── remote/           # Firebase Services (Auth, Firestore)
│   └── repository/       # Repositories (Offline-first data sync)
├── di/                   # Hilt Dependency Injection Modules
├── domain/
│   └── model/            # Core business models
├── ui/                   # Jetpack Compose UI layer (MVVM pattern)
│   ├── auth/             # Login, Registration, Complete Profile
│   ├── boardingpass/     # Boarding pass display and PDF generation
│   ├── booking/          # Flight search, results, passenger info, payment
│   ├── checkin/          # My Bookings and online check-in flows
│   ├── common/           # Reusable shared UI components
│   ├── home/             # Main dashboard
│   ├── onboarding/       # App introduction walkthrough
│   ├── profile/          # User profile and saved travelers management
│   └── seat/             # Interactive graphical seat map
└── MainActivity.kt       # Application entry point
```

---

## Getting Started

### Prerequisites
- Android Studio Hedgehog or later
- JDK 17 (bundled with Android Studio)
- Android SDK API 26+

### Setup Instructions

1. **Clone the repository**
   ```bash
   git clone https://github.com/YOUR_USERNAME/airline-checkin-app.git
   ```

2. **Open in Android Studio**
   - Launch Android Studio, click **Open**, and select the cloned folder.
   - Wait for the initial Gradle sync to complete.

3. **Configure Firebase**
   - Go to the [Firebase Console](https://console.firebase.google.com).
   - Create a new project or select an existing one.
   - Add an Android app with the package name: `com.airline.checkin`.
   - Download the generated `google-services.json` file.
   - Place `google-services.json` into the project's `app/` directory.
   - *(Note: Ensure Firestore and Firebase Authentication are enabled in your console).*

4. **Run the App**
   - Connect a physical device or start an Android Emulator (API 26+).
   - Click **Run** or press `Shift + F10`.

---

## Architecture Overview

This project follows **Clean Architecture** principles and the **MVVM (Model-View-ViewModel)** pattern:

- **UI Layer**: Composed of Jetpack Compose screens and ViewModels. ViewModels manage UI state and interact with the Domain/Data layers.
- **Domain Layer**: Contains business models (`domain/model/`) that are independent of any specific framework.
- **Data Layer**: Responsible for data fetching and persistence. It uses the Repository pattern to abstract data sources (Room for local caching and Firebase for remote storage). 

This clear separation of concerns ensures that the codebase is highly maintainable, testable, and scalable.
