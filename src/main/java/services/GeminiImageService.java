package services;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.net.URI;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;

public class GeminiImageService {

    // Essaie d’abord ce modèle (image preview / image)
    // Si tu veux tester autre chose après, on changera ensemble.
    private static final String MODEL_ID = "gemini-2.5-flash-image"; // d'après ta liste models
    private static final ObjectMapper M = new ObjectMapper();

    private final HttpClient http = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(15))
            .followRedirects(HttpClient.Redirect.ALWAYS)
            .build();

    public byte[] generateImageBytes(String prompt) throws Exception {
        String key = System.getenv("GEMINI_API_KEY");
        if (key == null || key.isBlank()) {
            throw new IllegalStateException("GEMINI_API_KEY introuvable (setx puis redémarre IntelliJ/terminal)");
        }

        String url = "https://generativelanguage.googleapis.com/v1beta/models/"
                + MODEL_ID + ":generateContent";

        // Body: prompt + imageConfig
        String jsonBody = """
        {
          "contents": [{
            "parts": [{"text": %s}]
          }],
          "generationConfig": {
            "imageConfig": {
              "aspectRatio": "16:9"
            }
          }
        }
        """.formatted(M.writeValueAsString(prompt));

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(120))
                .header("Content-Type", "application/json; charset=utf-8")
                .header("x-goog-api-key", key)
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody, StandardCharsets.UTF_8))
                .build();

        HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        if (resp.statusCode() != 200) {
            throw new RuntimeException("Erreur Gemini (" + resp.statusCode() + "): " + resp.body());
        }

        JsonNode root = M.readTree(resp.body());

        // Cherche inlineData.data (base64)
        JsonNode candidates = root.path("candidates");
        if (!candidates.isArray() || candidates.isEmpty()) {
            throw new RuntimeException("Réponse sans candidates: " + resp.body());
        }

        JsonNode parts = candidates.get(0).path("content").path("parts");
        if (!parts.isArray()) {
            throw new RuntimeException("Réponse invalide (parts): " + resp.body());
        }

        for (JsonNode part : parts) {
            JsonNode inline = part.path("inlineData");
            if (!inline.isMissingNode()) {
                String data = inline.path("data").asText("");
                if (!data.isBlank()) {
                    return Base64.getDecoder().decode(data);
                }
            }
        }

        throw new RuntimeException("Aucune image trouvée dans la réponse: " + resp.body());
    }
}