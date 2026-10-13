# CipherVault — Metadata Database Audit Report (Read-Only)

**Date of Audit:** October 11, 2026  
**Audit Type:** Read-Only Source Code & Database Forensic Analysis  
**Target Application:** CipherVault Secure Cloud Storage  
**Database Audited:** MySQL 8.0.46 Community Server (`jdbc:mysql://localhost:3306/ciphervault`)  
**Backend:** Spring Boot 4.1.1, Java 21, Hibernate DDL Auto `update`  
**Client:** Native Android (Java/XML, Material 3, Retrofit 2)  
**Execution Mode:** STRICTLY READ-ONLY (No source modifications, no database modifications, no runtime device sessions)  

---

## 1. Executive Summary

This investigation audits the existing CipherVault application, Spring Boot backend, and MySQL database to evaluate the viability of metadata-based search, filtering, and dynamic suggestion features. The audit was conducted against an active collection of **85 uploaded files** encompassing sample photos, RAW camera files, 4K/Full HD videos, documents, audio tracks, and application packages.

### Key Audit Findings:
1. **Database Inventory:** Exactly **85 files** are stored in `stored_files` and associated 1-to-1 with records in `file_metadata`. There are **0 orphaned files**, **0 orphaned metadata records**, and **0 duplicate joins**.
2. **Device Sample Verification:**
   * **Present and Verified:** 
     * **Canon EOS 5D Mark III:** 9 files (4 JPEGs, 3 CR2 RAW, 2 Full HD MP4 videos).
     * **Nikon COOLPIX B700:** 5 files (3 JPEGs, 2 Full HD MP4 videos).
     * **Samsung Galaxy S24 Ultra:** 7 files (4 stored as `Galaxy S24 Ultra`, 3 stored as `SM-S928B`).
     * **Canon EOS R6:** 4 files (3 JPEGs, 1 4K MP4 video).
     * **Sony ILCE-7M3 (Alpha 7 III):** 3 JPEG photos populated directly; 1 Full HD MP4 video present on disk and in database with model preserved in raw JSON.
     * **Sony DSC-W320:** 3 JPEG photos.
     * **Sony ILCE-7M4 (Alpha 7 IV):** 2 JPEG photos; 2 Full HD MP4 videos present with model preserved in raw JSON.
     * **Lenovo K6 POWER:** 3 JPEG photos.
     * **Nikon Z 6:** 1 JPEG photo (camera make stored as `NIKON CORPORATION`).
     * **Canon EOS 5D Mark IV:** 1 JPEG photo.
     * **Google Android Emulator:** 1 JPEG photo (`sdk_gphone16k_x86_64`).
   * **Absent from Database:**
     * **Redmi Note 7 Pro:** **0 files found.** (The uploaded Xiaomi files belong to `M2101K7AI` / `Redmi Note 10`).
3. **Pre-Encryption Extraction Architecture:**
   Metadata extraction executes **strictly before streaming AES-256-GCM encryption** in [`FileStorageService.java`](file:///c:/Users/akhil/OneDrive/Desktop/CipherVault/backend/src/main/java/com/ciphervault/ciphervault/file/FileStorageService.java). Plaintext metadata is stored in unencrypted MySQL columns. Consequently, **all metadata searches execute without reading, decrypting, or loading ciphertext from disk**.
4. **Critical Data Quality Deficiencies Discovered:**
   * **Sony Video Tag Omission:** In Sony XAVC-S MP4 videos (`C0024.MP4`, `C8237.MP4`, `C8238.MP4`), camera details are extracted by ExifTool under `"Manufacturer"` and `"ModelName"` rather than `"Make"` and `"Model"`. [`MetadataExtractionService.java`](file:///c:/Users/akhil/OneDrive/Desktop/CipherVault/backend/src/main/java/com/ciphervault/ciphervault/file/MetadataExtractionService.java) failed to capture them, saving `camera_make` and `camera_model` as `NULL`.
   * **Corrupted / Zero-Date Strings:** 11 files contain invalid dates such as `0000:00:00 00:00:00` or `0000:01:01 00:00:00` because raw MP4 headers default to zero and were persisted verbatim.
   * **Undefined Camera Exposure Strings:** Several video files and manual lenses have `exposure_time` and `f_number` populated with `"undef"`, and `iso` populated with `"0"`.
   * **Heterogeneous Duration and Bitrate Units:** Durations mix `"10.54 s"` with `"0:03:49"`. Bitrates contain text suffixes like `"3.23 Mbps"`. SQL numeric comparisons (`BETWEEN`, `>`, `<`) cannot function without schema normalization.
   * **Unpersisted GPS Coordinates:** 6 files have valid GPS coordinates in `raw_metadata_json`, but there is **no database column** for latitude/longitude in `file_metadata`.
5. **Search API Assessment:**
   `GET /api/files/search` supports server-side JPA specifications with category, size range, date text matching, camera make/model, resolution, codec, artist, and author filtering. However, **no backend API exists to query dynamic distinct counts or facet options per attribute**. The proposed UI rule to auto-collapse filters based on available counts requires a minor backend endpoint extension.

---

## 2. Environment and Database Inspected

| Component | Specification / Version | Configuration & Location |
| :--- | :--- | :--- |
| **Operating System** | Windows 11 (64-bit) | Local Developer Environment |
| **Relational Database** | MySQL Community Server 8.0.46 | Port 3306, Schema `ciphervault`, Character Set `utf8mb4` |
| **Database Connection** | HikariCP Connection Pool | `jdbc:mysql://localhost:3306/ciphervault`, SSL Disabled |
| **Backend Framework** | Spring Boot 4.1.1 / Java 21 | Spring Data JPA, Hibernate ORM 6.x (`ddl-auto=update`) |
| **Metadata Extraction** | Phil Harvey's ExifTool 13.32 | Spawned via `ProcessBuilder("exiftool", "-json", "-struct")` |
| **Native Fallbacks** | Java ImageIO & Apache PDFBox 3.0.3 | Executed when ExifTool fails or omits attributes |
| **Mobile Client** | Native Android (Java 17 / XML) | AndroidX, Material 3 (1.12.0), Retrofit 2, Gson |

---

## 3. Files and Device Models Actually Represented in the Database

Every file in the database was queried and analyzed against the user's checklist of camera and smartphone models.

### 3.1 Device Presence Verification Table

| Requested Device Model | Present in Database? | Exact Stored Make | Exact Stored Model | Total Files | File Types | Exact Database File IDs |
| :--- | :---: | :--- | :--- | :---: | :--- | :--- |
| **Sony ILCE-7M3 (Alpha 7 III)** | **YES** | `SONY` | `ILCE-7M3` | **4** | 3 JPEGs, 1 MP4 | Photos: 210, 211, 212<br>Video: 223 (`C0024.MP4`)* |
| **Canon EOS 5D Mark III** | **YES** | `Canon` | `Canon EOS 5D Mark III` | **9** | 4 JPEGs, 3 CR2 RAW, 2 MP4 | Photos: 217, 218, 219, 220<br>RAW: 209, 213, 214<br>Videos: 215, 216 |
| **Canon EOS 5D Mark IV** | **YES** | `Canon` | `Canon EOS 5D Mark IV` | **1** | 1 JPEG | Photo: 208 (`IMG_20220929_220551 (1).jpg`) |
| **Canon EOS R6** | **YES** | `Canon` | `Canon EOS R6` | **4** | 3 JPEGs, 1 MP4 (4K) | Photos: 237, 238, 240<br>Video: 239 (`0R4A4946.MP4`) |
| **Nikon Z 6** | **YES** | `NIKON CORPORATION` | `NIKON Z 6` | **1** | 1 JPEG | Photo: 205 (`NIKON Z6_20210926_061003.JPG`) |
| **Nikon COOLPIX B700** | **YES** | `NIKON` | `COOLPIX B700` | **5** | 3 JPEGs, 2 MP4 | Photos: 224, 230, 236<br>Videos: 231, 232 |
| **Sony DSC-W320** | **YES** | `SONY` | `DSC-W320` | **3** | 3 JPEGs | Photos: 233, 234, 235 |
| **Samsung Galaxy S24 Ultra** | **YES** | `samsung` | `Galaxy S24 Ultra` & `SM-S928B` | **7** | 7 JPEGs | Named: 201, 202, 203, 204<br>Hardware: 173, 174, 190 |
| **Redmi Note 7 Pro** | **NO** | `N/A` | `N/A` | **0** | None | **Not present in database.** |

*\*Note on File ID 223 (`C0024.MP4`): The physical file is an ILCE-7M3 video. Its camera model is stored in `raw_metadata_json` under `"Manufacturer":"Sony","ModelName":"ILCE-7M3"`, but the database column was populated as `NULL` due to backend tag mapping.*

### 3.2 Additional Devices Discovered in Database
* **Sony ILCE-7M4 (Sony Alpha 7 IV):** 4 files (Photos: IDs 206, 207; Videos: IDs 221, 222 in raw JSON).
* **Xiaomi Redmi Note 10 (`M2101K7AI`):** 2 JPEG photos (IDs 225, 226).
* **Lenovo K6 POWER:** 3 JPEG photos (IDs 227, 228, 229).
* **Google Android Emulator (`sdk_gphone16k_x86_64`):** 1 JPEG photo (ID 132).

---

## 4. Database Schema and Metadata Architecture

The database structure consists of two core tables linked in a strict 1-to-1 relationship:

```
┌──────────────────────────────────────────────┐
│                stored_files                  │
├──────────────────────────────────────────────┤
│ id (PK, BIGINT, auto_increment)              │
│ original_filename (VARCHAR 255, NOT NULL)    │
│ stored_filename (VARCHAR 255, NOT NULL)      │
│ file_size (BIGINT, NOT NULL)                 │
│ content_type (VARCHAR 255, NOT NULL)         │
│ sha256_hash (VARCHAR 64, NOT NULL)           │
│ storage_path (VARCHAR 255, NOT NULL)         │
│ encrypted (BIT 1, NOT NULL)                  │
│ has_preview (BIT 1, NOT NULL)                │
│ preview_path (VARCHAR 255, NULL)             │
│ preview_mime_type (VARCHAR 255, NULL)        │
│ user_id (FK -> users.id, BIGINT, NOT NULL)   │
│ created_at (DATETIME 6, NOT NULL)            │
└──────────────────────┬───────────────────────┘
                       │ 1:1 Foreign Key (file_id, UNIQUE)
                       ▼
┌──────────────────────────────────────────────┐
│                file_metadata                 │
├──────────────────────────────────────────────┤
│ id (PK, BIGINT, auto_increment)              │
│ file_id (FK -> stored_files.id, UNIQUE)      │
│ camera_make (VARCHAR 255, INDEXED)           │
│ camera_model (VARCHAR 255, INDEXED)          │
│ lens (VARCHAR 255)                           │
│ focal_length (VARCHAR 255)                   │
│ iso (VARCHAR 255)                            │
│ exposure_time (VARCHAR 255)                  │
│ f_number (VARCHAR 255)                       │
│ date_taken (VARCHAR 255)                     │
│ width (INT)                                  │
│ height (INT)                                 │
│ resolution (VARCHAR 255, INDEXED)            │
│ duration (VARCHAR 255)                       │
│ video_codec (VARCHAR 255, INDEXED)           │
│ audio_codec (VARCHAR 255)                    │
│ frame_rate (VARCHAR 255)                     │
│ bitrate (VARCHAR 255)                        │
│ title (VARCHAR 500, INDEXED)                 │
│ artist (VARCHAR 255, INDEXED)                │
│ album (VARCHAR 255, INDEXED)                 │
│ genre (VARCHAR 255, INDEXED)                 │
│ release_year (VARCHAR 255)                   │
│ author (VARCHAR 500, INDEXED)                │
│ creator (TEXT)                               │
│ subject (TEXT)                               │
│ keywords (TEXT)                              │
│ doc_created_date (VARCHAR 255)               │
│ doc_modified_date (VARCHAR 255)              │
│ raw_metadata_json (LONGTEXT)                 │
└──────────────────────────────────────────────┘
```

### Architectural Safeguards Verified:
* **Integrity:** `file_id` is unique in `file_metadata`. Querying `SELECT file_id, COUNT(*) FROM file_metadata GROUP BY file_id HAVING COUNT(*) > 1` confirmed **0 duplicate records**.
* **Completeness:** Querying `stored_files` without matching `file_metadata` confirmed **0 missing rows** (100% 1-to-1 sync).
* **Indexing:** B-Tree indexes exist on `camera_make`, `camera_model`, `resolution`, `video_codec`, `artist`, `album`, `genre`, `author`, and `title`.

---

## 5. Complete Metadata Field Inventory

| Field Category | Attribute | Stored Column | Extracted Source Tag | Exposed in API DTO? | Queryable Today? |
| :--- | :--- | :--- | :--- | :---: | :---: |
| **File System** | Filename | `stored_files.original_filename` | Upload Header | Yes | Yes (Free-text) |
| | File Size | `stored_files.file_size` | Plaintext File Length | Yes | Yes (`minSize`, `maxSize`) |
| | MIME Type | `stored_files.content_type` | Upload Header / Probe | Yes | Yes (Category filter) |
| | Upload Date | `stored_files.created_at` | System `@PrePersist` | Yes | Yes (Sorting only) |
| | Integrity | `stored_files.sha256_hash` | Plaintext Digest | Yes | No (Duplicate check only) |
| | Encryption | `stored_files.encrypted` | Storage Engine Flag | Yes | No (Status display only) |
| **Camera** | Make | `file_metadata.camera_make` | ExifTool `Make` | Yes | Yes (`cameraMake`) |
| | Model | `file_metadata.camera_model` | ExifTool `Model` | Yes | Yes (`cameraModel`) |
| | Lens | `file_metadata.lens` | ExifTool `LensModel`, `Lens` | Yes | Yes (Free-text only) |
| | Focal Length | `file_metadata.focal_length` | ExifTool `FocalLength` | Yes | No |
| | ISO | `file_metadata.iso` | ExifTool `ISO` | Yes | No |
| | Exposure Time | `file_metadata.exposure_time` | ExifTool `ExposureTime` | Yes | No |
| | Aperture | `file_metadata.f_number` | ExifTool `FNumber`, `Aperture`| Yes | No |
| | Date Taken | `file_metadata.date_taken` | ExifTool `DateTimeOriginal` | Yes | Yes (`dateTaken` text LIKE)|
| **Image/Video**| Width | `file_metadata.width` | ExifTool / ImageIO | Yes | No (Via Resolution) |
| | Height | `file_metadata.height` | ExifTool / ImageIO | Yes | No (Via Resolution) |
| | Resolution | `file_metadata.resolution` | Calculated `{w}x{h}` | Yes | Yes (`resolution`) |
| | Duration | `file_metadata.duration` | ExifTool `Duration` | Yes | No |
| | Video Codec | `file_metadata.video_codec` | ExifTool `VideoCodec`, `CodecID`| Yes| Yes (`codec`) |
| | Audio Codec | `file_metadata.audio_codec` | ExifTool `AudioCodec`, `AudioFormat`| Yes| Yes (`codec`) |
| | Frame Rate | `file_metadata.frame_rate` | ExifTool `VideoFrameRate` | Yes | No |
| | Bitrate | `file_metadata.bitrate` | ExifTool `AvgBitrate`, `Bitrate`| Yes | No |
| **Audio** | Title | `file_metadata.title` | ExifTool `Title`, `SongTitle` | Yes | Yes (Free-text only) |
| | Artist | `file_metadata.artist` | ExifTool `Artist`, `Composer`| Yes | Yes (`artist`) |
| | Album | `file_metadata.album` | ExifTool `Album` | Yes | Yes (Free-text only) |
| | Genre | `file_metadata.genre` | ExifTool `Genre` | Yes | Yes (`genre`) |
| | Release Year | `file_metadata.release_year` | ExifTool `Year`, `Date` | Yes | No |
| **Document** | Author | `file_metadata.author` | ExifTool / PDFBox `Author` | Yes | Yes (`author`) |
| | Creator | `file_metadata.creator` | ExifTool / PDFBox `Creator`| Yes | Yes (Free-text only) |
| | Subject | `file_metadata.subject` | ExifTool / PDFBox `Subject`| Yes | Yes (Free-text only) |
| | Keywords | `file_metadata.keywords` | ExifTool / PDFBox `Keywords`| Yes | Yes (Free-text only) |
| | Doc Created | `file_metadata.doc_created_date`| ExifTool / PDFBox Date | Yes | No |
| | Doc Modified| `file_metadata.doc_modified_date`| ExifTool `ModifyDate` | Yes | No |
| **Diagnostic** | Raw Dump | `file_metadata.raw_metadata_json`| Complete ExifTool Map JSON | **No** | No (Ignored via DTO) |

---

## 6. Metadata Coverage Counts and Percentages

Calculated across all **85 stored files** in the database:

| Field Name | Populated Files | Missing Files | Population Rate | Distinct Values | Evaluation & Usability |
| :--- | :---: | :---: | :---: | :---: | :--- |
| `raw_metadata_json` | 83 | 2 | **97.6%** | 83 | Internal diagnostic store; 2 binary blobs failed extraction. |
| `date_taken` | 66 | 19 | **77.6%** | 55 | High coverage, but contains 11 invalid zero-dates. |
| `resolution` | 66 | 19 | **77.6%** | 24 | **High utility;** cleanly captures photos, RAWs, and videos. |
| `width` / `height` | 66 | 19 | **77.6%** | 20 / 22 | Directly corresponds to resolution. |
| `doc_created_date` | 57 | 28 | **67.1%** | 46 | Captures PDF and camera file creation timestamps. |
| `doc_modified_date` | 56 | 29 | **65.9%** | 45 | ExifTool modify date. |
| `camera_make` | 41 | 44 | **48.2%** | 8 | **High utility;** covers 100% of real cameras and phones. |
| `camera_model` | 41 | 44 | **48.2%** | 13 | **High utility;** clean device names for chips and dropdowns. |
| `creator` | 39 | 46 | **45.9%** | 14 | Contains OS build tags, firmware revisions, and software. |
| `focal_length` | 39 | 46 | **45.9%** | 17 | Available on all camera photos. |
| `iso` | 39 | 46 | **45.9%** | 16 | Available on all camera photos. |
| `exposure_time` | 39 | 46 | **45.9%** | 22 | Available on all camera photos. |
| `f_number` | 39 | 46 | **45.9%** | 13 | Available on all camera photos. |
| `duration` | 21 | 64 | **24.7%** | 20 | Populated on 17 videos and 4 audio tracks. |
| `lens` | 20 | 65 | **23.5%** | 6 | **High utility for DSLR/Mirrorless;** 100% of standalone camera photos. |
| `video_codec` | 18 | 67 | **21.2%** | 3 | **High utility;** cleanly separates H.264, VP9, and Opus. |
| `audio_codec` | 18 | 67 | **21.2%** | 4 | Clean set: AAC (`mp4a`), PCM (`twos`, `sowt`), Opus. |
| `frame_rate` | 18 | 67 | **21.2%** | 7 | Discrete values: 24, 25, 29.97, 30, 50, 60, 100 fps. |
| `bitrate` | 17 | 68 | **20.0%** | 16 | Mixed unit strings (e.g. `3.23 Mbps`). |
| `title` | 16 | 69 | **18.8%** | 15 | Audio song titles and PDF title tags. |
| `artist` | 14 | 71 | **16.5%** | 13 | Populated on audio tracks and video creators. |
| `subject` | 9 | 76 | **10.6%** | 9 | Video social descriptions and PDF subjects. |
| `author` | 6 | 79 | **7.1%** | 3 | Document authors and audio composers. |
| `genre` | 4 | 81 | **4.7%** | 3 | Audio tracks only. |
| `release_year` | 4 | 81 | **4.7%** | 3 | Audio tracks only. |
| `album` | 3 | 82 | **3.5%** | 2 | Audio tracks only. |
| `keywords` | 0 | 85 | **0.0%** | 0 | **Unpopulated in all files.** |

---

## 7. Distinct Metadata Values and Their Frequencies

Exact frequency tables from read-only database queries:

### 7.1 Camera Make (8 Distinct Values, 41 Populated)
| Camera Make | File Count | Percentage |
| :--- | :---: | :---: |
| `Canon` | 14 | 34.1% |
| `SONY` | 8 | 19.5% |
| `samsung` | 7 | 17.1% |
| `NIKON` | 5 | 12.2% |
| `Lenovo` | 3 | 7.3% |
| `Xiaomi` | 2 | 4.9% |
| `NIKON CORPORATION`* | 1 | 2.4% |
| `Google` | 1 | 2.4% |

*\*Note: `NIKON CORPORATION` is an unnormalized variant of `NIKON` produced by Nikon Z 6 firmware.*

### 7.2 Camera Model (13 Distinct Values, 41 Populated)
| Camera Model | Normalized Device Family | File Count | Sample Files |
| :--- | :--- | :---: | :--- |
| `Canon EOS 5D Mark III` | Canon 5D Mk III | 9 | Photos (JPEGs), RAW (CR2), Videos (MP4) |
| `COOLPIX B700` | Nikon COOLPIX B700 | 5 | Photos (JPEGs), Videos (MP4) |
| `Galaxy S24 Ultra` | Samsung Galaxy S24 Ultra | 4 | Photos (JPEGs) |
| `Canon EOS R6` | Canon EOS R6 | 4 | Photos (JPEGs), Video (4K MP4) |
| `SM-S928B`* | Samsung Galaxy S24 Ultra | 3 | Photos (JPEGs) |
| `ILCE-7M3` | Sony Alpha 7 III | 3 | Photos (JPEGs) |
| `Lenovo K6 POWER` | Lenovo K6 Power | 3 | Photos (JPEGs) |
| `DSC-W320` | Sony DSC-W320 | 3 | Photos (JPEGs) |
| `ILCE-7M4` | Sony Alpha 7 IV | 2 | Photos (JPEGs) |
| `M2101K7AI`** | Xiaomi Redmi Note 10 | 2 | Photos (JPEGs) |
| `sdk_gphone16k_x86_64` | Android Emulator | 1 | Photo (JPEG) |
| `NIKON Z 6` | Nikon Z 6 | 1 | Photo (JPEG) |
| `Canon EOS 5D Mark IV` | Canon 5D Mk IV | 1 | Photo (JPEG) |

*\*Note: `SM-S928B` is the hardware code for `Galaxy S24 Ultra`.*  
*\*\*Note: `M2101K7AI` is the hardware code for `Redmi Note 10`.*

### 7.3 Lens Make & Model (6 Distinct Values, 20 Populated)
| Lens Model | Associated Camera | File Count |
| :--- | :--- | :---: |
| `EF24-70mm f/2.8L USM` | Canon EOS 5D Mark III | 9 |
| `RF24-105mm F4 L IS USM` | Canon EOS R6 | 4 |
| `FE 85mm F1.8` | Sony ILCE-7M3 | 3 |
| `FE 50mm F1.4 GM` | Sony ILCE-7M4 | 2 |
| `85mm f/1.8` | Nikon Z 6 | 1 |
| `EF24-105mm f/4L IS II USM`| Canon EOS 5D Mark IV | 1 |

### 7.4 Image & Video Resolutions (Top 12 of 24 Distinct Values, 66 Populated)
| Resolution | Aspect Ratio | Category | File Count | Sample Sources |
| :--- | :---: | :---: | :---: | :--- |
| `1080x2340` | 19.5:9 | Smartphone Photos | 7 | Samsung & Android Screenshots |
| `1920x1080` | 16:9 | Full HD Videos | 7 | Canon 5D3, Nikon B700, Sony A73/A74 |
| `5760x3840` | 3:2 | 22.1 MP Still Photos | 7 | Canon EOS 5D Mark III (JPEG & CR2) |
| `4000x3000` | 4:3 | 12.0 MP Still Photos | 5 | Samsung Galaxy S24 Ultra, Redmi Note 10 |
| `1080x1920` | 9:16 | Vertical Full HD Videos | 4 | Downloaded MP4 Reels |
| `1440x2560` | 9:16 | Vertical 2K Videos | 4 | Downloaded MP4 Videos |
| `4080x3060` | 4:3 | 12.5 MP Still Photos | 3 | Samsung S24 Ultra (SM-S928B) |
| `4160x2340` | 16:9 | 9.7 MP Still Photos | 3 | Lenovo K6 POWER |
| `4320x3240` | 4:3 | 14.0 MP Still Photos | 3 | Sony DSC-W320 |
| `5184x3888` | 4:3 | 20.1 MP Still Photos | 3 | Nikon COOLPIX B700 |
| `5472x3648` | 3:2 | 20.0 MP Still Photos | 3 | Canon EOS R6 |
| `6000x4000` | 3:2 | 24.0 MP Still Photos | 3 | Sony ILCE-7M3 (Alpha 7 III) |
| `3840x2160` | 16:9 | 4K Ultra HD Videos | 2 | Canon EOS R6 (`0R4A4946.MP4`), WebM |
| `7008x4672` | 3:2 | 32.7 MP Still Photos | 2 | Sony ILCE-7M4 (Alpha 7 IV) |
| `6048x4024` | 3:2 | 24.3 MP Still Photos | 1 | Nikon Z 6 |
| `8000x6000` | 4:3 | 48.0 MP High-Res Photo | 1 | Xiaomi Redmi Note 10 (`20221015-110225.jpg`)|

### 7.5 Video Codecs (3 Distinct Values, 18 Populated)
| Video Codec | File Count | Meaning / Profile |
| :--- | :---: | :--- |
| `vp09` | 9 | Google VP9 Video Codec (Web/Social Videos) |
| `avc1` | 8 | Advanced Video Coding / H.264 (Canon 5D3, Canon R6, Nikon B700, Sony Videos) |
| `A_OPUS` | 1 | WebM Container Video Stream |

### 7.6 Audio Codecs (4 Distinct Values, 18 Populated)
| Audio Codec | File Count | Meaning / Profile |
| :--- | :---: | :--- |
| `mp4a` | 12 | AAC Audio (MPEG-4 Audio Streams) |
| `twos` | 3 | 16-bit Big-Endian Uncompressed PCM (Sony Camera Audio) |
| `sowt` | 2 | 16-bit Little-Endian Uncompressed PCM (Canon 5D Mark III Audio) |
| `A_OPUS` | 1 | Opus Interactive Audio Codec |

---

## 8. Separate Image, Video, Audio, and Document Findings

### 8.1 Image & Photograph Findings
* **CR2 RAW File Support:** Canon `.CR2` RAW files (IDs 209, 213, 214) were processed by ExifTool with complete metadata extraction (resolution `5760x3840`, camera make `Canon`, model `Canon EOS 5D Mark III`, lens `EF24-70mm f/2.8L USM`, ISO `100`, shutter `1/50`, f-number `2.8`).
* **High-Resolution Parsing:** Resolutions up to 48 Megapixels (`8000x6000` on Xiaomi ID 225) and 33 Megapixels (`7008x4672` on Sony A74 ID 206) parsed without error.
* **Orientation Nuances:** ExifTool extracted orientation into `raw_metadata_json`, but orientation is not persisted to a dedicated column; portrait photos report native sensor dimensions (e.g., `4000x3000` instead of rotated `3000x4000`).

### 8.2 Video Findings
* **Camera Video Metadata:**
  * **Canon EOS 5D Mark III MP4s (IDs 215, 216):** Successfully captured make (`Canon`), model (`Canon EOS 5D Mark III`), resolution (`1920x1080`), codecs (`avc1`/`sowt`), and creation date.
  * **Canon EOS R6 4K MP4 (ID 239):** Successfully captured make (`Canon`), model (`Canon EOS R6`), resolution (`3840x2160`), codecs (`avc1`/`mp4a`), and date taken.
  * **Nikon COOLPIX B700 MP4s (IDs 231, 232):** Captured make (`NIKON`), model (`COOLPIX B700`), resolution (`1920x1080`), codecs (`avc1`/`mp4a`).
  * **Sony MP4s (IDs 221, 222, 223):** Failed to populate camera make/model due to tag naming differences in `MetadataExtractionService`.

### 8.3 Audio Findings
* **FLAC Metadata:** 3 FLAC tracks (IDs 147, 195, 196) extracted audio title, artist, and composer.
* **Coverage Gap:** Album (`3`), Genre (`4`), and Release Year (`4`) have very low representation in the vault.

### 8.4 Document Findings
* **PDF Extraction:** 8 PDF files in vault. PDFBox fallback extracted author (`ADMIN`, `AUDIT`) and creation date (`doc_created_date`).
* **Keywords:** Zero files in the vault possess PDF keywords.

---

## 9. Metadata Extraction and Persistence Findings

Tracing source code in [`MetadataExtractionService.java`](file:///c:/Users/akhil/OneDrive/Desktop/CipherVault/backend/src/main/java/com/ciphervault/ciphervault/file/MetadataExtractionService.java) and [`FileStorageService.java`](file:///c:/Users/akhil/OneDrive/Desktop/CipherVault/backend/src/main/java/com/ciphervault/ciphervault/file/FileStorageService.java):

```java
// FileStorageService.java (Lines 366-389)
// Stage 5: Extract metadata BEFORE encryption on plaintext tempSourceFile
if (metadataExtractionService != null) {
    fileMetadata = metadataExtractionService.extractMetadata(
            tempSourceFile,
            originalFilename,
            file.getContentType()
    );
}

// Stage 6: AES-256-GCM Streaming Encryption
encryptionService.encryptStream(input, output, userKey);

// Stage 9: Persist StoredFile and FileMetadata
StoredFile savedFile = fileRepository.save(storedFile);
fileMetadata.setFile(savedFile);
fileMetadataRepository.save(fileMetadata);
```

### Verified Processing Path:
1. Upload streamed to `cv_upload_src_*.ext` in temp directory.
2. ExifTool process launched: `exiftool -json -struct <path>`.
3. Output stdout read into memory buffer and parsed via Jackson `ObjectMapper`.
4. Extracted tag map populated into `FileMetadata` properties and serialized as raw JSON into `file_metadata.raw_metadata_json`.
5. Fallbacks applied if tags are null:
   * Images: `ImageIO.read()` -> `width`, `height`, `resolution`.
   * PDFs: `Loader.loadPDF()` -> `title`, `author`, `subject`, `creator`, `docCreatedDate`.
6. Streaming AES-256-GCM encryption persists ciphertext to `encrypted/` storage.
7. Plaintext metadata persisted to MySQL `file_metadata` table.

---

## 10. Existing API and Search Capabilities

### 10.1 Endpoint: `GET /api/files/search`
* **Route:** [`FileController.java#searchFiles`](file:///c:/Users/akhil/OneDrive/Desktop/CipherVault/backend/src/main/java/com/ciphervault/ciphervault/file/FileController.java#L352)
* **Authentication:** Mandatory Spring Security Bearer JWT. User is resolved via `userRepository.findByEmail(auth.getName())`.
* **User Isolation:** Strictly enforced in JPA query: `cb.equal(root.get("user"), user)`.
* **Query Parameters Supported:**
  * `query`: Free-text matching filename, MIME, or any of 14 metadata fields (`cameraMake`, `cameraModel`, `lens`, `resolution`, `videoCodec`, `audioCodec`, `artist`, `album`, `genre`, `title`, `author`, `creator`, `subject`, `keywords`).
  * `category`: Categorized by `FileCategoryService` (`IMAGES`, `VIDEOS`, `DOCUMENTS`, `AUDIO`, `ARCHIVES`).
  * `cameraMake`: Substring `LIKE` matching on `camera_make`.
  * `cameraModel`: Substring `LIKE` matching on `camera_model`.
  * `resolution`: Substring `LIKE` matching on `resolution`.
  * `codec`: Matches `video_codec` OR `audio_codec`.
  * `artist`, `author`, `genre`: Attribute substring `LIKE` matching.
  * `minSize`, `maxSize`: SQL numeric comparisons (`file_size >= minSize`, `file_size <= maxSize`).
  * `dateTaken`: Substring `LIKE` matching on `date_taken`.
  * `page`, `size`: Spring Data pagination with headers `X-Total-Count`, `X-Total-Pages`.

### 10.2 Endpoint: `GET /api/files/suggestions`
* **Route:** [`FileController.java#getSuggestions`](file:///c:/Users/akhil/OneDrive/Desktop/CipherVault/backend/src/main/java/com/ciphervault/ciphervault/file/FileController.java#L567)
* **Implementation:** Queries 9 metadata columns across the user's files with `LIKE %prefix%`, parses file extensions, tallies frequencies in an in-memory Java map, and returns the top 10 strings.
* **Limitation:** Flat string list across all fields; cannot return facet counts for a specific attribute (e.g. "available camera models").

---

## 11. Data Quality Issues and Limitations

| Issue Discovered | Affected Files | Root Cause | Impact on Search UI |
| :--- | :---: | :--- | :--- |
| **Sony Video Tag Omission** | 3 files (IDs 221, 222, 223) | `MetadataExtractionService` only checks `Make`/`Model`. ExifTool outputs `Manufacturer`/`ModelName` for Sony XAVC-S MP4s. | Sony videos do not appear when filtering by `cameraMake=SONY` or `cameraModel=ILCE-7M3`. |
| **Zero-Date Strings (`0000:00:00 00:00:00`)** | 11 files (IDs 145, 180–188, 236) | Raw MP4 headers with uninitialized clocks store zero-dates. Stored verbatim as text. | Parsing as calendar date throws `DateTimeParseException`. Corrupts chronological sorting. |
| **Undefined Camera Exposure Strings** | 5 files | ExifTool outputs `"undef"` for adapted lenses or video shutter, stored verbatim. | Cannot parse as numbers; displays awkward `"undef"` in UI. |
| **Unnormalized Duration Units** | 21 files | ExifTool outputs mixed strings (`"10.54 s"`, `"0:03:49"`). | SQL numeric range filtering (`duration > 60s`) impossible without client-side parsing. |
| **Unnormalized Bitrate Strings** | 17 files | Bitrate stored with string unit suffix (`"3.23 Mbps"`). | SQL numeric range filtering impossible. |
| **Device Model Bifurcation** | 7 files | Samsung S24 Ultra stored as `SM-S928B` (3 files) and `Galaxy S24 Ultra` (4 files). | User searching `Galaxy S24 Ultra` misses the 3 files tagged `SM-S928B`. |
| **Unstored GPS Tags** | 6 files | ExifTool extracts GPS into `raw_metadata_json`, but entity has no `latitude`/`longitude` columns. | Map/location search impossible despite raw data existing. |
| **Multiline Social Descriptions in Subject** | 9 files | Full multi-paragraph Instagram/YouTube captions stored in `file_metadata.subject`. | Clutters search results and match highlights. |

---

## 12. Recommended Filter Types for Each Useful Field

| Candidate Field | Usability Status | Current Population | Distinct Count | Recommended UI Control | Justification |
| :--- | :---: | :---: | :---: | :--- | :--- |
| **Category** | **Primary** | 100% | 5 | **Horizontal Chips** | Clean, universal selector (`All`, `Images`, `Videos`, `Docs`, `Audio`). |
| **Camera Model** | **Primary** | 48.2% | 13 | **Searchable Dropdown / Chips** | High-utility filter. Models with >3 files can be chips; others in dropdown. |
| **Camera Make** | **Primary** | 48.2% | 8 | **Compact Chips** | Small set of clean brands (Canon, Sony, Samsung, Nikon). |
| **Resolution** | **Primary** | 77.6% | 24 | **Dropdown with Quick Chips** | Quick chips for 4K (`3840x2160`), Full HD (`1920x1080`), 12MP (`4000x3000`). |
| **File Size Range** | **Primary** | 100% | Continuous | **Numeric Range (MB) Slider/Inputs** | Backend natively supports `minSize`/`maxSize` numeric comparisons. |
| **Video Codec** | **Secondary** | 21.2% | 3 | **Filter Chips** | Clean binary choices: `H.264 / AVC`, `VP9`. |
| **Lens Model** | **Secondary** | 23.5% | 6 | **Searchable Selector / List** | Great for photography enthusiasts; 6 clean lenses currently in DB. |
| **Date Taken** | **Secondary** | 77.6% | 55 | **Year/Month Presets & Text Search** | String LIKE matching works for `2026`, `2025`, `2014`. Avoid strict calendar picker due to zero-dates. |
| **Artist / Author** | **Secondary** | 16.5% | 13 | **Autocomplete Text Input** | Free-text input with suggestions. |
| **Audio Codec** | **Tertiary** | 21.2% | 4 | **Filter Chips** | `AAC`, `FLAC`, `PCM`. |
| **GPS / Location** | **Unavailable** | 0.0% (col) | 0 | **HIDDEN / DISABLED** | No database column or query support. |
| **Keywords** | **Unavailable** | 0.0% | 0 | **HIDDEN / DISABLED** | Zero files populated. |
| **ISO / Shutter / F-Stop**| **Unavailable** | 45.9% | 16 / 22 / 13 | **HIDDEN as Main Filter** | Contains `"undef"` strings; niche utility best kept inside File Details. |

---

## 13. Recommended Visibility and Suggestion Rules

### 13.1 Distinction Between the Three Visibility Conditions
1. **Condition A: Total files containing the field in the database (`files_with_field > threshold`).**
   * *Purpose:* Determines whether an entire **filter card/dimension should even exist in the UI**.
   * *Recommendation:* If fewer than **3 files** in the user's vault contain a field, the filter dimension should be hidden. (E.g. Hide `Lens` if user has only smartphone photos; hide `Keywords` entirely).
2. **Condition B: Number of distinct values for that field (`distinct_values > threshold`).**
   * *Purpose:* Determines the **UI presentation control style**.
   * *Recommendation:* 
     * **1 to 4 distinct values:** Render as **Compact Tap Chips** (e.g. Camera Make, Video Codec).
     * **5 or more distinct values:** Render as **Searchable Dropdown / Bottom Sheet Selector** with search filtering (e.g. Camera Model, Resolution).
3. **Condition C: Files matching an individual filter value (`files_per_value > threshold`).**
   * *Purpose:* Determines **dynamic suggestions and chip surfacing**.
   * *Recommendation:* Surface individual values as "Quick Filter Chips" only if they match at least **2 files** in the user's collection. Values with only 1 file remain discoverable via the full dropdown list.

### 13.2 Result Count Badges
* Displaying a result count beside each filter option (e.g. `Canon EOS 5D Mark III (9)`) is strongly recommended to set accurate user expectations and prevent zero-result queries.

---

## 14. Prioritized List of Supported Capabilities Today

The existing codebase and database can support the following features **without any backend code changes**:

1. **Category Navigation:** Instant filtering across `Images`, `Videos`, `Documents`, `Audio`, and `Archives`.
2. **Camera Make & Model Multi-Filter:** Querying `cameraMake` (e.g. `Canon`) and `cameraModel` (e.g. `Canon EOS 5D Mark III`) via `GET /api/files/search`.
3. **Resolution Filtering:** Exact resolution matching (`1920x1080`, `3840x2160`, `4000x3000`).
4. **File Size Range Filtering:** Validating min/max MB on the client and executing `minSize`/`maxSize` byte queries on the server.
5. **Video Codec Filtering:** Filtering by `avc1` (H.264) or `vp09` (VP9).
6. **Date Taken Year/Month Filtering:** Text-based substring matching (`2026`, `2025`, `2014`).
7. **Unified Free-Text Metadata Search:** Searching any term across filename and all 14 metadata attributes via the 16-way OR JPA specification.
8. **Client-Side Sorting:** Sorting search results by Date, Name, Size, or File Type using [`FileComparator.java`](file:///c:/Users/akhil/OneDrive/Desktop/CipherVault/android/app/src/main/java/com/ciphervault/app/FileComparator.java).

---

## 15. Capabilities Requiring Backend Changes

To unlock the full potential of metadata discovery, the following backend changes are required in a future phase:

1. **Dynamic Facets Endpoint:**
   * *Need:* Expose `GET /api/files/facets?attribute=cameraModel` returning distinct values and counts scoped to the user (`List<{value: String, count: Long}>`).
2. **Sony Video Extraction Fix:**
   * *Need:* Update [`MetadataExtractionService.java`](file:///c:/Users/akhil/OneDrive/Desktop/CipherVault/backend/src/main/java/com/ciphervault/ciphervault/file/MetadataExtractionService.java) line 225 to check `"Manufacturer"` in addition to `"Make"`, and `"ModelName"` in addition to `"Model"`.
3. **GPS Schema & Search:**
   * *Need:* Add `latitude` and `longitude` (`DOUBLE`) columns to `file_metadata` and expose radius/location bounding box search.
4. **Date Normalization:**
   * *Need:* Parse dates during extraction and reject/sanitize `0000:00:00 00:00:00` into `NULL`. Convert to SQL `DATETIME` or timestamp epoch for range queries.
5. **Numeric Units Normalization:**
   * *Need:* Store `duration_ms` as `BIGINT` (milliseconds) and `bitrate_bps` as `BIGINT` (bits per second) rather than raw text strings.
6. **Camera Model Aliasing:**
   * *Need:* Map hardware codes (`SM-S928B`) to consumer marketing names (`Galaxy S24 Ultra`).

---

## 16. Read-Only SQL Queries Used

The following non-destructive queries were executed directly against MySQL 8.0.46:

```sql
-- 1. Total Stored Files and Metadata Row Integrity
SELECT COUNT(*) FROM stored_files;
SELECT COUNT(*) FROM file_metadata;

-- 2. Verify 1-to-1 Relationship (Check for duplicates or orphans)
SELECT file_id, COUNT(*) FROM file_metadata GROUP BY file_id HAVING COUNT(*) > 1;
SELECT f.id FROM stored_files f LEFT JOIN file_metadata m ON m.file_id = f.id WHERE m.id IS NULL;
SELECT m.id FROM file_metadata m LEFT JOIN stored_files f ON f.id = m.file_id WHERE f.id IS NULL;

-- 3. Inventory by MIME Type and Format
SELECT content_type, COUNT(*) as cnt FROM stored_files GROUP BY content_type ORDER BY cnt DESC;

-- 4. Camera Make and Model Frequencies
SELECT camera_make, camera_model, COUNT(*) as cnt 
FROM file_metadata 
WHERE camera_make IS NOT NULL OR camera_model IS NOT NULL 
GROUP BY camera_make, camera_model ORDER BY cnt DESC;

-- 5. Lens Frequencies
SELECT lens, COUNT(*) as cnt FROM file_metadata WHERE lens IS NOT NULL GROUP BY lens ORDER BY cnt DESC;

-- 6. Resolution Frequencies
SELECT resolution, COUNT(*) as cnt FROM file_metadata WHERE resolution IS NOT NULL GROUP BY resolution ORDER BY cnt DESC;

-- 7. Video and Audio Codecs
SELECT video_codec, audio_codec, COUNT(*) as cnt 
FROM file_metadata 
WHERE video_codec IS NOT NULL OR audio_codec IS NOT NULL 
GROUP BY video_codec, audio_codec;

-- 8. Full Coverage and Null Counts Across All Attributes
SELECT 
  'camera_make' as field, COUNT(camera_make) as populated, 85 - COUNT(camera_make) as missing, COUNT(DISTINCT camera_make) as distinct_vals FROM file_metadata WHERE camera_make IS NOT NULL AND TRIM(camera_make) != ''
UNION ALL SELECT 'camera_model', COUNT(camera_model), 85 - COUNT(camera_model), COUNT(DISTINCT camera_model) FROM file_metadata WHERE camera_model IS NOT NULL AND TRIM(camera_model) != ''
UNION ALL SELECT 'lens', COUNT(lens), 85 - COUNT(lens), COUNT(DISTINCT lens) FROM file_metadata WHERE lens IS NOT NULL AND TRIM(lens) != ''
UNION ALL SELECT 'resolution', COUNT(resolution), 85 - COUNT(resolution), COUNT(DISTINCT resolution) FROM file_metadata WHERE resolution IS NOT NULL AND TRIM(resolution) != ''
UNION ALL SELECT 'date_taken', COUNT(date_taken), 85 - COUNT(date_taken), COUNT(DISTINCT date_taken) FROM file_metadata WHERE date_taken IS NOT NULL AND TRIM(date_taken) != ''
UNION ALL SELECT 'video_codec', COUNT(video_codec), 85 - COUNT(video_codec), COUNT(DISTINCT video_codec) FROM file_metadata WHERE video_codec IS NOT NULL AND TRIM(video_codec) != '';

-- 9. Check for Unextracted GPS Tags in Raw JSON
SELECT COUNT(*) FROM file_metadata WHERE raw_metadata_json LIKE '%GPSLatitude%' OR raw_metadata_json LIKE '%GPSPosition%';

-- 10. Check Zero-Date String Anomalies
SELECT date_taken, COUNT(*) FROM file_metadata WHERE date_taken LIKE '0000%' GROUP BY date_taken;
```

---

## 17. Verification Results and Evidence

* **SQL Execution Exit Code:** Code `0` across all read-only diagnostic invocations.
* **Database State:** 85 stored files, 85 metadata rows.
* **Integrity Proof:** Zero orphan rows, zero duplicate joins.
* **Code References Verified:**
  * Extraction timing: [`FileStorageService.java#L366-L405`](file:///c:/Users/akhil/OneDrive/Desktop/CipherVault/backend/src/main/java/com/ciphervault/ciphervault/file/FileStorageService.java#L366)
  * ExifTool execution: [`MetadataExtractionService.java#L108-L218`](file:///c:/Users/akhil/OneDrive/Desktop/CipherVault/backend/src/main/java/com/ciphervault/ciphervault/file/MetadataExtractionService.java#L108)
  * Search Specification: [`FileSearchService.java#L149-L283`](file:///c:/Users/akhil/OneDrive/Desktop/CipherVault/backend/src/main/java/com/ciphervault/ciphervault/file/FileSearchService.java#L149)
  * Controller search endpoint: [`FileController.java#L352-L534`](file:///c:/Users/akhil/OneDrive/Desktop/CipherVault/backend/src/main/java/com/ciphervault/ciphervault/file/FileController.java#L352)

---

## 18. Limitations Caused by Test Data and Missing Files

1. **Redmi Note 7 Pro Sample Missing:** The database does not contain files from a Redmi Note 7 Pro. Only a Redmi Note 10 (`M2101K7AI`) is present. If testing against a Redmi Note 7 Pro is mandatory, sample files from that device will need to be uploaded.
2. **Audio Track Diversity:** FLAC tracks are limited to 3 files with identical artist/composer tags. Audio album and genre filtering cannot be thoroughly validated until more varied tracks are uploaded.
3. **Keyword Metadata Absence:** Zero files in the current vault contain document keywords. UI controls for document keywords cannot be verified with live data.
4. **Read-Only Scope Compliance:** In accordance with execution rules, no database records were edited, repaired, inserted, or migrated, and no application source code was modified during this audit.
