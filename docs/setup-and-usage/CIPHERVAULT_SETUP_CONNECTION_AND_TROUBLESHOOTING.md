# CIPHERVAULT — SETUP, CONNECTION MODES & TROUBLESHOOTING GUIDE

**Document ID:** `CIPHERVAULT_SETUP_CONNECTION_AND_TROUBLESHOOTING.md`  
**Location:** `docs/reference/CIPHERVAULT_SETUP_CONNECTION_AND_TROUBLESHOOTING.md`  
**Target Environments:** Windows 11 Developer Host, Android Studio Emulator, Physical Pixel Device  

---

## 1. System Requirements & Prerequisites

| Component | Minimum Version | Verified Active Version |
|---|---|---|
| **Operating System** | Windows 10/11 64-bit | Windows 11 (OS Build 26100) |
| **Java Development Kit** | JDK 21 LTS | Java SE Runtime 21.0.12+7-LTS |
| **Database** | MySQL 8.0 Community Server | MySQL 8.0.46 on `localhost:3306` |
| **Android SDK** | API Level 34 / 35, Build-Tools 34.0.0 | Android SDK at `AppData\Local\Android\Sdk` |
| **Python** | Python 3.10+ (for Desktop Manager) | Python 3.12 (PySide6, psutil, requests, qrcode) |
| **Build Tools** | Maven 3.9+ (bundled wrapper `mvnw.cmd`), Gradle 8.7 (bundled wrapper `gradlew.bat`) |

---

## 2. Step-by-Step Backend & Database Startup

### Step 1: Start MySQL Database Service
Open PowerShell as Administrator:
```powershell
net start MySQL80
```
Verify the database schema:
```powershell
& "C:\Program Files\MySQL\MySQL Server 8.0\bin\mysql.exe" -u ciphervault_user -p"CipherVault@2026" -e "USE ciphervault; SHOW TABLES;"
```
*(If first time setup, initialize using `database/schema.sql`).*

### Step 2: Start Spring Boot Core Server
Navigate to the `backend/` directory:
```powershell
cd backend
.\mvnw.cmd spring-boot:run
```
The server will start on port `8080`. Verify by opening in browser or querying health:
```powershell
Invoke-RestMethod -Uri "http://localhost:8080/api/health"
```
Expected output:
```json
{
  "status": "UP",
  "appName": "CipherVault",
  "version": "1.0.0",
  "database": "CONNECTED",
  "storageRoot": "C:\\Users\\akhil\\OneDrive\\Desktop\\CipherVault\\backend\\storage"
}
```

### Step 3: (Optional) Launch Desktop Server Manager
To monitor status, view live logs, and generate connection QR codes:
```powershell
# From CipherVault project root:
.\run.bat
```

---

## 3. Android Client Connection Modes

CipherVault supports five connection strategies configured via `ConnectionActivity`:

### Mode A: Android Emulator Host (Recommended for Emulator Testing)
- **Target URL:** `http://10.0.2.2:8080`
- **Rationale:** The Android Studio emulator assigns `10.0.2.2` as an alias to the development host's loopback interface (`127.0.0.1`).
- **Steps:**
  1. Launch emulator: `& "$env:LOCALAPPDATA\Android\Sdk\emulator\emulator.exe" -avd Pixel_10_Pro_XL`.
  2. In CipherVault, tap "Server Settings" -> select "Android Emulator (10.0.2.2:8080)".
  3. Tap "Test Connection" -> Should display "Server reachable • Status: UP".

### Mode B: USB / ADB Reverse (Recommended for Physical Devices over Cable)
- **Target URL:** `http://127.0.0.1:8080` (Localhost)
- **Rationale:** Maps port 8080 on the physical phone directly to port 8080 on the PC.
- **Steps:**
  1. Connect phone via USB with USB Debugging enabled.
  2. Run in terminal:
     ```powershell
     & "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe" reverse tcp:8080 tcp:8080
     ```
  3. In CipherVault, tap "Server Settings" -> select "USB / ADB Reverse (127.0.0.1:8080)".
  4. Tap "Test Connection" -> Connection succeeds.

### Mode C: Wi-Fi / Local Area Network (Wireless Operation)
- **Target URL:** `http://<HOST_LAN_IP>:8080` (e.g., `http://192.168.1.15:8080`).
- **Steps:**
  1. Find PC IP address: `Get-NetIPAddress -AddressFamily IPv4 | Where-Object { $_.InterfaceAlias -like "*Wi-Fi*" }`.
  2. Ensure PC and Phone are on the same Wi-Fi network.
  3. In CipherVault, select "Local Wi-Fi Network" -> enter `<HOST_LAN_IP>` and `8080`.
  4. Ensure Windows Firewall permits incoming TCP traffic on port 8080.

### Mode D: Desktop QR Code Pairing
- **Steps:**
  1. In Desktop Server Manager (`run.bat`), navigate to the "Connect" tab.
  2. On the Android phone, tap "Scan Desktop QR Code".
  3. Point camera at the Desktop Manager QR code -> base URL is automatically applied and tested.

---

## 4. Troubleshooting Guide

### Issue 1: Cleartext HTTP Error
- **Symptom:** `java.io.IOException: Cleartext HTTP traffic to <IP> not permitted by network security policy`.
- **Cause:** Android 9+ (API 28+) blocks unencrypted HTTP by default.
- **Resolution:** CipherVault includes [`network_security_config.xml`](file:///c:/Users/akhil/OneDrive/Desktop/CipherVault/android/app/src/main/res/xml/network_security_config.xml) with:
  ```xml
  <base-config cleartextTrafficPermitted="true">
      <trust-anchors>
          <certificates src="system" />
          <certificates src="user" />
      </trust-anchors>
  </base-config>
  ```
  and declared in `AndroidManifest.xml` under `android:networkSecurityConfig="@xml/network_security_config"`.

### Issue 2: MySQL Connection Refused
- **Symptom:** Backend throws `CommunicationsLinkException: Communications link failure` on startup.
- **Resolution:**
  1. Check MySQL service status: `Get-Service MySQL80`.
  2. If stopped, run `net start MySQL80`.
  3. Verify port 3306 listening: `Get-NetTCPConnection -LocalPort 3306`.

### Issue 3: Port 8080 Already in Use
- **Symptom:** `Web server failed to start. Port 8080 was already in use.`
- **Resolution:**
  1. Identify process occupying port 8080: `Get-NetTCPConnection -LocalPort 8080`.
  2. Terminate the stale Java or process using PID: `Stop-Process -Id <PID> -Force`.

### Issue 4: ADB Reverse Disconnection after Unplugging
- **Symptom:** App loses connection over USB after cable is re-plugged.
- **Resolution:** Rerun `adb reverse tcp:8080 tcp:8080`.

---

## 5. Verification Commands Matrix

```powershell
# Check Android unit tests (73 tests)
cd android
.\gradlew.bat testDebugUnitTest

# Assemble Android debug APK
.\gradlew.bat assembleDebug

# Check Backend tests (138 tests)
cd ..\backend
.\mvnw.cmd test

# Run health check
Invoke-RestMethod http://localhost:8080/api/health
```

