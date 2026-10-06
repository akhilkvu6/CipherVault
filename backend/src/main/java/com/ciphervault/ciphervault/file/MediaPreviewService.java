package com.ciphervault.ciphervault.file;

import com.ciphervault.ciphervault.logging.ConsoleLogger;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.jcodec.api.FrameGrab;
import org.jcodec.common.model.Picture;
import org.jcodec.scale.AWTUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

@Service
public class MediaPreviewService {

    private static final Logger log =
            LoggerFactory.getLogger(MediaPreviewService.class);

    private static final int MAX_PREVIEW_DIMENSION = 384;
    private static final int PROCESS_BUFFER_SIZE = 4096;

    private static final List<String> COMMON_FFMPEG_PATHS = List.of(
            System.getProperty("user.home")
                    + "\\AppData\\Local\\Microsoft\\WinGet\\Links\\ffmpeg.exe",
            "C:\\Program Files\\ffmpeg\\bin\\ffmpeg.exe",
            "C:\\ffmpeg\\bin\\ffmpeg.exe",
            "/usr/bin/ffmpeg",
            "/usr/local/bin/ffmpeg",
            "/opt/homebrew/bin/ffmpeg"
    );

    public byte[] generatePreview(
            Path sourceFile,
            String filename,
            String contentType) {

        if (sourceFile == null || !Files.isRegularFile(sourceFile)) {
            return null;
        }

        try {
            if (Files.size(sourceFile) == 0) {
                return null;
            }

            String mime = contentType == null
                    ? ""
                    : contentType.toLowerCase(Locale.ROOT);

            String name = filename == null
                    ? ""
                    : filename.toLowerCase(Locale.ROOT);

            byte[] result;
            String type;
            String generator;

            if (isImage(mime, name)) {
                type = "IMAGE";
                generator = "ImageIO";
                result = generateImagePreview(sourceFile);

            } else if (isPdf(mime, name)) {
                type = "PDF";
                generator = "PDFBox";
                result = generatePdfPreview(sourceFile);

            } else if (isVideo(mime, name)) {
                type = "VIDEO";
                generator = "FFmpeg / JCodec";
                result = generateVideoPreview(sourceFile);

            } else if (isAudio(mime, name)) {
                type = "AUDIO";
                generator = "ExifTool ID3";
                result = generateAudioPreview(sourceFile);

            } else {
                ConsoleLogger.stage(
                        null,
                        ConsoleLogger.TAG_PREVIEW,
                        "Preview unavailable for " + filename + ": No compatible preview generator"
                );
                return null;
            }

            if (result != null && result.length > 0) {
                ConsoleLogger.stage(
                        null,
                        ConsoleLogger.TAG_PREVIEW,
                        "Preview generated for " + filename + " via " + generator
                );
                return result;
            }

            ConsoleLogger.stage(
                    null,
                    ConsoleLogger.TAG_PREVIEW,
                    "Preview unavailable for " + filename + ": Frame extraction yielded empty preview"
            );

        } catch (Exception e) {
            log.warn(
                    "Preview generation failed for {}: {}",
                    filename,
                    e.getMessage()
            );

            ConsoleLogger.stage(
                    null,
                    ConsoleLogger.TAG_PREVIEW,
                    "Preview generation failed for " + filename + ": " + e.getMessage()
            );
        }

        return null;
    }

    // Preserve the byte-array overload for existing tests and compatibility.
    public byte[] generatePreview(
            byte[] originalData,
            String filename,
            String contentType) {

        if (originalData == null || originalData.length == 0) {
            return null;
        }

        Path temp = null;

        try {
            temp = Files.createTempFile(
                    "cv_data_prev_",
                    getExtension(filename)
            );

            Files.write(temp, originalData);

            return generatePreview(
                    temp,
                    filename,
                    contentType
            );

        } catch (Exception e) {
            log.warn(
                    "Preview generation error from bytes: {}",
                    e.getMessage()
            );
            return null;

        } finally {
            cleanupTempFile(temp);
        }
    }

    private boolean isImage(
            String mime,
            String name) {

        return mime.startsWith("image/")
                || name.endsWith(".jpg")
                || name.endsWith(".jpeg")
                || name.endsWith(".png")
                || name.endsWith(".webp")
                || name.endsWith(".gif")
                || name.endsWith(".bmp");
    }

    private boolean isPdf(
            String mime,
            String name) {

        return "application/pdf".equalsIgnoreCase(mime)
                || name.endsWith(".pdf");
    }

    private boolean isVideo(
            String mime,
            String name) {

        return mime.startsWith("video/")
                || name.endsWith(".mp4")
                || name.endsWith(".mkv")
                || name.endsWith(".avi")
                || name.endsWith(".mov")
                || name.endsWith(".webm")
                || name.endsWith(".3gp")
                || name.endsWith(".flv");
    }

    private boolean isAudio(
            String mime,
            String name) {

        return mime.startsWith("audio/")
                || name.endsWith(".mp3")
                || name.endsWith(".m4a")
                || name.endsWith(".flac")
                || name.endsWith(".aac")
                || name.endsWith(".ogg")
                || name.endsWith(".wav");
    }

    private byte[] generatePdfPreview(Path sourceFile) {
        try (PDDocument document =
                     Loader.loadPDF(sourceFile.toFile())) {

            if (document.getNumberOfPages() > 0) {
                PDFRenderer renderer =
                        new PDFRenderer(document);

                BufferedImage image =
                        renderer.renderImageWithDPI(0, 96);

                if (image != null) {
                    return scaleAndEncodeJpeg(image);
                }
            }

        } catch (Exception e) {
            log.warn(
                    "PDF preview extraction error: {}",
                    e.getMessage()
            );
        }

        return null;
    }

    private byte[] generateImagePreview(Path sourceFile) {
        try {
            BufferedImage original =
                    ImageIO.read(sourceFile.toFile());

            if (original == null) {
                // Try embedded thumbnails when ImageIO cannot decode the source.
                byte[] extracted =
                        extractWithExiftool(
                                sourceFile.toFile(),
                                "-ThumbnailImage"
                        );

                if (extracted == null || extracted.length == 0) {
                    extracted =
                            extractWithExiftool(
                                    sourceFile.toFile(),
                                    "-PreviewImage"
                            );
                }

                if (extracted != null && extracted.length > 0) {
                    original =
                            ImageIO.read(
                                    new ByteArrayInputStream(extracted)
                            );
                }
            }

            if (original == null) {
                return null;
            }

            return scaleAndEncodeJpeg(original);

        } catch (Exception e) {
            log.warn(
                    "Image preview processing error: {}",
                    e.getMessage()
            );
            return null;
        }
    }

    private byte[] generateVideoPreview(Path sourceFile) {
        Path tempOutput = null;

        try {
            tempOutput =
                    Files.createTempFile(
                            "cv_vid_out_",
                            ".jpg"
                    );

            // Primary engine: FFmpeg supports the broadest range of video formats.
            String ffmpegPath =
                    findFfmpegExecutable();

            if (ffmpegPath != null) {
                boolean success =
                        runFfmpegFrameExtract(
                                ffmpegPath,
                                sourceFile,
                                tempOutput,
                                "00:00:01"
                        );

                if (!success
                        || !Files.exists(tempOutput)
                        || Files.size(tempOutput) == 0) {

                    success =
                            runFfmpegFrameExtract(
                                    ffmpegPath,
                                    sourceFile,
                                    tempOutput,
                                    "00:00:00"
                            );
                }

                if (success
                        && Files.exists(tempOutput)
                        && Files.size(tempOutput) > 0) {

                    BufferedImage frame =
                            ImageIO.read(
                                    tempOutput.toFile()
                            );

                    if (frame != null) {
                        log.debug(
                                "Video preview generated via FFmpeg: {}",
                                sourceFile.getFileName()
                        );

                        return scaleAndEncodeJpeg(frame);
                    }
                }
            }

            // Fallback: JCodec provides a pure-Java MP4/MOV decoder.
            byte[] jcodecResult =
                    extractFrameWithJCodec(sourceFile);

            if (jcodecResult != null
                    && jcodecResult.length > 0) {

                log.debug(
                        "Video preview generated via JCodec fallback: {}",
                        sourceFile.getFileName()
                );

                return jcodecResult;
            }

            // Final fallback: extract embedded artwork/preview metadata.
            byte[] cover =
                    extractWithExiftool(
                            sourceFile.toFile(),
                            "-CoverArt"
                    );

            if (cover == null || cover.length == 0) {
                cover =
                        extractWithExiftool(
                                sourceFile.toFile(),
                                "-PreviewImage"
                        );
            }

            if (cover != null && cover.length > 0) {
                BufferedImage image =
                        ImageIO.read(
                                new ByteArrayInputStream(cover)
                        );

                if (image != null) {
                    return scaleAndEncodeJpeg(image);
                }
            }

        } catch (Exception e) {
            log.warn(
                    "Video preview frame extraction failed: {}",
                    e.getMessage()
            );

        } finally {
            cleanupTempFile(tempOutput);
        }

        return null;
    }

    private byte[] extractFrameWithJCodec(Path sourceFile) {
        try {
            File file = sourceFile.toFile();

            Picture picture = null;

            try {
                picture =
                        FrameGrab.getFrameFromFile(
                                file,
                                1
                        );
            } catch (Exception ignored) {
            }

            if (picture == null) {
                try {
                    picture =
                            FrameGrab.getFrameFromFile(
                                    file,
                                    0
                            );
                } catch (Exception ignored) {
                }
            }

            if (picture != null) {
                BufferedImage image =
                        AWTUtil.toBufferedImage(picture);

                if (image != null) {
                    return scaleAndEncodeJpeg(image);
                }
            }

        } catch (Exception | LinkageError e) {
            log.debug(
                    "JCodec frame extraction skipped: {}",
                    e.getMessage()
            );
        }

        return null;
    }

    private byte[] generateAudioPreview(Path sourceFile) {
        try {
            byte[] artwork =
                    extractWithExiftool(
                            sourceFile.toFile(),
                            "-Picture"
                    );

            if (artwork == null || artwork.length == 0) {
                artwork =
                        extractWithExiftool(
                                sourceFile.toFile(),
                                "-CoverArt"
                        );
            }

            if (artwork != null && artwork.length > 0) {
                BufferedImage image =
                        ImageIO.read(
                                new ByteArrayInputStream(artwork)
                        );

                if (image != null) {
                    return scaleAndEncodeJpeg(image);
                }
            }

        } catch (Exception e) {
            log.warn(
                    "Audio artwork extraction error: {}",
                    e.getMessage()
            );
        }

        return null;
    }

    private byte[] scaleAndEncodeJpeg(
            BufferedImage original) throws Exception {

        int originalWidth =
                original.getWidth();

        int originalHeight =
                original.getHeight();

        int targetWidth =
                originalWidth;

        int targetHeight =
                originalHeight;

        if (originalWidth > MAX_PREVIEW_DIMENSION
                || originalHeight > MAX_PREVIEW_DIMENSION) {

            if (originalWidth >= originalHeight) {
                targetWidth =
                        MAX_PREVIEW_DIMENSION;

                targetHeight =
                        Math.max(
                                1,
                                (originalHeight
                                        * MAX_PREVIEW_DIMENSION)
                                        / originalWidth
                        );

            } else {
                targetHeight =
                        MAX_PREVIEW_DIMENSION;

                targetWidth =
                        Math.max(
                                1,
                                (originalWidth
                                        * MAX_PREVIEW_DIMENSION)
                                        / originalHeight
                        );
            }
        }

        BufferedImage resized =
                new BufferedImage(
                        targetWidth,
                        targetHeight,
                        BufferedImage.TYPE_INT_RGB
                );

        Graphics2D graphics =
                resized.createGraphics();

        try {
            graphics.setRenderingHint(
                    RenderingHints.KEY_INTERPOLATION,
                    RenderingHints.VALUE_INTERPOLATION_BILINEAR
            );

            graphics.setRenderingHint(
                    RenderingHints.KEY_RENDERING,
                    RenderingHints.VALUE_RENDER_QUALITY
            );

            graphics.setRenderingHint(
                    RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON
            );

            graphics.setColor(Color.WHITE);

            graphics.fillRect(
                    0,
                    0,
                    targetWidth,
                    targetHeight
            );

            graphics.drawImage(
                    original,
                    0,
                    0,
                    targetWidth,
                    targetHeight,
                    null
            );

        } finally {
            graphics.dispose();
        }

        ByteArrayOutputStream output =
                new ByteArrayOutputStream();

        ImageIO.write(
                resized,
                "jpg",
                output
        );

        return output.toByteArray();
    }

    private boolean runFfmpegFrameExtract(
            String ffmpegPath,
            Path inputPath,
            Path outputPath,
            String seekTime) {

        Process process = null;

        try {
            ProcessBuilder processBuilder =
                    new ProcessBuilder(
                            ffmpegPath,
                            "-y",
                            "-loglevel",
                            "error",
                            "-ss",
                            seekTime,
                            "-i",
                            inputPath
                                    .toAbsolutePath()
                                    .toString(),
                            "-vframes",
                            "1",
                            "-vf",
                            "scale=min("
                                    + MAX_PREVIEW_DIMENSION
                                    + ",iw):-2",
                            "-f",
                            "image2",
                            outputPath
                                    .toAbsolutePath()
                                    .toString()
                    );

            // Discard FFmpeg console output because the preview is written to disk.
            processBuilder.redirectError(
                    ProcessBuilder.Redirect.DISCARD
            );

            processBuilder.redirectOutput(
                    ProcessBuilder.Redirect.DISCARD
            );

            process =
                    processBuilder.start();

            boolean finished =
                    process.waitFor(
                            10,
                            TimeUnit.SECONDS
                    );

            if (!finished) {
                process.destroyForcibly();
                return false;
            }

            return process.exitValue() == 0
                    && Files.exists(outputPath)
                    && Files.size(outputPath) > 0;

        } catch (Exception e) {
            if (process != null) {
                process.destroyForcibly();
            }

            log.warn(
                    "FFmpeg execution error: {}",
                    e.getMessage()
            );

            return false;
        }
    }

    private String findFfmpegExecutable() {
        String environmentPath =
                System.getenv("FFMPEG_PATH");

        if (environmentPath != null
                && !environmentPath.isBlank()) {

            File file =
                    new File(environmentPath);

            if (file.isFile()) {
                return file.getAbsolutePath();
            }
        }

        for (String path : COMMON_FFMPEG_PATHS) {
            try {
                File file =
                        new File(path);

                if (file.isFile()) {
                    return file.getAbsolutePath();
                }

            } catch (Exception ignored) {
            }
        }

        // Dynamically search common Windows WinGet package locations.
        try {
            String localAppData =
                    System.getenv("LOCALAPPDATA");

            if (localAppData != null) {
                File wingetDirectory =
                        new File(
                                localAppData,
                                "Microsoft\\WinGet\\Packages"
                        );

                if (wingetDirectory.isDirectory()) {
                    File[] packages =
                            wingetDirectory.listFiles(
                                    (dir, name) ->
                                            name.toLowerCase(
                                                    Locale.ROOT
                                            ).contains("ffmpeg")
                            );

                    if (packages != null) {
                        for (File packageDirectory : packages) {
                            File[] candidates =
                                    packageDirectory.listFiles(
                                            (dir, name) ->
                                                    name.toLowerCase(
                                                            Locale.ROOT
                                                    ).contains("ffmpeg")
                                                            || name.equalsIgnoreCase("bin")
                                    );

                            if (candidates == null) {
                                continue;
                            }

                            for (File candidate : candidates) {
                                File executable =
                                        new File(
                                                candidate,
                                                "ffmpeg.exe"
                                        );

                                if (executable.isFile()) {
                                    return executable.getAbsolutePath();
                                }

                                File nestedExecutable =
                                        new File(
                                                candidate,
                                                "bin\\ffmpeg.exe"
                                        );

                                if (nestedExecutable.isFile()) {
                                    return nestedExecutable.getAbsolutePath();
                                }
                            }
                        }
                    }
                }
            }

        } catch (Exception ignored) {
        }

        return findFfmpegOnPath();
    }

    private String findFfmpegOnPath() {
        Process process = null;

        try {
            process =
                    new ProcessBuilder(
                            "ffmpeg",
                            "-version"
                    )
                            .redirectErrorStream(true)
                            .redirectOutput(
                                    ProcessBuilder.Redirect.DISCARD
                            )
                            .start();

            boolean finished =
                    process.waitFor(
                            3,
                            TimeUnit.SECONDS
                    );

            if (!finished) {
                process.destroyForcibly();
                return null;
            }

            return process.exitValue() == 0
                    ? "ffmpeg"
                    : null;

        } catch (Exception e) {
            if (process != null) {
                process.destroyForcibly();
            }

            return null;
        }
    }

    private byte[] extractWithExiftool(
            File file,
            String tag) {
        Process process = null;
        java.util.concurrent.CompletableFuture<byte[]> outputFuture = null;

        try {
            ProcessBuilder processBuilder =
                    new ProcessBuilder(
                            "exiftool",
                            "-b",
                            tag,
                            file.getAbsolutePath()
                    );

            // Keep binary preview data on stdout and discard diagnostic stderr output.
            processBuilder.redirectError(
                    ProcessBuilder.Redirect.DISCARD
            );

            process =
                    processBuilder.start();

            Process runningProcess = process;

            outputFuture =
                    java.util.concurrent.CompletableFuture.supplyAsync(() -> {
                        try (InputStream input =
                                     runningProcess.getInputStream();
                             ByteArrayOutputStream output =
                                     new ByteArrayOutputStream()) {

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
                            5,
                            TimeUnit.SECONDS
                    );

            if (!finished) {
                process.destroyForcibly();
                outputFuture.cancel(true);
                return null;
            }

            byte[] output =
                    outputFuture.get(
                            2,
                            TimeUnit.SECONDS
                    );

            if (process.exitValue() == 0 && output != null && output.length > 0) {
                return output;
            }

        } catch (Exception e) {
            if (process != null) {
                process.destroyForcibly();
            }
            if (outputFuture != null) {
                outputFuture.cancel(true);
            }
        }

        return null;
    }

    private String getExtension(String filename) {
        if (filename == null || filename.isBlank()) {
            return "";
        }

        int dot =
                filename.lastIndexOf('.');

        return dot != -1
                ? filename.substring(dot)
                : "";
    }

    private void cleanupTempFile(Path path) {
        if (path != null) {
            try {
                Files.deleteIfExists(path);
            } catch (Exception ignored) {
            }
        }
    }
}