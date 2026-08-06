# 🏗️ Technical Architecture — KhataTrack

**KhataTrack** follows modern Android app development best practices recommended by Google, utilizing Jetpack Compose for declarative UI, Unidirectional Data Flow (UDF), clean architecture, and modular data sources.

---

## 📐 Architecture Overview

```
+-------------------------------------------------------------+
|                        UI Layer                             |
|  - Jetpack Compose Screens & Material 3 Components          |
|  - ViewModels & StateFlow Management                        |
+-------------------------------------------------------------+
                              │
                              ▼
+-------------------------------------------------------------+
|                      Domain / Service Layer                 |
|  - Transaction & Contact Repositories                       |
|  - AlarmManager Background Scheduler & BootReceiver         |
|  - Voice Audio Recording Service                            |
+-------------------------------------------------------------+
                              │
               ┌──────────────┴──────────────┐
               ▼                             ▼
+-----------------------------+ +-----------------------------+
|      Local Data Source      | |      Cloud / AI Layer       |
|  - Room Database (SQLite)   | |  - Firebase Auth & Firestore|
|  - Encrypted SharedPreferences|  - Server-Side Gemini API   |
+-----------------------------+ +-----------------------------+
```

---

## 🔑 Key Components

### 1. UI Layer (`Jetpack Compose`)
- Multi-screen navigation handled by `androidx.navigation.compose`.
- UI State is exposed via `StateFlow` and consumed seamlessly in Compose components.

### 2. Local Ledger (`Room Database`)
- Entities: `Transaction`, `Contact`, `ReminderTag`.
- Data Access Objects (DAOs) return coroutine Flow streams for reactive UI updates.

### 3. Background Reminders (`AlarmManager` & `BootReceiver`)
- Schedules high-precision alarms for debt payment notifications.
- Listens to `android.intent.action.BOOT_COMPLETED` to reschedule alarms automatically upon device restart.

### 4. Cloud & AI Infrastructure
- Firebase Authentication supports user identity.
- Firebase Firestore enables optional cloud backup & sync.
- Firebase AI / Gemini API facilitates smart voice-to-ledger extraction and automated category tagging.
