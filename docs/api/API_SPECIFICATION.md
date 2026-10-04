# CipherVault REST API Specification

**Base URL**: `http://<server-host>:8080`  
**Authentication Scheme**: HTTP Authorization header: `Bearer <jwt-token>`  
**Content-Type**: `application/json` (except file uploads: `multipart/form-data`)

---

## 1. Authentication Endpoints

### 1.1 Register User
- **Method**: `POST`
- **Path**: `/api/auth/register`
- **Request Body**:
```json
{
  "username": "alice",
  "email": "alice@example.com",
  "password": "Password123!"
}
```
- **Response**: `200 OK`
```json
{
  "message": "User registered successfully",
  "username": "alice",
  "email": "alice@example.com"
}
```
- **Error Responses**:
  - `400 BAD REQUEST`: Invalid input, weak password (<6 characters), or username/email already registered.

### 1.2 Sign In
- **Method**: `POST`
- **Path**: `/api/auth/login`
- **Request Body**:
```json
{
  "email": "alice@example.com",
  "password": "Password123!"
}
```
- **Response**: `200 OK`
```json
{
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "username": "alice"
}
```
- **Error Responses**:
  - `401 UNAUTHORIZED`: Invalid credentials.
  - `429 TOO MANY REQUESTS`: Rate limited after 5 consecutive failed attempts (15-minute lockout).

### 1.3 Change Password
- **Method**: `POST`
- **Path**: `/api/auth/change-password`
- **Headers**: `Authorization: Bearer <token>`
- **Request Body**:
```json
{
  "currentPassword": "Password123!",
  "newPassword": "NewSecurePassword456!",
  "confirmPassword": "NewSecurePassword456!"
}
```
- **Response**: `200 OK`
```json
{
  "message": "Password changed successfully"
}
```
- **Error Responses**:
  - `400 BAD REQUEST`: Current password is missing/incorrect, new password is too short (<6), confirmation mismatch, or new password matches current password.
  - `401 UNAUTHORIZED`: Missing or invalid JWT token.

---

## 2. User & Profile Endpoints

### 2.1 Get Authoritative Profile Metrics
- **Method**: `GET`
- **Path**: `/api/user/profile`
- **Headers**: `Authorization: Bearer <token>`
- **Response**: `200 OK`
```json
{
  "username": "alice",
  "email": "alice@example.com",
  "storageLimit": 1073741824,
  "usedStorage": 14934820,
  "fileCount": 4,
  "encryptedCount": 3,
  "categories": {
    "IMAGES": 2,
    "PDFS": 1,
    "VIDEOS": 0,
    "DOCUMENTS": 0,
    "OTHER": 1
  }
}
```

---

## 3. File Storage Endpoints

### 3.1 Upload File
- **Method**: `POST`
- **Path**: `/api/files/upload`
- **Headers**: `Authorization: Bearer <token>`
- **Request Format**: `multipart/form-data`
  - `file`: Binary file stream
  - `encrypt`: Boolean (`true` for AES-256-GCM chunked streaming, `false` for unencrypted)
- **Response**: `200 OK`
```json
{
  "success": true,
  "message": "File uploaded successfully",
  "fileId": 12,
  "filename": "annual_report.pdf",
  "fileSize": 14934820,
  "encrypted": true,
  "sha256Hash": "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855"
}
```
- **Error Responses**:
  - `400 BAD REQUEST`: File is empty or missing.
  - `409 CONFLICT`: Duplicate file detected (matching SHA-256 hash for this user).
  - `413 PAYLOAD TOO LARGE`: File exceeds 200 MB maximum upload limit.
  - `507 INSUFFICIENT STORAGE`: User storage quota exceeded.

### 3.2 List Files (Supports Optional Pagination)
- **Method**: `GET`
- **Path**: `/api/files`
- **Headers**: `Authorization: Bearer <token>`
- **Query Parameters**:
  - `page` (optional): 0-indexed page number.
  - `size` (optional): Number of records per page (default: 50, max: 100).
- **Response Headers**:
  - `X-Total-Count`: Total number of files.
  - `X-Total-Pages`: Total number of pages.
  - `X-Current-Page`: Current page index.
  - `X-Page-Size`: Number of items returned.
- **Response Body**: `200 OK`
```json
[
  {
    "id": 12,
    "filename": "annual_report.pdf",
    "fileSize": 14934820,
    "contentType": "application/pdf",
    "encrypted": true,
    "sha256Hash": "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855",
    "hasPreview": true,
    "category": "Documents",
    "createdAt": "2026-10-02T10:15:30",
    "metadata": {
      "title": "Annual Financial Report",
      "author": "Finance Dept",
      "docCreatedDate": "2026-01-15"
    }
  }
]
```

### 3.3 Check Duplicate File
- **Method**: `GET`
- **Path**: `/api/files/check-duplicate?hash=<sha256-hex>`
- **Headers**: `Authorization: Bearer <token>`
- **Response**: `200 OK`
```json
{
  "isDuplicate": true,
  "existingFileName": "annual_report.pdf",
  "fileId": 12
}
```

### 3.4 Search Files (Multi-Attribute & Filter Support)
- **Method**: `GET`
- **Path**: `/api/files/search`
- **Headers**: `Authorization: Bearer <token>`
- **Query Parameters**:
  - `query` (optional): Case-insensitive match on filename, title, author, artist, or tags.
  - `category` (optional): `All`, `Images`, `Videos`, `Documents`, `Media`, `Archives`, `PDFs`, `Other`.
  - `cameraMake` (optional): e.g. `Sony`, `Apple`, `Samsung`.
  - `cameraModel` (optional): e.g. `Alpha 7`, `iPhone 15 Pro`.
  - `resolution` (optional): e.g. `3840x2160`.
  - `codec` (optional): e.g. `HEVC`, `H.264`, `AAC`.
  - `artist` (optional): e.g. Music performer.
  - `author` (optional): e.g. Document author.
  - `genre` (optional): e.g. `Cinematic`, `Rock`.
  - `page` (optional): 0-indexed page number.
  - `size` (optional): Records per page.
- **Response Headers**: `X-Total-Count`, `X-Total-Pages`, `X-Current-Page`, `X-Page-Size`
- **Response**: `200 OK` (Array of matching `FileResponse` objects)

### 3.5 Autocomplete Suggestions
- **Method**: `GET`
- **Path**: `/api/files/suggestions?prefix=<string>`
- **Headers**: `Authorization: Bearer <token>`
- **Response**: `200 OK`
```json
["Samsung", "Sony Alpha 7", "Sunset over Hills"]
```

### 3.6 Download File (Decrypted or Raw Ciphertext)
- **Method**: `GET`
- **Path**: `/api/files/{id}/download?decrypt=<boolean>`
- **Headers**: `Authorization: Bearer <token>`
- **Query Parameters**:
  - `decrypt=true`: Automatically decrypts AES-256-GCM ciphertext on the fly and streams plaintext.
  - `decrypt=false`: Streams raw AES-256-GCM ciphertext bytes (.encrypted) with cryptographic header intact.
- **Response**: `200 OK` (Streamed binary response body)
- **Response Headers**:
  - `Content-Disposition`: `attachment; filename="..."`
  - `Content-Length`: Accurate payload size (plaintext size when decrypt=true, disk size when decrypt=false).
  - `X-File-SHA256`: Expected SHA-256 hash.
  - `X-Encrypted`: `"true"` or `"false"`.

### 3.7 Download Thumbnail Preview
- **Method**: `GET`
- **Path**: `/api/files/{id}/preview`
- **Headers**: `Authorization: Bearer <token>`
- **Response**: `200 OK` (`image/jpeg` binary stream with Cache-Control headers)

### 3.8 Regenerate Thumbnail Preview
- **Method**: `POST`
- **Path**: `/api/files/{id}/regenerate-preview`
- **Headers**: `Authorization: Bearer <token>`
- **Response**: `200 OK`
```json
{
  "success": true,
  "message": "Preview regenerated successfully",
  "hasPreview": true
}
```

### 3.9 Delete File
- **Method**: `DELETE`
- **Path**: `/api/files/{id}`
- **Headers**: `Authorization: Bearer <token>`
- **Response**: `200 OK` (Deletes physical file, thumbnail preview, metadata, database record, and frees user quota atomically)

---

## 4. Health Check

### 4.1 Server Health
- **Method**: `GET`
- **Path**: `/api/health`
- **Public**: No authentication required
- **Response**: `200 OK`
```json
{
  "status": "UP",
  "timestamp": 1759500000000,
  "service": "CipherVault Backend"
}
```
