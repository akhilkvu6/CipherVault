# CipherVault — Comprehensive Operations & How-To-Run Guide

> **Official Step-by-Step Operator Manual**  
> Complete setup, execution, health verification, mobile connection, and teardown instructions for developers and evaluators.

---

## Table of Contents

1. [Prerequisites & System Verification](#1-prerequisites--system-verification)
2. [Database Provisioning (MySQL 8.0+)](#2-database-provisioning-mysql-80)
3. [Spring Boot Backend Execution](#3-spring-boot-backend-execution)
4. [Python Server Manager (Desktop GUI Alternative)](#4-python-server-manager-desktop-gui-alternative)
5. [Backend Health Verification & Smoke Testing](#5-backend-health-verification--smoke-testing)
6. [Android Application Build & Installation](#6-android-application-build--installation)
7. [Automated Verification & Test Execution](#7-automated-verification--test-execution)
8. [First-Time User Walkthrough](#8-first-time-user-walkthrough)
9. [Graceful Shutdown & Teardown](#9-graceful-shutdown--teardown)

---

## 1. Prerequisites & System Verification

Before starting any CipherVault subsystem, verify that your development workstation has the required runtimes and tools installed.

### 1.1 Java Development Kit (JDK 21 LTS)
CipherVault backend and Android Gradle builds require JDK 21.
```powershell
java -version
javac -version
```
*Expected output: `openjdk version "21.x.x"` or `java version "21.x.x"`.*  
If multiple Java versions exist, ensure `JAVA_HOME` points to your JDK 21 installation:
```powershell
$env:JAVA_HOME = "C:\Program Files\Java\jdk-21"
```

### 1.2 MySQL Server 8.0+
Ensure MySQL Server 8.0 or later is installed and running on default TCP port `3306`.
```powershell
mysql --version
```
*Expected output: `mysql  Ver 8.0.x for Win64...`*

### 1.3 Android SDK & Tools
CipherVault targets Android SDK API 37 with minimum SDK 26 (Android 8.0 Oreo).
- **Android Studio**: Android Studio Ladybug or later recommended.
- **Android SDK Platforms**: Platform API 37 installed (`Android SDK Platform 37`).
- **Build Tools**: Version `35.0.0` or compatible.
- **ADB (Android Debug Bridge)**: Ensure `adb` is available in your system `PATH`:
  ```powershell
  adb --version
  ```

### 1.4 Python 3.10+ (Optional for Desktop Server Manager)
The desktop Server Control GUI requires Python 3.10 or higher.
```powershell
python --version
```

### 1.5 ExifTool (Optional)
CipherVault's backend integrates with ExifTool for deep lens, camera, and document metadata extraction. If ExifTool is not present on your system `PATH`, CipherVault gracefully falls back to native Java metadata extraction without throwing errors.

---

## 2. Database Provisioning (MySQL 8.0+)

CipherVault stores encrypted file indexes, deduplication checksums, user accounts, and extracted metadata in MySQL.

### 2.1 Start MySQL Service
If MySQL is installed as a Windows service:
```powershell
# In an Administrator PowerShell terminal:
net start MySQL80
```
Alternatively, verify the service status:
```powershell
Get-Service -Name "*mysql*"
```

### 2.2 Create Database & User
Open the MySQL interactive command line or MySQL Workbench:
```powershell
mysql -u root -p
```
Execute the initialization script:
```sql
-- 1. Create database with UTF-8 support
CREATE DATABASE IF NOT EXISTS ciphervault CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- 2. Create the application user (replace placeholders with your preferred credentials)
CREATE USER IF NOT EXISTS '<MYSQL_USERNAME>'@'localhost' IDENTIFIED BY '<MYSQL_PASSWORD>';

-- 3. Grant full privileges on the ciphervault schema
GRANT ALL PRIVILEGES ON ciphervault.* TO '<MYSQL_USERNAME>'@'localhost';

-- 4. Flush privilege tables
FLUSH PRIVILEGES;
```

> [!NOTE]
> By default, `backend/src/main/resources/application.properties` reads the credentials from environment variables:
> - `CIPHERVAULT_DB_USERNAME` (defaults to `ciphervault_user`)
> - `CIPHERVAULT_DB_PASSWORD` (defaults to `CipherVault@2026`)
>
> If you customize your credentials, set these environment variables before running the backend or update your local properties.

### 2.3 Schema Management
CipherVault employs Spring Data JPA with `spring.jpa.hibernate.ddl-auto=update`. When the backend launches, Hibernate automatically verifies, creates, or updates all tables (`users`, `stored_files`, `file_metadata`) and database constraints. The full schema specification is also available at `database/schema.sql`.

---

## 3. Spring Boot Backend Execution

The backend server is responsible for authentication, cryptographic key derivation, multipart file storage, SHA-256 deduplication, and forensic metadata parsing.

### 3.1 Working Directory
The Maven wrapper (`mvnw.cmd`) and `pom.xml` reside in the `backend/` directory.

### 3.2 Launch via Maven Wrapper
Open a PowerShell terminal and navigate to the backend directory:
```powershell
cd C:\Users\akhil\OneDrive\Desktop\CipherVault\backend
.\mvnw.cmd spring-boot:run
```

### 3.3 Configuration Highlights
The backend runs with the following production defaults (configured in `application.properties`):
- **Server Port**: `8080` (`server.port=8080`)
- **Bind Address**: `0.0.0.0` (`server.address=0.0.0.0`) — binds to all local network adapters, allowing connections from the Android Emulator, USB reverse, local Wi-Fi LAN, and Mobile Hotspot.
- **Multipart Max File Size**: `2GB` (`spring.servlet.multipart.max-file-size=2GB`)
- **Multipart Request Size**: `2GB` (`spring.servlet.multipart.max-request-size=2GB`)
- **Tomcat Connection Timeout**: `600,000 ms` (10 minutes for multi-gigabyte transfers)
- **Async Timeout**: `1,200,000 ms` (20 minutes)

### 3.4 Startup Log Verification
When the backend starts successfully, the following output appears in your terminal:
```text
Tomcat started on port 8080 (http) with context path '/'
Started CipherVaultApplication in X.XXX seconds
```

---

## 4. Python Server Manager (Desktop GUI Alternative)

For developers who prefer a visual desktop control panel, CipherVault includes a PySide6 GUI utility in `server-control/`.

### 4.1 First-Time Setup
In a terminal, navigate to the `server-control/` directory and install the required Python packages:
```powershell
cd C:\Users\akhil\OneDrive\Desktop\CipherVault\server-control
pip install -r requirements.txt
```

### 4.2 Launch the Manager
Run the Windows batch launcher:
```powershell
.\run.bat
```
Or execute directly via Python:
```powershell
python main.py
```

### 4.3 Server Manager Capabilities
- **1-Click Backend Process Control**: Start, stop, and restart the Spring Boot backend process with live output log streaming.
- **Network Interface Auto-Detection**: Automatically detects your workstation's Wi-Fi LAN IP, Ethernet IP, Hotspot IP, and ADB USB device status.
- **One-Click ADB Reverse**: Automatically executes `adb reverse tcp:8080 tcp:8080` when an Android device is plugged in via USB.
- **Real-Time Health Polling**: Sends background probes every 3 seconds to ensure port `8080` is healthy and responsive.

---

## 5. Backend Health Verification & Smoke Testing

Always verify backend health before launching the Android client.

### 5.1 Public Health Endpoint
CipherVault exposes an unauthenticated health probe at `GET /api/health`.

### 5.2 Verification via cURL or PowerShell
```powershell
# Using cURL:
curl http://localhost:8080/api/health

# Or using native PowerShell:
Invoke-RestMethod -Uri "http://localhost:8080/api/health" -Method Get | ConvertTo-Json
```

### 5.3 Expected Response
```json
{
  "status": "UP",
  "service": "CipherVault Backend",
  "timestamp": "2026-10-08T10:45:00.123456Z",
  "components": {
    "backend": "HEALTHY",
    "database": "HEALTHY",
    "storage": "HEALTHY",
    "encryption": "HEALTHY",
    "metadata": "HEALTHY",
    "transfers": "HEALTHY"
  }
}
```

### 5.4 Subsystem Diagnostic Meaning
- `backend`: Core Spring Boot application context is running.
- `database`: Active MySQL connection confirmed via `userRepository.count()`.
- `storage`: Physical storage folder `storage/vault/` verified on disk.
- `encryption`: AES-256-GCM cipher suite initialized.
- `metadata`: ExifTool process engine / fallback ready.
- `transfers`: File transfer pipeline operational.

> [!IMPORTANT]
> The Spring Actuator endpoint `/actuator/health` requires JWT authentication and will return `HTTP 401 Unauthorized` if hit without a token. Always use `/api/health` for connection testing.

---

## 6. Android Application Build & Installation

### 6.1 Option A: Launch via Android Studio (Recommended)
1. Launch Android Studio.
2. Select **Open** and choose the `C:\Users\akhil\OneDrive\Desktop\CipherVault\android` directory.
3. Allow Gradle to sync dependencies.
4. Select your target device (Physical Device via USB or Android Virtual Device).
5. Click the green **Run (Shift+F10)** button.

### 6.2 Option B: Compile & Install via Command Line
Open a terminal in the `android/` directory:

```powershell
cd C:\Users\akhil\OneDrive\Desktop\CipherVault\android

# Build the debug APK:
.\gradlew.bat assembleDebug
```
The output APK is generated at:
```text
android/app/build/outputs/apk/debug/app-debug.apk
```

Install directly onto an attached device via ADB:
```powershell
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

Launch the application:
```powershell
adb shell am start -n com.ciphervault.app/.SplashActivity
```

---

## 7. Automated Verification & Test Execution

Before certifying a deployment, run both backend and Android test suites.

### 7.1 Backend Unit & Integration Tests
Navigate to the `backend/` directory:
```powershell
cd C:\Users\akhil\OneDrive\Desktop\CipherVault\backend
.\mvnw.cmd test
```
*Expected output:*
```text
[INFO] Results:
[INFO] Tests run: 24, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS
```

### 7.2 Android Unit Tests
Navigate to the `android/` directory:
```powershell
cd C:\Users\akhil\OneDrive\Desktop\CipherVault\android
.\gradlew.bat testDebugUnitTest
```
*Expected output:*
```text
BUILD SUCCESSFUL in Xs
```

---

## 8. First-Time User Walkthrough

When launching CipherVault for the first time:

1. **Splash Screen**: Displays the CipherVault emblem and initializes the security subsystem.
2. **Onboarding Carousel**: Introduces the three core security pillars:
   - *Zero-Knowledge Storage*: Envelope encryption with client-isolated master keys.
   - *Forensic Intelligence*: Deep camera, lens, and file metadata analysis.
   - *Biometric Armor*: Hardware-backed app lock protecting stored credentials.
3. **Server Connection**:
   - Tap **Server Settings** or select your connection profile (e.g. *Emulator 10.0.2.2* or *ADB Reverse 127.0.0.1*).
   - Tap **Test Connection** to measure live latency.
   - Tap **Save & Continue** to persist the verified endpoint.
4. **Account Registration / Login**:
   - Register a new user account (Username, Email, Password).
   - The backend creates the per-user cryptographic key and issues a JWT token.
5. **Vault Home Screen**:
   - View storage quotas, upload files up to 2GB, capture camera photos directly to the encrypted vault, inspect forensic metadata, search, and download stored files.

---

## 9. Graceful Shutdown & Teardown

To shut down the CipherVault environment cleanly:

### 9.1 Stop Backend Server
In the PowerShell terminal running the Spring Boot backend:
- Press `Ctrl + C`
- When prompted `Terminate batch job (Y/N)?`, type `Y` and press `Enter`.
- Tomcat will gracefully shut down the Hikari connection pool and flush pending file writes.

### 9.2 Stop Python Server Manager
If running `server-control`, simply close the window or click **Quit** in the menu.

### 9.3 Stop MySQL Service (Optional)
If you wish to stop the MySQL Windows service:
```powershell
net stop MySQL80
```

### 9.4 Disconnect Android Device / ADB Reverse
If you established an ADB reverse tunnel, clear it with:
```powershell
adb reverse --remove tcp:8080
```
