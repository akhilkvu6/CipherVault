# CipherVault System Architecture

## 1. High-Level Architecture Overview

CipherVault is an enterprise-grade private cloud storage system built with end-to-end cryptographic integrity, zero-memory-exhaustion streaming I/O, hardware-backed credential storage, and rich multimedia metadata extraction.

```
+-------------------------------------------------------------+
|                     Android Client (M3)                     |
|                                                             |
|  [MainActivity] (Fragment show/hide caching)                |
|         |---> [HomeFragment]                                |
|         |---> [FilesFragment] (Client-side & Server Search)  |
|         |---> [UploadFragment] (Chunked Streaming)          |
|         |---> [SettingsFragment]                            |
|                                                             |
|  [FileUtils]     --> Managed ThreadPool for Background I/O  |
|  [SessionManager]--> EncryptedSharedPreferences (MasterKey) |
+------------------------------+------------------------------+
                               | HTTPS / HTTP REST
                               v
+-------------------------------------------------------------+
|                 Spring Boot 4.1.1 Backend                   |
|                                                             |
|  [JwtAuthenticationFilter]  --> Bearer Token Validation     |
|  [FileController]          --> HTTP routing & pagination    |
|  [FileStorageService]      --> Storage lifecycle management |
|  [FileSearchService]       --> JPA Criteria multi-attribute |
|  [EncryptionService]       --> AES-256-GCM chunked stream   |
|  [KeyManagementService]    --> PBKDF2 KEK + Per-User UDEK   |
|  [MediaPreviewService]     --> PDFBox & ImageIO Thumbnails  |
|  [MetadataExtractionService]-> ExifTool Metadata Pipeline   |
+------------------------------+------------------------------+
                               |
               +---------------+---------------+
               |                               |
               v                               v
+-----------------------------+ +-----------------------------+
|        MySQL 8.x DB         | |    Server Local Storage     |
| - users (quota, UDEK key)   | | - storage/uploads/          |
| - stored_files (sha256 hash | | - storage/encrypted/        |
|   uq_user_sha256 constraint)| | - storage/previews/         |
| - file_metadata (exif/tags) | |                             |
+-----------------------------+ +-----------------------------+
```

---

## 2. Cryptographic Envelope Hierarchy

CipherVault implements dual-layer envelope encryption separating the Server Key Encryption Key (KEK) from User Data Encryption Keys (UDEKs):

```
                     Master Server Secret
                              +
                     Static Server Salt
                              |
                              v  PBKDF2-HMAC-SHA256 (65,536 iterations)
                     +-----------------+
                     |   Server KEK    |  (AES-256 Key Encryption Key)
                     +--------+--------+
                              |
            +-----------------+-----------------+
            | Encrypts                          | Encrypts
            v                                   v
+-----------------------+           +-----------------------+
|  User A Encrypted DEK |           |  User B Encrypted DEK |
|   (Stored in DB)      |           |   (Stored in DB)      |
+-----------+-----------+           +-----------+-----------+
            | Decrypts at runtime               | Decrypts at runtime
            v                                   v
+-----------------------+           +-----------------------+
|     User A UDEK       |           |     User B UDEK       |
|   (AES-256 Secret)    |           |   (AES-256 Secret)    |
+-----------+-----------+           +-----------+-----------+
            |                                   |
            v Encrypts/Decrypts files           v Encrypts/Decrypts files
     User A Stored Files                 User B Stored Files
   (AES-256-GCM / 128-bit)             (AES-256-GCM / 128-bit)
```

1. **Server KEK**: Derived via PBKDF2-HMAC-SHA256 with 65,536 iterations and a 256-bit output from the master application secret. In production environments, dev defaults are strictly forbidden and throw fatal configuration errors on startup.
2. **User Data Encryption Key (UDEK)**: A cryptographically secure 256-bit random key (`SecureRandom`) generated per user upon first encrypted file upload.
3. **Key Storage**: The UDEK is encrypted under the Server KEK using AES-256-GCM and stored in `users.user_key`. At no point is an unencrypted UDEK persisted on disk or in database records.
4. **File Encryption**: Each encrypted file is processed with AES-256-GCM using a unique 12-byte initialization vector (IV) prepended to the ciphertext, authenticated with a 128-bit GCM authentication tag.

---

## 3. Streaming I/O Pipeline (Zero-Heap Exhaustion)

Unlike standard file-upload implementations that buffer incoming files into memory (`byte[]` or `ByteArrayOutputStream`), CipherVault enforces strict constant-memory streaming:

### Upload Path:
```
Android File URI
  -> InputStream (ContentResolver)
  -> StreamingProgressRequestBody (16 KB buffer)
  -> HTTP Multipart Request
  -> Backend MultipartFile InputStream
  -> Temporary on-disk spool
  -> CipherOutputStream (AES-256-GCM, 16 KB chunked)
  -> Encrypted File on Disk (storage/encrypted/)
```
- **Peak Memory Usage**: $< 32\text{ KB}$ JVM heap overhead, regardless of file size.
- **Integrity**: Streaming `MessageDigest` calculates SHA-256 checksum concurrently during upload.
- **Deduplication**: Guaranteed both by pre-encryption hash check and database-level `uq_stored_files_user_sha256` composite unique constraint.

### Download Path:
```
Encrypted File on Disk
  -> FileInputStream
  -> CipherInputStream (AES-256-GCM decryption)
  -> Spring StreamingResponseBody (16 KB buffer)
  -> HTTP Response Stream
  -> Android ResponseBody.byteStream()
  -> Downloaded Plaintext in Download/CipherVault/ via FileUtils ThreadPool
```

---

## 4. Metadata & Native Preview Engine

- **Metadata Extraction**: Extracted before encryption via `MetadataExtractionService` using ExifTool. Extracts camera make/model, lens, focal length, ISO, exposure, resolution, video/audio codecs, framerate, artist, album, title, author, and creation dates into `file_metadata`.
- **Images (PNG, JPG, WEBP)**: Resized into optimized JPEG thumbnails using high-quality downsampling.
- **PDF Documents**: Rendered natively via Apache PDFBox 3.0.3 at 72 DPI to capture page 1 as a thumbnail.
- **Videos**: Extracted via JCodec / FFmpeg pipeline.
- **Isolation**: Thumbnails are stored in `storage/previews/` with random UUID naming and served via authenticated endpoint `/api/files/{id}/preview`.
