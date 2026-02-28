package services;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.Duration;
import java.util.Locale;

public class EventImageApi {

    private final UnsplashImageService unsplash = new UnsplashImageService();

    private final HttpClient http = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(15))
            .followRedirects(HttpClient.Redirect.ALWAYS)
            .build();

    private static final Path UPLOAD_DIR =
            Paths.get(System.getProperty("user.dir"), "uploads", "images");

    public ImageAiService.GeneratedImage generateForEvent(
            String titre,
            String description,
            String type,
            String lieu
    ) throws Exception {

        Files.createDirectories(UPLOAD_DIR);

        // ✅ Prompt court (évite URL trop longue)
        String prompt = buildShortPrompt(titre, description, type, lieu);

        // 1) ✅ IA (Pollinations)
        try {
            byte[] bytes = generateWithPollinations(prompt);

            String baseName = safeFileBase(titre);
            String fileName = baseName + "_" + System.currentTimeMillis() + ".png";
            Path target = UPLOAD_DIR.resolve(fileName);
            Files.write(target, bytes, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);

            String sourceUrl = buildPollinationsUrl(prompt);
            return new ImageAiService.GeneratedImage(fileName, target, sourceUrl, bytes);

        } catch (Exception e1) {
            // 2) ✅ Retry Pollinations avec prompt ultra simple
            try {
                String simplePrompt = buildUltraSimplePrompt(titre, type);
                byte[] bytes = generateWithPollinations(simplePrompt);

                String baseName = safeFileBase(titre);
                String fileName = baseName + "_" + System.currentTimeMillis() + ".png";
                Path target = UPLOAD_DIR.resolve(fileName);
                Files.write(target, bytes, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);

                String sourceUrl = buildPollinationsUrl(simplePrompt);
                return new ImageAiService.GeneratedImage(fileName, target, sourceUrl, bytes);

            } catch (Exception e2) {
                // 3) ✅ Unsplash fallback (ne doit jamais crasher)
                return fallbackUnsplashOrDefault(titre, description, type, lieu);
            }
        }
    }

    // ============================================================
    // ✅ Pollinations
    // ============================================================

    private byte[] generateWithPollinations(String prompt) throws Exception {
        String url = buildPollinationsUrl(prompt);

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(120))
                .header("User-Agent", "JavaFX")
                .header("Accept", "image/*")
                .GET()
                .build();

        HttpResponse<InputStream> resp = http.send(req, HttpResponse.BodyHandlers.ofInputStream());

        if (resp.statusCode() != 200) {
            throw new RuntimeException("Pollinations HTTP " + resp.statusCode());
        }

        byte[] bytes = readAll(resp.body());
        if (bytes == null || bytes.length < 5000) {
            throw new RuntimeException("Pollinations returned too small image/empty.");
        }

        return bytes;
    }

    private String buildPollinationsUrl(String prompt) {
        String encoded = URLEncoder.encode(prompt, StandardCharsets.UTF_8);

        // ✅ garde taille raisonnable
        return "https://image.pollinations.ai/prompt/" + encoded
                + "?width=1024&height=768&nologo=true&seed=" + System.currentTimeMillis();
    }

    private static byte[] readAll(InputStream in) throws Exception {
        try (in) {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] buf = new byte[8192];
            int r;
            while ((r = in.read(buf)) != -1) out.write(buf, 0, r);
            return out.toByteArray();
        }
    }

    // ============================================================
    // ✅ Prompt (court)
    // ============================================================

    private static String buildShortPrompt(String titre, String desc, String type, String lieu) {
        String t = safe(type).toUpperCase(Locale.ROOT);

        // ✅ important: court, mais basé sur titre + description
        String scene = extractScene(desc);

        StringBuilder sb = new StringBuilder();
        sb.append("High quality event poster illustration, cinematic, detailed. ");
        sb.append("Theme: ").append(typeTheme(t)).append(". ");
        if (!safe(titre).isBlank()) sb.append("Subject: ").append(safe(titre)).append(". ");
        if (!scene.isBlank()) sb.append("Scene: ").append(scene).append(". ");
        if (!safe(lieu).isBlank()) sb.append("Location: ").append(safe(lieu)).append(". ");
        sb.append("No text, no letters, no logos.");

        return sb.toString();
    }

    private static String buildUltraSimplePrompt(String titre, String type) {
        String t = safe(type).toUpperCase(Locale.ROOT);
        String main = safe(titre);
        if (main.isBlank()) main = "event";
        return "Beautiful poster illustration, " + typeTheme(t) + ", subject " + main + ", no text.";
    }

    private static String typeTheme(String typeUpper) {
        return switch (typeUpper) {
            case "SOIREE" -> "night party, neon lights, music";
            case "RANDONNEE" -> "mountains hiking, nature adventure";
            case "CAMPING" -> "campfire, tent, forest, stars";
            case "SEJOUR" -> "travel, vacation, scenic destination";
            default -> "celebration, gathering";
        };
    }

    private static String extractScene(String desc) {
        String s = safe(desc).replaceAll("\\s+", " ");
        if (s.length() <= 120) return s;
        int cut = s.indexOf('.');
        if (cut > 0 && cut < 140) return s.substring(0, cut).trim();
        return s.substring(0, 120).trim();
    }

    // ============================================================
    // ✅ Unsplash fallback sans crash
    // ============================================================

    private ImageAiService.GeneratedImage fallbackUnsplashOrDefault(
            String titre, String desc, String type, String lieu
    ) throws Exception {

        String baseName = safeFileBase(titre);

        // ✅ queries de secours (du plus riche au plus simple)
        String[] queries = new String[] {
                buildUnsplashQuery(titre, desc, type, lieu),
                safe(titre),
                safe(type),
                "event",
                "party",
                "nature"
        };

        for (String q : queries) {
            if (q == null || q.isBlank()) continue;

            try {
                String imageUrl = unsplash.getRandomImageUrl(q);
                byte[] bytes = unsplash.downloadImageBytes(imageUrl);

                String fileName = baseName + "_" + System.currentTimeMillis() + ".jpg";
                Path target = UPLOAD_DIR.resolve(fileName);
                Files.write(target, bytes, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);

                return new ImageAiService.GeneratedImage(fileName, target, imageUrl, bytes);

            } catch (Exception ignore) {
                // continue
            }
        }

        // ✅ dernier fallback: image locale par défaut
        // (assure-toi que logo.png existe dans uploads/images ou resources)
        String fallbackName = "logo.png";
        Path p = UPLOAD_DIR.resolve(fallbackName);

        if (Files.exists(p)) {
            byte[] bytes = Files.readAllBytes(p);
            return new ImageAiService.GeneratedImage(fallbackName, p, "local-default", bytes);
        }

        // Si pas de logo non plus, on ne crash pas, mais on explique.
        throw new RuntimeException("Aucune image disponible (Pollinations+Unsplash+logo.png).");
    }

    private static String buildUnsplashQuery(String titre, String desc, String type, String lieu) {
        String q = (safe(titre) + " " + extractScene(desc) + " " + safe(type) + " " + safe(lieu)).trim();
        return q.isBlank() ? "event" : q;
    }

    private static String safeFileBase(String titre) {
        String base = safe(titre);
        if (base.isBlank()) base = "event";
        base = base.replaceAll("[^a-zA-Z0-9-_]", "_");
        if (base.length() > 30) base = base.substring(0, 30);
        return base;
    }

    private static String safe(String s) {
        return s == null ? "" : s.trim();
    }
}