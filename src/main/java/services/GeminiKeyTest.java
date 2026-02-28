package services;

import java.net.URI;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

public class GeminiKeyTest {
    public static void main(String[] args) throws Exception {

        String key = System.getenv("GEMINI_API_KEY");
        if (key == null || key.isBlank()) {
            System.out.println("❌ GEMINI_API_KEY introuvable");
            return;
        }

        String url = "https://generativelanguage.googleapis.com/v1beta/models?key=" + key;

        HttpClient http = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(20))
                .GET()
                .build();

        HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        System.out.println("Status = " + resp.statusCode());
        System.out.println(resp.body());
    }
}