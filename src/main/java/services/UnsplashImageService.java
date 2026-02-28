package services;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

public class UnsplashImageService {

    private static final ObjectMapper M = new ObjectMapper();

    private final HttpClient http = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(15))
            .followRedirects(HttpClient.Redirect.ALWAYS)
            .build();

    /**
     * Retourne une URL d'image (regular) depuis Unsplash selon une requête.
     */
    public String getRandomImageUrl(String query) throws Exception {
        String key = System.getenv("UNSPLASH_ACCESS_KEY");
        if (key == null || key.isBlank()) {
            throw new IllegalStateException("UNSPLASH_ACCESS_KEY introuvable");
        }

        String q = (query == null || query.isBlank()) ? "event" : query.trim();
        String url = "https://api.unsplash.com/photos/random?query="
                + URLEncoder.encode(q, StandardCharsets.UTF_8)
                + "&orientation=landscape"
                + "&content_filter=high";

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(30))
                .header("Accept-Version", "v1")
                .header("Authorization", "Client-ID " + key)
                .GET()
                .build();

        HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        if (resp.statusCode() != 200) {
            throw new RuntimeException("Erreur Unsplash (" + resp.statusCode() + "): " + resp.body());
        }

        JsonNode root = M.readTree(resp.body());
        String imageUrl = root.path("urls").path("regular").asText("");
        if (imageUrl.isBlank()) {
            // fallback
            imageUrl = root.path("urls").path("full").asText("");
        }
        if (imageUrl.isBlank()) {
            throw new RuntimeException("Unsplash: URL image introuvable: " + resp.body());
        }
        return imageUrl;
    }

    /**
     * Télécharge les bytes d'une image (jpg généralement).
     */
    public byte[] downloadImageBytes(String imageUrl) throws Exception {
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(imageUrl))
                .timeout(Duration.ofSeconds(60))
                .header("User-Agent", "JavaFX")
                .GET()
                .build();

        HttpResponse<byte[]> resp = http.send(req, HttpResponse.BodyHandlers.ofByteArray());
        if (resp.statusCode() != 200) {
            throw new RuntimeException("Erreur download image (" + resp.statusCode() + ")");
        }
        return resp.body();
    }
}