package services;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class SpotifyOEmbedService {

    private final HttpClient http = HttpClient.newBuilder()
            .followRedirects(HttpClient.Redirect.ALWAYS)
            .build();

    public static class SpotifyInfo {
        public String title;
        public String providerName;
        public String thumbnailUrl;
    }

    public SpotifyInfo fetchOEmbed(String spotifyUrl) throws Exception {
        if (spotifyUrl == null || spotifyUrl.isBlank()) return null;

        // oEmbed endpoint Spotify
        String endpoint = "https://open.spotify.com/oembed?url=" +
                URLEncoder.encode(spotifyUrl, StandardCharsets.UTF_8);

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(endpoint))
                .header("User-Agent", "JavaFX")
                .GET()
                .build();

        HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString());
        if (resp.statusCode() != 200) return null;

        String json = resp.body();
        SpotifyInfo info = new SpotifyInfo();

        info.title = extractString(json, "\"title\"");
        info.providerName = extractString(json, "\"provider_name\"");
        info.thumbnailUrl = extractString(json, "\"thumbnail_url\"");

        return info;
    }

    // mini extract JSON (sans lib)
    private String extractString(String json, String key) {
        if (json == null) return "";
        int i = json.indexOf(key);
        if (i < 0) return "";
        int colon = json.indexOf(":", i);
        if (colon < 0) return "";
        int firstQuote = json.indexOf("\"", colon + 1);
        if (firstQuote < 0) return "";
        int secondQuote = json.indexOf("\"", firstQuote + 1);
        if (secondQuote < 0) return "";
        return json.substring(firstQuote + 1, secondQuote).trim();
    }
}