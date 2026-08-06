# AI Agent Instructions for KhataTrack

Welcome to **KhataTrack**, an Android personal ledger application built with Kotlin, Jetpack Compose, Room Database, and Firebase services by **Souvik Dey** (`@imsovikde`).

---

## 🛠️ Essential Commands

### Build & Compilation
- **Assemble Debug APK**: `./gradlew assembleDebug`
- **Assemble Release APK**: `./gradlew assembleRelease`
- **Clean Project**: `./gradlew clean`

### Testing & Verification
- **Run Unit Tests**: `./gradlew testDebugUnitTest`
- **Run Android Lint**: `./gradlew lint`
- **Roborazzi Screenshot Tests**: `./gradlew verifyRoborazziDebug`

---

## 🏛️ Architecture & Conventions

1. **UI Layer**: Built entirely using Jetpack Compose and Material 3 (`androidx.compose.material3`).
2. **Data Layer**: Local storage powered by Room Database (`androidx.room`). Network/Cloud storage backed by Firebase Auth & Firestore.
3. **Background Services**: `AlarmManager` and `WorkManager` handle scheduled reminders and persistent state recovery across reboots (`BootReceiver`).
4. **Environment Variables**: Managed via `.env` (development) and `.env.example` (template). Secrets Gradle plugin injects `.env` variables into `BuildConfig`.

---

## ⚠️ Files to Avoid Editing Directly

- `gradlew` / `gradlew.bat`: Official Gradle wrapper scripts.
- `.github/workflows/release-apk.yml`: Automated release pipeline configuration.
- `my-upload-key.jks`: Key store file (if present).

---

## 🛡️ Guidelines & Quality Expectations

- Maintain 100% Kotlin 2.0+ compatibility with strong null safety.
- Do not commit actual secrets, API tokens, or keystore credentials to version control.
- Ensure all unit tests pass before submitting changes.
