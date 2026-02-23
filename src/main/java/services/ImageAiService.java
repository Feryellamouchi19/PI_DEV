package services;

import java.awt.*;
import java.awt.font.FontRenderContext;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.Duration;
import java.util.List;
import java.util.Locale;
import javax.imageio.ImageIO;

/**
 * ImageAiService (FREE) using Pollinations Text-to-Image.
 * - No API key needed.
 * - Robust: retries + fallback local poster generation if API fails.
 *
 * Output folder: uploads/images/
 * Returned value: file name to store in DB.
 */
public class ImageAiService {

    // --------- Pollinations endpoint (FREE) ----------
    // It returns image bytes directly.
    // Example:
    // https://image.pollinations.ai/prompt/your%20prompt?width=1024&height=1024&seed=123&nologo=true
    private static final String POLLINATIONS_BASE = "https://image.pollinations.ai/prompt/";

    private static final Path UPLOAD_DIR = Path.of("uploads/images");

    private final HttpClient http = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(20))
            .build();

    /** Result object for UI + DB */
    public static class GeneratedImage {
        public final String fileName;   // store in DB
        public final Path path;         // full output file
        public final String mimeType;   // image/png, image/jpeg...
        public final byte[] bytes;      // for preview

        public GeneratedImage(String fileName, Path path, String mimeType, byte[] bytes) {
            this.fileName = fileName;
            this.path = path;
            this.mimeType = mimeType;
            this.bytes = bytes;
        }
    }

    /** DB-friendly method (returns just file name) */
    public String generateAndSave(String prompt, String baseName) throws Exception {
        return generateSaveAndGet(prompt, baseName).fileName;
    }

    /**
     * Generates with Pollinations, saves to uploads/images/,
     * returns bytes for preview.
     * If Pollinations fails -> fallback local generated poster.
     */
    public GeneratedImage generateSaveAndGet(String prompt, String baseName) throws Exception {
        Files.createDirectories(UPLOAD_DIR);

        String safeBase = sanitizeBase(baseName);
        String fileName = safeBase + "_" + System.currentTimeMillis() + ".png";
        Path out = UPLOAD_DIR.resolve(fileName);

        // 1) Try Pollinations with retries
        try {
            byte[] bytes = generateWithPollinations(prompt);
            // Convert to PNG to be consistent
            byte[] pngBytes = ensurePng(bytes);

            Files.write(out, pngBytes, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
            return new GeneratedImage(fileName, out, "image/png", pngBytes);

        } catch (Exception apiEx) {
            // 2) Fallback local image
            byte[] fallback = generateLocalPosterPng(prompt, safeBase);
            Files.write(out, fallback, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
            return new GeneratedImage(fileName, out, "image/png", fallback);
        }
    }

    // ===================== Pollinations call =====================

    private byte[] generateWithPollinations(String prompt) throws Exception {
        if (prompt == null || prompt.trim().isEmpty()) {
            throw new IllegalArgumentException("Prompt vide.");
        }

        String enc = URLEncoder.encode(prompt.trim(), StandardCharsets.UTF_8);
        int seed = (int) (System.currentTimeMillis() % Integer.MAX_VALUE);

        // You can tweak width/height. 768 is faster.
        String url = POLLINATIONS_BASE + enc
                + "?width=768&height=768"
                + "&seed=" + seed
                + "&nologo=true";

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(90))
                .header("Accept", "image/*")
                .GET()
                .build();

        // Retries: 3 attempts with small backoff
        Exception last = null;
        for (int attempt = 1; attempt <= 3; attempt++) {
            try {
                HttpResponse<byte[]> res = http.send(req, HttpResponse.BodyHandlers.ofByteArray());
                int code = res.statusCode();

                if (code < 200 || code >= 300) {
                    String shortMsg = "HTTP " + code;
                    throw new RuntimeException("Erreur Pollinations (" + code + "): " + shortMsg);
                }

                // Check Content-Type (must be image)
                String ct = res.headers().firstValue("content-type").orElse("").toLowerCase(Locale.ROOT);
                if (!ct.startsWith("image/")) {
                    throw new RuntimeException("Réponse non-image (Content-Type=" + ct + ")");
                }

                byte[] body = res.body();
                if (body == null || body.length < 1000) {
                    throw new RuntimeException("Image trop petite / invalide.");
                }

                return body;

            } catch (Exception ex) {
                last = ex;
                // Backoff
                Thread.sleep(600L * attempt);
            }
        }
        throw last;
    }

    // ===================== Helpers =====================

    private static String sanitizeBase(String baseName) {
        String safeBase = (baseName == null ? "event" : baseName.trim())
                .replaceAll("[^a-zA-Z0-9-_]", "_");
        if (safeBase.isBlank()) safeBase = "event";
        return safeBase;
    }

    /**
     * Converts any image bytes (jpeg/webp/png) to PNG bytes for saving.
     */
    private static byte[] ensurePng(byte[] imageBytes) throws Exception {
        BufferedImage img = ImageIO.read(new ByteArrayInputStream(imageBytes));
        if (img == null) {
            throw new RuntimeException("Impossible de décoder l'image retournée.");
        }

        // Force RGB (safer)
        BufferedImage rgb = new BufferedImage(img.getWidth(), img.getHeight(), BufferedImage.TYPE_INT_RGB);
        Graphics2D g = rgb.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.drawImage(img, 0, 0, null);
        g.dispose();

        // Write as PNG
        java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
        ImageIO.write(rgb, "png", baos);
        return baos.toByteArray();
    }

    /**
     * Fallback local poster generator (always works).
     * Creates a clean poster with title + short prompt text.
     */
    private static byte[] generateLocalPosterPng(String prompt, String title) throws Exception {
        int w = 768, h = 768;
        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();

        // Quality
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        // Background gradient
        GradientPaint gp = new GradientPaint(0, 0, new Color(20, 24, 33), w, h, new Color(80, 35, 65));
        g.setPaint(gp);
        g.fillRect(0, 0, w, h);

        // Overlay card
        int pad = 48;
        int cardX = pad;
        int cardY = pad;
        int cardW = w - pad * 2;
        int cardH = h - pad * 2;

        g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.85f));
        g.setColor(new Color(10, 12, 18));
        g.fill(new RoundRectangle2D.Double(cardX, cardY, cardW, cardH, 36, 36));

        g.setComposite(AlphaComposite.SrcOver);

        // Title
        String bigTitle = (title == null || title.isBlank()) ? "Event" : title;
        Font titleFont = new Font("Segoe UI", Font.BOLD, 44);
        g.setFont(titleFont);
        g.setColor(Color.WHITE);

        int tx = cardX + 36;
        int ty = cardY + 80;
        g.drawString(truncate(bigTitle, 26), tx, ty);

        // Subtitle
        g.setFont(new Font("Segoe UI", Font.PLAIN, 18));
        g.setColor(new Color(220, 220, 220));
        g.drawString("Generated locally (API unavailable)", tx, ty + 34);

        // Prompt text box
        int boxY = ty + 70;
        int boxH = cardH - (boxY - cardY) - 40;

        g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.15f));
        g.setColor(Color.WHITE);
        g.fillRoundRect(tx, boxY, cardW - 72, boxH, 24, 24);
        g.setComposite(AlphaComposite.SrcOver);

        g.setColor(new Color(240, 240, 240));
        g.setFont(new Font("Segoe UI", Font.PLAIN, 20));

        List<String> lines = wrapText(prompt, g.getFont(), cardW - 120);
        int ly = boxY + 44;
        for (String line : lines) {
            if (ly > boxY + boxH - 24) break;
            g.drawString(line, tx + 18, ly);
            ly += 28;
        }

        g.dispose();

        java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
        ImageIO.write(img, "png", baos);
        return baos.toByteArray();
    }

    private static String truncate(String s, int max) {
        if (s == null) return "";
        s = s.trim();
        if (s.length() <= max) return s;
        return s.substring(0, max - 1) + "…";
    }

    private static List<String> wrapText(String text, Font font, int maxWidth) {
        if (text == null) text = "";
        text = text.trim();
        if (text.isEmpty()) return List.of("(no details)");

        FontRenderContext frc = new FontRenderContext(null, true, true);
        String[] words = text.replace("\r", "").replace("\n", " ").split("\\s+");

        java.util.ArrayList<String> lines = new java.util.ArrayList<>();
        StringBuilder current = new StringBuilder();

        for (String w : words) {
            String next = current.isEmpty() ? w : (current + " " + w);
            double width = font.getStringBounds(next, frc).getWidth();
            if (width <= maxWidth) {
                current.setLength(0);
                current.append(next);
            } else {
                if (!current.isEmpty()) lines.add(current.toString());
                current.setLength(0);
                current.append(w);
            }
        }
        if (!current.isEmpty()) lines.add(current.toString());
        return lines;
    }
}