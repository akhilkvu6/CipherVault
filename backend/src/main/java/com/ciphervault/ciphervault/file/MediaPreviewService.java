package com.ciphervault.ciphervault.file;

import com.ciphervault.ciphervault.util.ConsoleLogger;
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

    private static final int MAX_PREVIEW_DIMENSION = 384;
    private static final List<String> COMMON_FFMPEG_PATHS = Arrays.asList(
            "ffmpeg",
            "C:\\Program Files\\ShareX\\ffmpeg.exe",
            "C:\\Program Files\\Krita (x64)\\bin\\ffmpeg.exe"
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

            if (isImage(mime, name)) {
                return generateImagePreview(sourceFile);
            } else if (isVideo(mime, name)) {
                return generateVideoPreview(sourceFile);
            } else if (isAudio(mime, name)) {
                return generateAudioPreview(sourceFile);
            }
        } catch (Exception e) {
            ConsoleLogger.warn("Preview generation failed for " + filename + ": " + e.getMessage());
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
            ConsoleLogger.warn("Preview generation error from bytes: " + e.getMessage());
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
            ConsoleLogger.warn("Image preview processing error: " + e.getMessage());
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

            String ffmpegPath = findFfmpegExecutable();
            if (ffmpegPath != null) {
                // Try 1 second mark first directly from the source file
                boolean success = runFfmpegFrameExtract(ffmpegPath, sourceFile, tempOutput, "00:00:01");
                if (!success || Files.size(tempOutput) == 0) {
                    // Try 0 second mark
                    runFfmpegFrameExtract(ffmpegPath, sourceFile, tempOutput, "00:00:00");
                }

                if (Files.exists(tempOutput) && Files.size(tempOutput) > 0) {
                    BufferedImage frame = ImageIO.read(tempOutput.toFile());
                    if (frame != null) {
                        return scaleAndEncodeJpeg(frame);
                    }
                }
            }

            // Fallback: Check if ExifTool can extract embedded CoverArt / PreviewImage directly from source file
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
            ConsoleLogger.warn("Video preview frame extraction failed: " + e.getMessage());
        } finally {
            cleanupTempFile(tempOutput);
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
            ConsoleLogger.warn("Audio artwork extraction error: " + e.getMessage());
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
                    "-ss", seekTime,
                    "-i", inputPath.toAbsolutePath().toString(),
                    "-vframes", "1",
                    "-vf", "scale='min(" + MAX_PREVIEW_DIMENSION + ",iw)':-2",
                    "-f", "image2",
                    outputPath.toAbsolutePath().toString()
            );
            pb.redirectErrorStream(true);
            Process process = pb.start();
            boolean finished = process.waitFor(10, TimeUnit.SECONDS);
            if (!finished) {
                process.destroyForcibly();
                return false;
            }
            return process.exitValue() == 0;
        } catch (Exception e) {
            return false;
        }
    }

    private String findFfmpegExecutable() {
        String envFfmpeg = System.getenv("FFMPEG_PATH");
        if (envFfmpeg != null && new File(envFfmpeg).exists()) {
            return envFfmpeg;
        }

        for (String path : COMMON_FFMPEG_PATHS) {
            if ("ffmpeg".equals(path)) {
                try {
                    Process process = new ProcessBuilder("ffmpeg", "-version").start();
                    if (process.waitFor(3, TimeUnit.SECONDS) && process.exitValue() == 0) {
                        return "ffmpeg";
                    }
                } catch (Exception ignored) {}
            } else {
                File f = new File(path);
                if (f.exists() && f.canExecute()) {
                    return path;
                }
            }
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
