package services;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SpotifyOEmbedService {

    public static class SpotifyCard {
        public final String title;
        public final String thumbnailUrl;
        public final String providerName;

        public SpotifyCard(String title, String thumbnailUrl, String providerName) {
            this.title = title;
            this.thumbnailUrl = thumbnailUrl;
            this.providerName = providerName;
        }
    }

    private final HttpClient http = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(8))
            .build();

    /**
     * Récupère un "card" (title + image) depuis Spotify oEmbed.
     * Exemple URL: https://open.spotify.com/playlist/XXXX
     */
    public SpotifyCard fetchCard(String spotifyUrl) throws Exception {
        String encoded = URLEncoder.encode(spotifyUrl, StandardCharsets.UTF_8);
        String url = "https://open.spotify.com/oembed?url=" + encoded;

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(10))
                .GET()
                .header("User-Agent", "JavaFX")
                .build();

        HttpResponse<String> res = http.send(req, HttpResponse.BodyHandlers.ofString());
        if (res.statusCode() != 200) {
            throw new RuntimeException("Spotify oEmbed HTTP " + res.statusCode());
        }

        String json = res.body();

        String title = extractJsonString(json, "title");
        String thumb = extractJsonString(json, "thumbnail_url");
        String provider = extractJsonString(json, "provider_name");

        return new SpotifyCard(title, thumb, provider);
    }

    // --- mini extracteur JSON (sans lib) ---
    private static String extractJsonString(String json, String key) {
        Pattern p = Pattern.compile("\"" + Pattern.quote(key) + "\"\\s*:\\s*\"(.*?)\"", Pattern.DOTALL);
        Matcher m = p.matcher(json);
        if (!m.find()) return "";
        return unescape(m.group(1));
    }

    private static String unescape(String s) {
        return s.replace("\\/", "/")
                .replace("\\n", "\n")
                .replace("\\t", "\t")
                .replace("\\\"", "\"");
    }
}