# CipherVault Evaluation & Setup Guide

## 1. Prerequisites

- **JDK**: Java 21 LTS (or OpenJDK 21)
- **Database**: MySQL Server 8.0+ running on port `3306`
- **Android**: Android Studio with Android SDK Platform 37 (Min SDK: 26, Android 8.0+)
- **Build Tools**: Apache Maven (wrapper included: `./mvnw.cmd`), Gradle 9.6.0 (wrapper included: `./gradlew.bat`)
- **Optional Tools**: ExifTool on system PATH for camera/lens EXIF metadata extraction (gracefully falls back to pure Java if absent)

---

## 2. Database Configuration

1. Launch MySQL CLI or Workbench and create the database user:
```sql
CREATE DATABASE IF NOT EXISTS ciphervault CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE USER IF NOT EXISTS 'ciphervault_user'@'localhost' IDENTIFIED BY 'CipherVault@2026';
GRANT ALL PRIVILEGES ON ciphervault.* TO 'ciphervault_user'@'localhost';
FLUSH PRIVILEGES;
```

2. Schema and indexes are automatically verified and managed by Spring Data JPA / Hibernate upon backend boot.

---

## 3. Launching Backend

In a PowerShell terminal:
```powershell
cd C:\Users\akhil\OneDrive\Desktop\CipherVault\backend
.\mvnw.cmd spring-boot:run
```
Backend will start on port `8080`. Verify health:
```powershell
curl http://localhost:8080/api/health
```

---

## 4. Connecting Android Device / Emulator

### Option A: Android Studio Emulator
The emulator automatically maps host loopback to `10.0.2.2`.
1. Open CipherVault on emulator.
2. In the Login screen, tap **Server Settings**.
3. Tap the **Emulator (10.0.2.2)** preset chip.
4. Tap **Check Connection** -> Green checkmark indicates active link!

### Option B: Physical Phone via USB Cable
1. Enable USB Debugging on your phone.
2. Run ADB reverse in terminal:
```powershell
adb reverse tcp:8080 tcp:8080
```
3. In CipherVault, tap **Server Settings**, select **ADB Reverse (127.0.0.1)**.
4. Tap **Check Connection** -> Connected!

### Option C: Physical Phone via Local Wi-Fi
1. Ensure phone and computer are on the same Wi-Fi network.
2. Find your PC's IP via `ipconfig` (e.g. `192.168.1.50`).
3. In CipherVault **Server Settings**, enter `192.168.1.50:8080` and tap **Check Connection**.

---

## 5. Running Automated Verification Suites

### Backend Unit & Integration Tests:
```powershell
cd C:\Users\akhil\OneDrive\Desktop\CipherVault\backend
.\mvnw.cmd test
```
*Expected output: `Tests run: 24, Failures: 0, Errors: 0, Skipped: 0. BUILD SUCCESS`*

### Android Unit Tests:
```powershell
cd C:\Users\akhil\OneDrive\Desktop\CipherVault\android
.\gradlew.bat testDebugUnitTest
```
*Expected output: `BUILD SUCCESSFUL`*

### Android APK Assembly:
```powershell
cd C:\Users\akhil\OneDrive\Desktop\CipherVault\android
.\gradlew.bat assembleDebug
```
*Output APK located at: `android/app/build/outputs/apk/debug/app-debug.apk`*
