package services;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

public class GeoCodingService {

    private final HttpClient http = HttpClient.newBuilder()
            .followRedirects(HttpClient.Redirect.ALWAYS)
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    public static class GeoPoint {
        public final double lat;
        public final double lon;
        public final String displayName;

        public GeoPoint(double lat, double lon, String displayName) {
            this.lat = lat;
            this.lon = lon;
            this.displayName = displayName;
        }
    }

    public GeoPoint geocode(String location) throws Exception {
        if (location == null || location.isBlank()) return null;

        String q = URLEncoder.encode(location.trim(), StandardCharsets.UTF_8);

        String url = "https://nominatim.openstreetmap.org/search?format=json&limit=1&q=" + q;

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(10))
                .header("User-Agent", "PI_DEV JavaFX App")
                .GET()
                .build();

        HttpResponse<String> response =
                http.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) return null;

        String json = response.body();
        if (json == null || json.equals("[]")) return null;

        String latStr = extract(json, "\"lat\":\"", "\"");
        String lonStr = extract(json, "\"lon\":\"", "\"");
        String name   = extract(json, "\"display_name\":\"", "\"");

        if (latStr == null || lonStr == null) return null;

        return new GeoPoint(
                Double.parseDouble(latStr),
                Double.parseDouble(lonStr),
                name
        );
    }

    private String extract(String s, String start, String end) {
        int i = s.indexOf(start);
        if (i < 0) return null;
        int j = s.indexOf(end, i + start.length());
        if (j < 0) return null;
        return s.substring(i + start.length(), j);
    }
}