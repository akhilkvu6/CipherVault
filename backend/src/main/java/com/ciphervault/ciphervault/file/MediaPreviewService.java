package com.ciphervault.ciphervault.file;

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
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
public class MediaPreviewService {

    private static final Logger log = LoggerFactory.getLogger(MediaPreviewService.class);

    private static final int MAX_PREVIEW_DIMENSION = 384;
    private static final List<String> COMMON_FFMPEG_PATHS = Arrays.asList(
            "ffmpeg",
            "ffmpeg.exe",
            System.getProperty("user.home") + "\\AppData\\Local\\Microsoft\\WinGet\\Links\\ffmpeg.exe",
            System.getProperty("user.home") + "\\AppData\\Local\\Microsoft\\WinGet\\Packages\\Gyan.FFmpeg_Microsoft.Winget.Source_8wekyb3d8bbwe\\ffmpeg-9.0.2-full_build\\bin\\ffmpeg.exe",
            "C:\\Program Files\\ffmpeg\\bin\\ffmpeg.exe",
            "C:\\ffmpeg\\bin\\ffmpeg.exe",
            "/usr/bin/ffmpeg",
            "/usr/local/bin/ffmpeg",
            "/opt/homebrew/bin/ffmpeg"
    );

    public byte[] generatePreview(Path sourceFile, String filename, String contentType) {
        if (sourceFile == null || !Files.exists(sourceFile)) {
            return null;
        }

        try {
            if (Files.size(sourceFile) == 0) {
                return null;
            }

            String mime = contentType != null ? contentType.toLowerCase() : "";
            String name = filename != null ? filename.toLowerCase() : "";
            byte[] result = null;
            String type = "IMAGE";
            String generator = "ImageIO";

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
                com.ciphervault.ciphervault.logging.ConsoleLogger.logPreviewUnavailable(filename, "No compatible preview generator");
                return null;
            }

            if (result != null && result.length > 0) {
                com.ciphervault.ciphervault.logging.ConsoleLogger.logPreviewReady(filename, null, type, generator, true, true);
                return result;
            } else {
                com.ciphervault.ciphervault.logging.ConsoleLogger.logPreviewUnavailable(filename, "Frame extraction yielded empty preview");
            }
        } catch (Exception e) {
            log.warn("Preview generation failed for {}: {}", filename, e.getMessage());
            com.ciphervault.ciphervault.logging.ConsoleLogger.logPreviewUnavailable(filename, e.getMessage());
        }

        return null;
    }

    public byte[] generatePreview(byte[] originalData, String filename, String contentType) {
        if (originalData == null || originalData.length == 0) {
            return null;
        }
        Path temp = null;
        try {
            String ext = getExtension(filename);
            temp = Files.createTempFile("cv_data_prev_", ext);
            Files.write(temp, originalData);
            return generatePreview(temp, filename, contentType);
        } catch (Exception e) {
            log.warn("Preview generation error from bytes: {}", e.getMessage());
            return null;
        } finally {
            cleanupTempFile(temp);
        }
    }

    private boolean isImage(String mime, String name) {
        return mime.startsWith("image/")
                || name.endsWith(".jpg") || name.endsWith(".jpeg")
                || name.endsWith(".png") || name.endsWith(".webp")
                || name.endsWith(".gif") || name.endsWith(".bmp");
    }

    private boolean isPdf(String mime, String name) {
        return "application/pdf".equalsIgnoreCase(mime) || name.endsWith(".pdf");
    }

    private boolean isVideo(String mime, String name) {
        return mime.startsWith("video/")
                || name.endsWith(".mp4") || name.endsWith(".mkv")
                || name.endsWith(".avi") || name.endsWith(".mov")
                || name.endsWith(".webm") || name.endsWith(".3gp")
                || name.endsWith(".flv");
    }

    private boolean isAudio(String mime, String name) {
        return mime.startsWith("audio/")
                || name.endsWith(".mp3") || name.endsWith(".m4a")
                || name.endsWith(".flac") || name.endsWith(".aac")
                || name.endsWith(".ogg") || name.endsWith(".wav");
    }

    public byte[] generatePdfPreview(Path sourceFile) {
        try (PDDocument document = Loader.loadPDF(sourceFile.toFile())) {
            if (document.getNumberOfPages() > 0) {
                PDFRenderer renderer = new PDFRenderer(document);
                BufferedImage bim = renderer.renderImageWithDPI(0, 96);
                if (bim != null) {
                    return scaleAndEncodeJpeg(bim);
                }
            }
        } catch (Exception e) {
            log.warn("PDF preview extraction error: {}", e.getMessage());
        }
        return null;
    }

    public byte[] generateImagePreview(Path sourceFile) {
        try {
            BufferedImage original = ImageIO.read(sourceFile.toFile());
            if (original == null) {
                // Try exiftool extraction for embedded thumbnail / preview if standard decoder returned null
                byte[] extracted = extractWithExiftool(sourceFile.toFile(), "-ThumbnailImage");
                if (extracted == null || extracted.length == 0) {
                    extracted = extractWithExiftool(sourceFile.toFile(), "-PreviewImage");
                }
                if (extracted != null && extracted.length > 0) {
                    original = ImageIO.read(new ByteArrayInputStream(extracted));
                }
            }

            if (original == null) {
                return null;
            }

            return scaleAndEncodeJpeg(original);
        } catch (Exception e) {
            log.warn("Image preview processing error: {}", e.getMessage());
            return null;
        }
    }

    public byte[] generateImagePreview(byte[] originalData) {
        if (originalData == null || originalData.length == 0) return null;
        Path temp = null;
        try {
            temp = Files.createTempFile("cv_img_prev_", ".tmp");
            Files.write(temp, originalData);
            return generateImagePreview(temp);
        } catch (Exception e) {
            return null;
        } finally {
            cleanupTempFile(temp);
        }
    }

    private byte[] generateVideoPreview(Path sourceFile) {
        Path tempOutput = null;

        try {
            tempOutput = Files.createTempFile("cv_vid_out_", ".jpg");

            // 1. Primary: FFmpeg (supports H.264, H.265/HEVC, VP8/VP9/WebM, MKV, AVI, MP4, etc.)
            String ffmpegPath = findFfmpegExecutable();
            if (ffmpegPath != null) {
                boolean success = runFfmpegFrameExtract(ffmpegPath, sourceFile, tempOutput, "00:00:01");
                if (!success || Files.size(tempOutput) == 0) {
                    success = runFfmpegFrameExtract(ffmpegPath, sourceFile, tempOutput, "00:00:00");
                }

                if (success && Files.exists(tempOutput) && Files.size(tempOutput) > 0) {
                    BufferedImage frame = ImageIO.read(tempOutput.toFile());
                    if (frame != null) {
                        log.debug("Video preview generated via FFmpeg: {}", sourceFile.getFileName());
                        return scaleAndEncodeJpeg(frame);
                    }
                }
            }

            // 2. Pure-Java Fallback: JCodec (decodes MP4/MOV frames without external binaries)
            byte[] jcodecResult = extractFrameWithJCodec(sourceFile);
            if (jcodecResult != null && jcodecResult.length > 0) {
                log.debug("Video preview generated via JCodec fallback: {}", sourceFile.getFileName());
                return jcodecResult;
            }

            // 3. Metadata Fallback: Check if ExifTool can extract embedded CoverArt / PreviewImage directly
            byte[] cover = extractWithExiftool(sourceFile.toFile(), "-CoverArt");
            if (cover == null || cover.length == 0) {
                cover = extractWithExiftool(sourceFile.toFile(), "-PreviewImage");
            }
            if (cover != null && cover.length > 0) {
                BufferedImage bi = ImageIO.read(new ByteArrayInputStream(cover));
                if (bi != null) {
                    return scaleAndEncodeJpeg(bi);
                }
            }

        } catch (Exception e) {
            log.warn("Video preview frame extraction failed: {}", e.getMessage());
        } finally {
            cleanupTempFile(tempOutput);
        }

        return null;
    }

    private byte[] extractFrameWithJCodec(Path sourceFile) {
        try {
            File f = sourceFile.toFile();
            Picture picture = null;
            try {
                picture = FrameGrab.getFrameFromFile(f, 1);
            } catch (Exception ignored) {}
            if (picture == null) {
                try {
                    picture = FrameGrab.getFrameFromFile(f, 0);
                } catch (Exception ignored) {}
            }
            if (picture != null) {
                BufferedImage bi = AWTUtil.toBufferedImage(picture);
                if (bi != null) {
                    return scaleAndEncodeJpeg(bi);
                }
            }
        } catch (Throwable t) {
            log.debug("JCodec frame extraction skipped: {}", t.getMessage());
        }
        return null;
    }

    private byte[] generateAudioPreview(Path sourceFile) {
        try {
            byte[] art = extractWithExiftool(sourceFile.toFile(), "-Picture");
            if (art == null || art.length == 0) {
                art = extractWithExiftool(sourceFile.toFile(), "-CoverArt");
            }

            if (art != null && art.length > 0) {
                BufferedImage bi = ImageIO.read(new ByteArrayInputStream(art));
                if (bi != null) {
                    return scaleAndEncodeJpeg(bi);
                }
            }
        } catch (Exception e) {
            log.warn("Audio artwork extraction error: {}", e.getMessage());
        }

        return null;
    }

    private byte[] scaleAndEncodeJpeg(BufferedImage original) throws Exception {
        int origW = original.getWidth();
        int origH = original.getHeight();

        int targetW = origW;
        int targetH = origH;

        if (origW > MAX_PREVIEW_DIMENSION || origH > MAX_PREVIEW_DIMENSION) {
            if (origW >= origH) {
                targetW = MAX_PREVIEW_DIMENSION;
                targetH = Math.max(1, (origH * MAX_PREVIEW_DIMENSION) / origW);
            } else {
                targetH = MAX_PREVIEW_DIMENSION;
                targetW = Math.max(1, (origW * MAX_PREVIEW_DIMENSION) / origH);
            }
        }

        BufferedImage resized = new BufferedImage(targetW, targetH, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = resized.createGraphics();
        try {
            g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            g2d.setColor(Color.WHITE);
            g2d.fillRect(0, 0, targetW, targetH);
            g2d.drawImage(original, 0, 0, targetW, targetH, null);
        } finally {
            g2d.dispose();
        }

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(resized, "jpg", baos);
        return baos.toByteArray();
    }

    private boolean runFfmpegFrameExtract(String ffmpegPath, Path inputPath, Path outputPath, String seekTime) {
        try {
            ProcessBuilder pb = new ProcessBuilder(
                    ffmpegPath,
                    "-y",
                    "-loglevel", "error",
                    "-ss", seekTime,
                    "-i", inputPath.toAbsolutePath().toString(),
                    "-vframes", "1",
                    "-vf", "scale='min(" + MAX_PREVIEW_DIMENSION + ",iw)':-2",
                    "-f", "image2",
                    outputPath.toAbsolutePath().toString()
            );
            pb.redirectErrorStream(true);
            Process process = pb.start();

            // Crucial: Drain stdout/stderr so child process never blocks on OS pipe buffer
            try (InputStream is = process.getInputStream()) {
                byte[] buffer = new byte[4096];
                while (is.read(buffer) != -1) {}
            }

            boolean finished = process.waitFor(10, TimeUnit.SECONDS);
            if (!finished) {
                process.destroyForcibly();
                return false;
            }
            return process.exitValue() == 0 && Files.exists(outputPath) && Files.size(outputPath) > 0;
        } catch (Exception e) {
            log.warn("FFmpeg execution error: {}", e.getMessage());
            return false;
        }
    }

    private String findFfmpegExecutable() {
        String envFfmpeg = System.getenv("FFMPEG_PATH");
        if (envFfmpeg != null && new File(envFfmpeg).exists()) {
            return envFfmpeg;
        }

        for (String path : COMMON_FFMPEG_PATHS) {
            try {
                File f = new File(path);
                if (f.exists() && f.isFile()) {
                    return f.getAbsolutePath();
                }
            } catch (Exception ignored) {}
        }

        // Dynamically search in user LocalAppData WinGet Packages
        try {
            String localAppData = System.getenv("LOCALAPPDATA");
            if (localAppData != null) {
                File wingetDir = new File(localAppData, "Microsoft\\WinGet\\Packages");
                if (wingetDir.exists() && wingetDir.isDirectory()) {
                    File[] pkgs = wingetDir.listFiles((dir, name) -> name.toLowerCase().contains("ffmpeg"));
                    if (pkgs != null) {
                        for (File pkg : pkgs) {
                            File[] binFolders = pkg.listFiles((dir, name) -> name.toLowerCase().contains("ffmpeg") || name.equalsIgnoreCase("bin"));
                            if (binFolders != null) {
                                for (File bf : binFolders) {
                                    File cand = new File(bf, "ffmpeg.exe");
                                    if (cand.exists() && cand.isFile()) return cand.getAbsolutePath();
                                    File subBin = new File(bf, "bin\\ffmpeg.exe");
                                    if (subBin.exists() && subBin.isFile()) return subBin.getAbsolutePath();
                                }
                            }
                        }
                    }
                }
            }
        } catch (Exception ignored) {}

        for (String cmd : Arrays.asList("ffmpeg", "ffmpeg.exe")) {
            try {
                Process process = new ProcessBuilder(cmd, "-version").start();
                try (InputStream is = process.getInputStream()) {
                    byte[] b = new byte[1024];
                    while (is.read(b) != -1) {}
                }
                if (process.waitFor(3, TimeUnit.SECONDS) && process.exitValue() == 0) {
                    return cmd;
                }
            } catch (Exception ignored) {}
        }
        return null;
    }

    private byte[] extractWithExiftool(File file, String tag) {
        try {
            ProcessBuilder pb = new ProcessBuilder(
                    "exiftool",
                    "-b",
                    tag,
                    file.getAbsolutePath()
            );
            Process process = pb.start();
            try (InputStream is = process.getInputStream()) {
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                byte[] buf = new byte[4096];
                int n;
                while ((n = is.read(buf)) != -1) {
                    baos.write(buf, 0, n);
                }
                boolean finished = process.waitFor(5, TimeUnit.SECONDS);
                if (finished && process.exitValue() == 0 && baos.size() > 0) {
                    return baos.toByteArray();
                }
            }
        } catch (Exception ignored) {}
        return null;
    }

    private String getExtension(String filename) {
        int dot = filename.lastIndexOf('.');
        return dot != -1 ? filename.substring(dot) : "";
    }

    private void cleanupTempFile(Path path) {
        if (path != null) {
            try {
                Files.deleteIfExists(path);
            } catch (Exception ignored) {}
        }
    }
}
