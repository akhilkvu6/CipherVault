package com.ciphervault.ciphervault.file;

import com.ciphervault.ciphervault.logging.ConsoleLogger;
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
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

@Service
public class MetadataExtractionService {

    private static final Logger log =
            LoggerFactory.getLogger(MetadataExtractionService.class);

    private static final int PROCESS_BUFFER_SIZE = 4096;
    private static final int EXIFTOOL_TIMEOUT_SECONDS = 10;

    private final ObjectMapper objectMapper = new ObjectMapper();

    public FileMetadata extractMetadata(
            Path sourceFile,
            String filename,
            String contentType) {

        FileMetadata metadata = new FileMetadata();

        if (sourceFile == null || !Files.isRegularFile(sourceFile)) {
            return metadata;
        }

        try {
            // Primary metadata extraction is performed by ExifTool.
            Map<String, Object> tags = runExifTool(sourceFile);

            if (!tags.isEmpty()) {
                populateFromExifTool(metadata, tags);
                storeRawMetadata(metadata, tags);
            }

            // Use native libraries when ExifTool does not provide required fields.
            applyFallbacks(
                    metadata,
                    sourceFile,
                    filename,
                    contentType
            );

            ConsoleLogger.logMetadataTrace(
                    null,
                    filename,
                    "ExifTool",
                    contentType != null ? contentType : "unknown",
                    metadata.getCameraMake(),
                    metadata.getCameraModel(),
                    metadata.getResolution(),
                    metadata.getDateTaken(),
                    0
            );

        } catch (Exception e) {
            log.warn(
                    "Non-fatal metadata extraction error for {}: {}",
                    filename,
                    e.getMessage()
            );
        }

        return metadata;
    }

    private void storeRawMetadata(
            FileMetadata metadata,
            Map<String, Object> tags) {

        try {
            String rawJson =
                    objectMapper.writeValueAsString(tags);

            metadata.setRawMetadataJson(rawJson);

        } catch (Exception e) {
            log.debug(
                    "Could not serialize raw metadata: {}",
                    e.getMessage()
            );
        }
    }

    private Map<String, Object> runExifTool(Path path) {
        Process process = null;
        CompletableFuture<byte[]> outputFuture = null;

        try {
            ProcessBuilder processBuilder =
                    new ProcessBuilder(
                            "exiftool",
                            "-json",
                            "-struct",
                            path.toAbsolutePath().toString()
                    );

            // Keep metadata JSON on stdout and discard diagnostic output.
            processBuilder.redirectError(
                    ProcessBuilder.Redirect.DISCARD
            );

            process = processBuilder.start();

            Process runningProcess = process;

            outputFuture =
                    CompletableFuture.supplyAsync(() -> {
                        try (
                                InputStream input =
                                        runningProcess.getInputStream();

                                ByteArrayOutputStream output =
                                        new ByteArrayOutputStream()
                        ) {
                            byte[] buffer =
                                    new byte[PROCESS_BUFFER_SIZE];

                            int bytesRead;

                            while ((bytesRead =
                                    input.read(buffer)) != -1) {

                                output.write(
                                        buffer,
                                        0,
                                        bytesRead
                                );
                            }

                            return output.toByteArray();

                        } catch (Exception e) {
                            return new byte[0];
                        }
                    });

            boolean finished =
                    process.waitFor(
                            EXIFTOOL_TIMEOUT_SECONDS,
                            TimeUnit.SECONDS
                    );

            if (!finished) {
                process.destroyForcibly();
                outputFuture.cancel(true);

                log.warn(
                        "ExifTool timed out on file: {}",
                        path.getFileName()
                );

                return Collections.emptyMap();
            }

            byte[] output =
                    outputFuture.get(
                            2,
                            TimeUnit.SECONDS
                    );

            if (process.exitValue() != 0
                    || output.length == 0) {

                return Collections.emptyMap();
            }

            List<Map<String, Object>> metadata =
                    objectMapper.readValue(
                            output,
                            new TypeReference<>() {
                            }
                    );

            if (metadata != null && !metadata.isEmpty()) {
                return metadata.get(0);
            }

        } catch (Exception e) {
            log.debug(
                    "ExifTool execution skipped or failed: {}",
                    e.getMessage()
            );

            if (process != null && process.isAlive()) {
                process.destroyForcibly();
            }

            if (outputFuture != null) {
                outputFuture.cancel(true);
            }
        }

        return Collections.emptyMap();
    }

    private void populateFromExifTool(
            FileMetadata metadata,
            Map<String, Object> tags) {

        // Image and camera metadata.
        metadata.setCameraMake(
                getTagString(tags, "Make")
        );

        metadata.setCameraModel(
                getTagString(tags, "Model")
        );

        metadata.setLens(
                getTagString(
                        tags,
                        "LensModel",
                        "Lens",
                        "LensID"
                )
        );

        metadata.setFocalLength(
                getTagString(tags, "FocalLength")
        );

        metadata.setIso(
                getTagString(tags, "ISO")
        );

        metadata.setExposureTime(
                getTagString(
                        tags,
                        "ExposureTime",
                        "ShutterSpeed"
                )
        );

        metadata.setFNumber(
                getTagString(
                        tags,
                        "FNumber",
                        "ApertureValue",
                        "Aperture"
                )
        );

        metadata.setDateTaken(
                getTagString(
                        tags,
                        "DateTimeOriginal",
                        "CreateDate",
                        "DateCreated"
                )
        );

        // Image dimensions.
        Integer width =
                getTagInt(
                        tags,
                        "ImageWidth",
                        "SourceImageWidth"
                );

        Integer height =
                getTagInt(
                        tags,
                        "ImageHeight",
                        "SourceImageHeight"
                );

        metadata.setWidth(width);
        metadata.setHeight(height);

        if (width != null
                && height != null
                && width > 0
                && height > 0) {

            metadata.setResolution(
                    width + "x" + height
            );
        }

        // Video and audio metadata.
        metadata.setDuration(
                getTagString(tags, "Duration")
        );

        metadata.setVideoCodec(
                getTagString(
                        tags,
                        "VideoCodec",
                        "CompressorID",
                        "CodecID",
                        "HandlerDescription",
                        "VideoCompression"
                )
        );

        metadata.setAudioCodec(
                getTagString(
                        tags,
                        "AudioCodec",
                        "AudioFormat",
                        "CodecID"
                )
        );

        metadata.setFrameRate(
                getTagString(
                        tags,
                        "VideoFrameRate",
                        "FrameRate"
                )
        );

        metadata.setBitrate(
                getTagString(
                        tags,
                        "AvgBitrate",
                        "Bitrate"
                )
        );

        // Audio metadata.
        metadata.setTitle(
                getTagString(
                        tags,
                        "Title",
                        "SongTitle"
                )
        );

        metadata.setArtist(
                getTagString(
                        tags,
                        "Artist",
                        "Band",
                        "Composer"
                )
        );

        metadata.setAlbum(
                getTagString(tags, "Album")
        );

        metadata.setGenre(
                getTagString(tags, "Genre")
        );

        metadata.setReleaseYear(
                getTagString(
                        tags,
                        "Year",
                        "RecordingYear",
                        "Date"
                )
        );

        // Document and PDF metadata.
        metadata.setAuthor(
                getTagString(
                        tags,
                        "Author",
                        "By-line"
                )
        );

        metadata.setCreator(
                getTagString(
                        tags,
                        "Creator",
                        "Producer",
                        "Software",
                        "Application"
                )
        );

        metadata.setSubject(
                getTagString(
                        tags,
                        "Subject",
                        "Description"
                )
        );

        metadata.setKeywords(
                getTagString(tags, "Keywords")
        );

        metadata.setDocCreatedDate(
                getTagString(
                        tags,
                        "CreateDate",
                        "CreationDate"
                )
        );

        metadata.setDocModifiedDate(
                getTagString(
                        tags,
                        "ModifyDate",
                        "ModDate"
                )
        );
    }

    private void applyFallbacks(
            FileMetadata metadata,
            Path sourceFile,
            String filename,
            String contentType) {

        String mime =
                contentType != null
                        ? contentType.toLowerCase(Locale.ROOT)
                        : "";

        String name =
                filename != null
                        ? filename.toLowerCase(Locale.ROOT)
                        : "";

        // Fall back to ImageIO when ExifTool did not provide dimensions.
        if (metadata.getWidth() == null
                || metadata.getHeight() == null) {

            boolean imageFile =
                    mime.startsWith("image/")
                            || name.endsWith(".jpg")
                            || name.endsWith(".jpeg")
                            || name.endsWith(".png")
                            || name.endsWith(".bmp")
                            || name.endsWith(".gif")
                            || name.endsWith(".webp");

            if (imageFile) {
                applyImageDimensions(
                        metadata,
                        sourceFile
                );
            }
        }

        // Fall back to PDFBox for missing PDF document properties.
        boolean pdfFile =
                "application/pdf".equalsIgnoreCase(mime)
                        || name.endsWith(".pdf");

        if (pdfFile && hasMissingPdfMetadata(metadata)) {
            applyPdfMetadata(
                    metadata,
                    sourceFile
            );
        }
    }

    private void applyImageDimensions(
            FileMetadata metadata,
            Path sourceFile) {

        try {
            BufferedImage image =
                    ImageIO.read(sourceFile.toFile());

            if (image != null) {
                metadata.setWidth(image.getWidth());
                metadata.setHeight(image.getHeight());
                metadata.setResolution(
                        image.getWidth()
                                + "x"
                                + image.getHeight()
                );
            }

        } catch (Exception e) {
            log.debug(
                    "Image dimension fallback failed: {}",
                    e.getMessage()
            );
        }
    }

    private boolean hasMissingPdfMetadata(
            FileMetadata metadata) {

        return metadata.getTitle() == null
                || metadata.getAuthor() == null
                || metadata.getSubject() == null
                || metadata.getKeywords() == null
                || metadata.getCreator() == null
                || metadata.getDocCreatedDate() == null;
    }

    private void applyPdfMetadata(
            FileMetadata metadata,
            Path sourceFile) {

        try (PDDocument document =
                     Loader.loadPDF(sourceFile.toFile())) {

            PDDocumentInformation information =
                    document.getDocumentInformation();

            if (information == null) {
                return;
            }

            if (metadata.getTitle() == null
                    && information.getTitle() != null
                    && !information.getTitle().isBlank()) {

                metadata.setTitle(
                        information.getTitle()
                );
            }

            if (metadata.getAuthor() == null
                    && information.getAuthor() != null
                    && !information.getAuthor().isBlank()) {

                metadata.setAuthor(
                        information.getAuthor()
                );
            }

            if (metadata.getSubject() == null
                    && information.getSubject() != null
                    && !information.getSubject().isBlank()) {

                metadata.setSubject(
                        information.getSubject()
                );
            }

            if (metadata.getKeywords() == null
                    && information.getKeywords() != null
                    && !information.getKeywords().isBlank()) {

                metadata.setKeywords(
                        information.getKeywords()
                );
            }

            if (metadata.getCreator() == null
                    && information.getCreator() != null
                    && !information.getCreator().isBlank()) {

                metadata.setCreator(
                        information.getCreator()
                );
            }

            if (metadata.getDocCreatedDate() == null
                    && information.getCreationDate() != null) {

                SimpleDateFormat dateFormat =
                        new SimpleDateFormat(
                                "yyyy:MM:dd HH:mm:ss"
                        );

                metadata.setDocCreatedDate(
                        dateFormat.format(
                                information
                                        .getCreationDate()
                                        .getTime()
                        )
                );
            }

        } catch (Exception e) {
            log.debug(
                    "PDF metadata fallback failed: {}",
                    e.getMessage()
            );
        }
    }

    private String getTagString(
            Map<String, Object> tags,
            String... tagNames) {

        for (String tagName : tagNames) {
            Object value = tags.get(tagName);

            if (value == null) {
                continue;
            }

            String stringValue =
                    value.toString().trim();

            if (!stringValue.isEmpty()
                    && !stringValue.equalsIgnoreCase("null")) {

                return stringValue;
            }
        }

        return null;
    }

    private Integer getTagInt(
            Map<String, Object> tags,
            String... tagNames) {

        for (String tagName : tagNames) {
            Object value = tags.get(tagName);

            if (value == null) {
                continue;
            }

            if (value instanceof Number number) {
                return number.intValue();
            }

            try {
                String digits =
                        value.toString()
                                .trim()
                                .replaceAll("[^0-9]", "");

                if (!digits.isEmpty()) {
                    return Integer.parseInt(digits);
                }

            } catch (Exception e) {
                log.debug(
                        "Could not parse metadata integer {}: {}",
                        tagName,
                        e.getMessage()
                );
            }
        }

        return null;
    }
}