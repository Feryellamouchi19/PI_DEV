package services;

import java.awt.*;
import java.awt.font.FontRenderContext;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.Duration;
import java.util.List;
import javax.imageio.ImageIO;

/**
 * ImageAiService - Génération d'affiches IA.
 * API principale: FluxImageGen (5 img/jour gratuites, sans clé, style photorealism).
 * Fallback: affiche locale si l'API échoue.
 *
 * Output folder: uploads/images/
 */
public class ImageAiService {

    // --------- FluxImageGen (gratuit, sans clé, 5/jour par IP) ----------
    // POST JSON, retourne imageUrl. Style photorealism pour affiches réalistes.
    private static final String FLUX_API = "https://fluximagegen.com/api/generate";

    private static final Path UPLOAD_DIR = Path.of("uploads/images");

    private final HttpClient http = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(30))
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
        return generateSaveAndGet(prompt, baseName, null, null, null, null).fileName;
    }

    /**
     * Génère via API IA, enregistre dans uploads/images/.
     * Si l'API échoue -> fallback affiche locale enrichie.
     */
    public GeneratedImage generateSaveAndGet(String prompt, String baseName,
            String titre, String description, String type, String lieu) throws Exception {
        Files.createDirectories(UPLOAD_DIR);

        String safeBase = sanitizeBase(baseName);
        String fileName = safeBase + "_" + System.currentTimeMillis() + ".png";
        Path out = UPLOAD_DIR.resolve(fileName);

        try {
            byte[] bytes = generateWithApi(prompt);
            byte[] pngBytes = ensurePng(bytes);
            Files.write(out, pngBytes, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
            return new GeneratedImage(fileName, out, "image/png", pngBytes);
        } catch (Exception apiEx) {
            byte[] fallback = generateLocalPosterPng(safeBase, titre, description, type, lieu);
            Files.write(out, fallback, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
            return new GeneratedImage(fileName, out, "image/png", fallback);
        }
    }

    // ===================== APIs IA =====================

    private byte[] generateWithApi(String prompt) throws Exception {
        // 1) Hugging Face si token configuré (gratuit, ~300 req/h)
        String hfToken = getHuggingFaceToken();
        if (hfToken != null && !hfToken.isBlank()) {
            byte[] img = tryHuggingFace(prompt, hfToken);
            if (img != null) return img;
        }
        // 2) FluxImageGen (5/jour sans clé)
        return generateWithFluxImageGen(prompt);
    }

    private static String getHuggingFaceToken() {
        String t = System.getenv("HF_TOKEN");
        if (t != null && !t.isBlank()) return t.trim();
        t = System.getenv("HUGGINGFACE_TOKEN");
        if (t != null && !t.isBlank()) return t.trim();
        try {
            Path p = Path.of("hf_token.txt");
            if (Files.exists(p)) {
                t = Files.readString(p).trim();
                if (!t.isBlank()) return t;
            }
        } catch (Exception ignored) {}
        return null;
    }

    private byte[] tryHuggingFace(String prompt, String token) {
        try {
            String json = "{\"inputs\":\"" + escapeJson(prompt.trim()) + "\"}";
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create("https://api-inference.huggingface.co/models/black-forest-labs/FLUX.1-schnell"))
                    .timeout(Duration.ofSeconds(90))
                    .header("Authorization", "Bearer " + token)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(json, StandardCharsets.UTF_8))
                    .build();
            HttpResponse<byte[]> res = http.send(req, HttpResponse.BodyHandlers.ofByteArray());
            if (res.statusCode() != 200) return null;
            byte[] body = res.body();
            if (body == null || body.length < 1000) return null;
            String ct = res.headers().firstValue("content-type").orElse("");
            if (!ct.startsWith("image/")) return null;
            return body;
        } catch (Exception e) {
            return null;
        }
    }

    private byte[] generateWithFluxImageGen(String prompt) throws Exception {
        if (prompt == null || prompt.trim().isEmpty()) {
            throw new IllegalArgumentException("Prompt vide.");
        }

        String jsonBody = "{\"prompt\":\"" + escapeJson(prompt.trim()) + "\",\"style\":\"photorealism\"}";

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(FLUX_API))
                .timeout(Duration.ofSeconds(90))
                .header("Content-Type", "application/json")
                .header("User-Agent", "PI_DEV-Events/1.0")
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody, StandardCharsets.UTF_8))
                .build();

        HttpResponse<String> res = http.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        int code = res.statusCode();

        if (code == 429) {
            throw new RuntimeException("Limite quotidienne atteinte (5 images/jour). Réessayez demain.");
        }
        if (code < 200 || code >= 300) {
            throw new RuntimeException("API image: HTTP " + code);
        }

        String body = res.body();
        String imageUrl = extractJsonString(body, "imageUrl");
        if (imageUrl == null || imageUrl.isBlank()) {
            String err = extractJsonString(body, "error");
            throw new RuntimeException(err != null ? err : "Réponse API invalide (pas d'imageUrl)");
        }

        // Télécharger l'image depuis imageUrl
        return fetchImageFromUrl(imageUrl);
    }

    private static String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    private static String extractJsonString(String json, String key) {
        if (json == null) return null;
        String search = "\"" + key + "\"";
        int i = json.indexOf(search);
        if (i < 0) return null;
        i = json.indexOf(":", i);
        if (i < 0) return null;
        int q1 = json.indexOf("\"", i);
        if (q1 < 0) return null;
        int q2 = json.indexOf("\"", q1 + 1);
        while (q2 > 0 && json.charAt(q2 - 1) == '\\') {
            q2 = json.indexOf("\"", q2 + 1);
        }
        if (q2 < 0) return null;
        return json.substring(q1 + 1, q2).replace("\\\"", "\"").replace("\\\\", "\\");
    }

    private byte[] fetchImageFromUrl(String imageUrl) throws Exception {
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(imageUrl))
                .timeout(Duration.ofSeconds(60))
                .header("User-Agent", "PI_DEV-Events/1.0")
                .GET()
                .build();

        HttpResponse<byte[]> res = http.send(req, HttpResponse.BodyHandlers.ofByteArray());
        if (res.statusCode() < 200 || res.statusCode() >= 300) {
            throw new RuntimeException("Erreur téléchargement image: HTTP " + res.statusCode());
        }
        byte[] body = res.body();
        if (body == null || body.length < 500) {
            throw new RuntimeException("Image téléchargée invalide.");
        }
        return body;
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
     * Fallback: affiche événement style professionnel avec titre, description, type, lieu.
     */
    private static byte[] generateLocalPosterPng(String baseName, String titre, String description, String type, String lieu) throws Exception {
        int w = 768, h = 1024; // Format affiche verticale
        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();

        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

        // Fond: dégradé sombre (style affiche concert)
        GradientPaint bg = new GradientPaint(0, 0, new Color(12, 12, 28), 0, h, new Color(35, 20, 55));
        g.setPaint(bg);
        g.fillRect(0, 0, w, h);

        // Bande colorée en haut (accent)
        g.setColor(new Color(220, 80, 120));
        g.fillRect(0, 0, w, 12);

        // Zone "visuelle" simulée (rectangle style photo)
        g.setColor(new Color(40, 35, 60));
        g.fillRoundRect(40, 80, w - 80, 380, 16, 16);
        g.setColor(new Color(60, 50, 90));
        g.setStroke(new BasicStroke(2f));
        g.drawRoundRect(40, 80, w - 80, 380, 16, 16);

        // Icônes/forme décorative dans la zone visuelle
        g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.15f));
        g.setColor(new Color(255, 200, 100));
        g.fillOval(w/2 - 80, 200, 160, 160);
        g.setComposite(AlphaComposite.SrcOver);

        // Titre principal (gros, impactant)
        String bigTitle = (titre == null || titre.isBlank()) ? "Événement" : titre.trim();
        bigTitle = truncate(bigTitle, 25);
        Font titleFont = new Font("Segoe UI", Font.BOLD, 56);
        g.setFont(titleFont);
        g.setColor(Color.WHITE);

        FontRenderContext frc = g.getFontRenderContext();
        java.awt.geom.Rectangle2D titleBounds = titleFont.getStringBounds(bigTitle, frc);
        int tx = (int) ((w - titleBounds.getWidth()) / 2);
        int ty = 520;
        g.drawString(bigTitle, tx, ty);

        // Badge type
        String typeStr = (type == null || type.isBlank()) ? "" : type.toUpperCase();
        if (!typeStr.isBlank()) {
            g.setFont(new Font("Segoe UI", Font.BOLD, 14));
            int tw = (int) g.getFontMetrics().getStringBounds(typeStr, g).getWidth();
            int bx = (w - tw - 24) / 2;
            g.setColor(new Color(220, 80, 120, 200));
            g.fillRoundRect(bx, ty + 20, tw + 24, 32, 16, 16);
            g.setColor(Color.WHITE);
            g.drawString(typeStr, bx + 12, ty + 42);
        }

        // Description (2-3 lignes max)
        String desc = (description == null || description.isBlank()) ? "" : description.trim();
        if (!desc.isBlank()) {
            desc = desc.length() > 120 ? desc.substring(0, 117) + "..." : desc;
            g.setFont(new Font("Segoe UI", Font.PLAIN, 18));
            g.setColor(new Color(220, 220, 230));
            List<String> lines = wrapText(desc, g.getFont(), w - 80);
            int ly = ty + 90;
            for (int i = 0; i < Math.min(3, lines.size()); i++) {
                String line = lines.get(i);
                int lx = (w - (int) g.getFontMetrics().getStringBounds(line, g).getWidth()) / 2;
                g.drawString(line, lx, ly);
                ly += 26;
            }
        }

        // Lieu en bas
        if (lieu != null && !lieu.isBlank()) {
            g.setFont(new Font("Segoe UI", Font.PLAIN, 16));
            g.setColor(new Color(180, 180, 200));
            String loc = "📍 " + truncate(lieu.trim(), 35);
            int lw = (int) g.getFontMetrics().getStringBounds(loc, g).getWidth();
            g.drawString(loc, (w - lw) / 2, h - 50);
        }

        // Ligne "Affiche générée" discrète
        g.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        g.setColor(new Color(120, 120, 140));
        g.drawString("Affiche événement", w - 120, h - 20);

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