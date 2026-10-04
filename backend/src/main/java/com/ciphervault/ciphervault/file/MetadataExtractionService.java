package com.ciphervault.ciphervault.file;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDDocumentInformation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.TimeUnit;

@Service
public class MetadataExtractionService {

    private static final Logger log = LoggerFactory.getLogger(MetadataExtractionService.class);
    private final ObjectMapper objectMapper = new ObjectMapper();

    public FileMetadata extractMetadata(Path sourceFile, String filename, String contentType) {
        FileMetadata metadata = new FileMetadata();
        if (sourceFile == null || !Files.exists(sourceFile)) {
            return metadata;
        }

        try {
            // 1. Primary extraction via ExifTool
            Map<String, Object> tags = runExifTool(sourceFile);
            if (tags != null && !tags.isEmpty()) {
                populateFromExifTool(metadata, tags);
                try {
                    String rawJson = objectMapper.writeValueAsString(tags);
                    if (rawJson.length() > 65000) {
                        rawJson = rawJson.substring(0, 65000);
                    }
                    metadata.setRawMetadataJson(rawJson);
                } catch (Exception ignored) {
                }
            }

            // 2. Fallbacks for missing essential dimensions or document properties
            applyFallbacks(metadata, sourceFile, filename, contentType);

            com.ciphervault.ciphervault.logging.ConsoleLogger.logMetadataExtracted(
                    filename,
                    "ExifTool",
                    metadata.getRawMetadataJson() != null || metadata.getCameraMake() != null,
                    metadata.getCameraMake() != null || metadata.getCameraModel() != null,
                    metadata.getDateTaken() != null,
                    metadata.getResolution() != null
            );

        } catch (Exception e) {
            log.warn("Non-fatal metadata extraction error for {}: {}", filename, e.getMessage());
        }

        return metadata;
    }

    private Map<String, Object> runExifTool(Path path) {
        try {
            ProcessBuilder pb = new ProcessBuilder("exiftool", "-json", "-struct", path.toAbsolutePath().toString());
            pb.redirectErrorStream(true);
            Process process = pb.start();

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            try (InputStream is = process.getInputStream()) {
                byte[] buffer = new byte[4096];
                int n;
                while ((n = is.read(buffer)) != -1) {
                    baos.write(buffer, 0, n);
                }
            }

            boolean finished = process.waitFor(10, TimeUnit.SECONDS);
            if (!finished) {
                process.destroyForcibly();
                log.warn("ExifTool timed out on file: {}", path.getFileName());
                return Collections.emptyMap();
            }

            if (process.exitValue() == 0 && baos.size() > 0) {
                List<Map<String, Object>> list = objectMapper.readValue(baos.toByteArray(), new TypeReference<>() {});
                if (list != null && !list.isEmpty()) {
                    return list.get(0);
                }
            }
        } catch (Exception e) {
            log.debug("ExifTool execution skipped or failed: {}", e.getMessage());
        }
        return Collections.emptyMap();
    }

    private void populateFromExifTool(FileMetadata meta, Map<String, Object> tags) {
        // Image / Camera
        meta.setCameraMake(getTagString(tags, "Make"));
        meta.setCameraModel(getTagString(tags, "Model"));
        meta.setLens(getTagString(tags, "LensModel", "Lens", "LensID"));
        meta.setFocalLength(getTagString(tags, "FocalLength"));
        meta.setIso(getTagString(tags, "ISO"));
        meta.setExposureTime(getTagString(tags, "ExposureTime", "ShutterSpeed"));
        meta.setFNumber(getTagString(tags, "FNumber", "ApertureValue", "Aperture"));
        meta.setDateTaken(getTagString(tags, "DateTimeOriginal", "CreateDate", "DateCreated"));

        // Dimensions
        Integer width = getTagInt(tags, "ImageWidth", "SourceImageWidth");
        Integer height = getTagInt(tags, "ImageHeight", "SourceImageHeight");
        meta.setWidth(width);
        meta.setHeight(height);
        if (width != null && height != null && width > 0 && height > 0) {
            meta.setResolution(width + "x" + height);
        }

        // Video / Audio
        meta.setDuration(getTagString(tags, "Duration"));
        meta.setVideoCodec(getTagString(tags, "VideoCodec", "CompressorID", "CodecID", "VideoCompression"));
        meta.setAudioCodec(getTagString(tags, "AudioFormat", "AudioCodec", "AudioChannels"));
        meta.setFrameRate(getTagString(tags, "VideoFrameRate", "FrameRate"));
        meta.setBitrate(getTagString(tags, "AvgBitrate", "Bitrate"));

        // Audio
        meta.setTitle(getTagString(tags, "Title", "SongTitle"));
        meta.setArtist(getTagString(tags, "Artist", "Band", "Composer"));
        meta.setAlbum(getTagString(tags, "Album"));
        meta.setGenre(getTagString(tags, "Genre"));
        meta.setReleaseYear(getTagString(tags, "Year", "RecordingYear", "Date"));

        // Document / PDF
        meta.setAuthor(getTagString(tags, "Author", "By-line", "Artist"));
        meta.setCreator(getTagString(tags, "Creator", "Producer", "Software", "Application"));
        meta.setSubject(getTagString(tags, "Subject", "Description"));
        meta.setKeywords(getTagString(tags, "Keywords"));
        meta.setDocCreatedDate(getTagString(tags, "CreateDate", "CreationDate"));
        meta.setDocModifiedDate(getTagString(tags, "ModifyDate", "ModDate"));
    }

    private void applyFallbacks(FileMetadata meta, Path sourceFile, String filename, String contentType) {
        String mime = contentType != null ? contentType.toLowerCase() : "";
        String name = filename != null ? filename.toLowerCase() : "";

        // Fallback for image dimensions via ImageIO
        if (meta.getWidth() == null || meta.getHeight() == null) {
            if (mime.startsWith("image/") || name.endsWith(".jpg") || name.endsWith(".png") || name.endsWith(".bmp") || name.endsWith(".gif")) {
                try {
                    BufferedImage bimg = ImageIO.read(sourceFile.toFile());
                    if (bimg != null) {
                        meta.setWidth(bimg.getWidth());
                        meta.setHeight(bimg.getHeight());
                        meta.setResolution(bimg.getWidth() + "x" + bimg.getHeight());
                    }
                } catch (Exception ignored) {
                }
            }
        }

        // Fallback for PDF metadata via PDFBox
        if ("application/pdf".equalsIgnoreCase(mime) || name.endsWith(".pdf")) {
            if (meta.getTitle() == null || meta.getAuthor() == null) {
                try (PDDocument doc = Loader.loadPDF(sourceFile.toFile())) {
                    PDDocumentInformation info = doc.getDocumentInformation();
                    if (info != null) {
                        if (meta.getTitle() == null && info.getTitle() != null && !info.getTitle().isBlank()) {
                            meta.setTitle(info.getTitle());
                        }
                        if (meta.getAuthor() == null && info.getAuthor() != null && !info.getAuthor().isBlank()) {
                            meta.setAuthor(info.getAuthor());
                        }
                        if (meta.getSubject() == null && info.getSubject() != null && !info.getSubject().isBlank()) {
                            meta.setSubject(info.getSubject());
                        }
                        if (meta.getKeywords() == null && info.getKeywords() != null && !info.getKeywords().isBlank()) {
                            meta.setKeywords(info.getKeywords());
                        }
                        if (meta.getCreator() == null && info.getCreator() != null && !info.getCreator().isBlank()) {
                            meta.setCreator(info.getCreator());
                        }
                        if (meta.getDocCreatedDate() == null && info.getCreationDate() != null) {
                            SimpleDateFormat sdf = new SimpleDateFormat("yyyy:MM:dd HH:mm:ss");
                            meta.setDocCreatedDate(sdf.format(info.getCreationDate().getTime()));
                        }
                    }
                } catch (Exception ignored) {
                }
            }
        }
    }

    private String getTagString(Map<String, Object> tags, String... tagNames) {
        for (String tagName : tagNames) {
            Object val = tags.get(tagName);
            if (val != null) {
                String str = val.toString().trim();
                if (!str.isEmpty() && !str.equalsIgnoreCase("null")) {
                    return str;
                }
            }
        }
        return null;
    }

    private Integer getTagInt(Map<String, Object> tags, String... tagNames) {
        for (String tagName : tagNames) {
            Object val = tags.get(tagName);
            if (val != null) {
                if (val instanceof Number) {
                    return ((Number) val).intValue();
                }
                try {
                    String str = val.toString().trim().replaceAll("[^0-9]", "");
                    if (!str.isEmpty()) {
                        return Integer.parseInt(str);
                    }
                } catch (Exception ignored) {
                }
            }
        }
        return null;
    }
}
