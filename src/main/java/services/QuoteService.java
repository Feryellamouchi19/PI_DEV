package services;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

public class QuoteService {

    private final HttpClient http = HttpClient.newBuilder()
            .followRedirects(HttpClient.Redirect.ALWAYS)
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    public static class Quote {
        public final String text;
        public final String author;

        public Quote(String text, String author) {
            this.text = text;
            this.author = author;
        }
    }

    public Quote getRandomQuote() throws Exception {
        String url = "https://zenquotes.io/api/random";

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .GET()
                .timeout(Duration.ofSeconds(10))
                .header("User-Agent", "PI_DEV JavaFX")
                .build();

        HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString());
        if (resp.statusCode() != 200) return null;

        String json = resp.body();
        // Format: [{"q":"...","a":"...","h":"..."}]

        String q = extractFirst(json, "\"q\":\"", "\"");
        String a = extractFirst(json, "\"a\":\"", "\"");

        if (q == null || q.isBlank()) return null;

        q = unescape(q);
        a = (a == null) ? "" : unescape(a);

        return new Quote(q, a);
    }

    private static String extractFirst(String s, String start, String end) {
        int i = s.indexOf(start);
        if (i < 0) return null;
        int j = s.indexOf(end, i + start.length());
        if (j < 0) return null;
        return s.substring(i + start.length(), j);
    }

    private static String unescape(String s) {
        return s.replace("\\n", "\n")
                .replace("\\\"", "\"")
                .replace("\\u2019", "’")
                .replace("\\u201c", "“")
                .replace("\\u201d", "”");
    }
}