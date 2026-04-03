package com.automation.utils;

import com.automation.driver.DriverManager;
import com.madgag.gif.fmsware.AnimatedGifEncoder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

// Captures browser frames during a test and encodes them as an animated GIF.
// Uses Selenium's TakesScreenshot so capture works when the browser is minimised or headless.
// One recorder per thread — backed by ThreadLocal for parallel-test safety.
public class VideoRecorder {

    private static final Logger log = LogManager.getLogger(VideoRecorder.class);

    private static final ThreadLocal<VideoRecorder> RECORDER = new ThreadLocal<>();

    private static final boolean VIDEO_ENABLED = ConfigReader.getBoolean("video.enabled", true);
    private static final int     FPS            = ConfigReader.getInt("video.fps", 5);
    private static final String  OUTPUT_DIR     = "test-output/videos/";

    private static final DateTimeFormatter TS_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss");

    private final List<BufferedImage>      frames    = new ArrayList<>();
    private final WebDriver                driver;
    private       ScheduledExecutorService scheduler;

    private VideoRecorder(WebDriver driver) {
        this.driver = driver;
    }

    // --- Public API ---

    public static void start() {
        if (!VIDEO_ENABLED) {
            log.debug("Video recording disabled — skipping start");
            return;
        }
        WebDriver driver = DriverManager.getDriver();
        if (driver == null) {
            log.warn("No active WebDriver — cannot start video recording");
            return;
        }
        VideoRecorder recorder = new VideoRecorder(driver);
        RECORDER.set(recorder);
        recorder.startCapture();
        log.info("Video recording started");
    }

    // Stops the recorder for the current thread, encodes a GIF, and returns the file path.
    // Returns null if recording was not active or encoding failed.
    public static String stop(String testName) {
        VideoRecorder recorder = RECORDER.get();
        RECORDER.remove();
        if (recorder == null) {
            return null;
        }
        return recorder.stopAndEncode(testName);
    }

    // --- Private helpers ---

    private void startCapture() {
        long intervalMs = 1000L / Math.max(1, FPS);
        scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "gif-recorder");
            t.setDaemon(true);
            return t;
        });
        scheduler.scheduleAtFixedRate(() -> {
            try {
                // Ask the browser engine directly — works minimised and in headless mode.
                byte[] bytes = ((TakesScreenshot) driver).getScreenshotAs(OutputType.BYTES);
                BufferedImage frame = ImageIO.read(new ByteArrayInputStream(bytes));
                if (frame != null) {
                    BufferedImage scaled = scaleImage(frame, 0.5);
                    synchronized (frames) {
                        frames.add(scaled);
                    }
                }
            } catch (Exception e) {
                log.warn("Frame capture failed: {}", e.getMessage());
            }
        }, 0, intervalMs, TimeUnit.MILLISECONDS);
    }

    private String stopAndEncode(String testName) {
        if (scheduler != null) {
            scheduler.shutdown();
            try {
                scheduler.awaitTermination(5, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        List<BufferedImage> capturedFrames;
        synchronized (frames) {
            capturedFrames = new ArrayList<>(frames);
        }

        if (capturedFrames.isEmpty()) {
            log.warn("No frames captured for test: {}", testName);
            return null;
        }
        return encodeGif(testName, capturedFrames);
    }

    private String encodeGif(String testName, List<BufferedImage> capturedFrames) {
        try {
            Files.createDirectories(Paths.get(OUTPUT_DIR));
        } catch (IOException e) {
            log.error("Failed to create output directory '{}': {}", OUTPUT_DIR, e.getMessage());
            return null;
        }
        String timestamp = LocalDateTime.now().format(TS_FORMAT);
        String safeName  = testName.replaceAll("[^a-zA-Z0-9_-]", "_");
        String filePath  = OUTPUT_DIR + safeName + "_" + timestamp + ".gif";

        try (FileOutputStream fos = new FileOutputStream(filePath)) {
            AnimatedGifEncoder encoder = new AnimatedGifEncoder();
            encoder.start(fos);
            encoder.setDelay(1000 / Math.max(1, FPS));
            encoder.setRepeat(0); // loop forever
            for (BufferedImage frame : capturedFrames) {
                encoder.addFrame(frame);
            }
            encoder.finish();
            log.info("GIF saved: {}", filePath);
            return filePath;
        } catch (IOException e) {
            log.error("Failed to save GIF for test '{}': {}", testName, e.getMessage());
            return null;
        }
    }

    private static BufferedImage scaleImage(BufferedImage original, double scale) {
        int newWidth  = (int) (original.getWidth()  * scale);
        int newHeight = (int) (original.getHeight() * scale);
        BufferedImage scaled = new BufferedImage(newWidth, newHeight, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = scaled.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.drawImage(original, 0, 0, newWidth, newHeight, null);
        g.dispose();
        return scaled;
    }
}
