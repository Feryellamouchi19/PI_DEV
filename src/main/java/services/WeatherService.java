package services;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * Météo gratuite sans API key:
 * - Geocoding: Open-Meteo Geocoding
 * - Forecast:  Open-Meteo Forecast
 */
public class WeatherService {

    public static class WeatherInfo {
        public final String locationName;
        public final double temperatureC;
        public final double windKmh;
        public final double precipitationMm;
        public final String time;

        public WeatherInfo(String locationName, double temperatureC, double windKmh, double precipitationMm, String time) {
            this.locationName = locationName;
            this.temperatureC = temperatureC;
            this.windKmh = windKmh;
            this.precipitationMm = precipitationMm;
            this.time = time;
        }
    }

    // ---------- PUBLIC API ----------
    public WeatherInfo getWeatherForCityAtHour(String city, String isoDateTime) throws Exception {
        if (city == null || city.isBlank()) throw new IllegalArgumentException("Lieu vide");
        if (isoDateTime == null || isoDateTime.isBlank()) throw new IllegalArgumentException("Date vide");

        // 1) Géocodage (lat/lon) depuis ville
        Geo geo = geocodeCity(city);

        // 2) Météo hour-by-hour (on prend l'heure exacte si possible)
        return forecastAtHour(geo, isoDateTime);
    }

    // ---------- INTERNAL ----------
    private static class Geo {
        double lat, lon;
        String name;
        Geo(double lat, double lon, String name) { this.lat = lat; this.lon = lon; this.name = name; }
    }

    private Geo geocodeCity(String city) throws Exception {
        String q = URLEncoder.encode(city, StandardCharsets.UTF_8);
        String url = "https://geocoding-api.open-meteo.com/v1/search?name=" + q + "&count=1&language=fr&format=json";

        String json = httpGet(url);

        // Parsing simple sans libs externes (on lit les champs lat/lon/name)
        // Cherche "latitude":... "longitude":... "name":"..."
        Double lat = extractNumber(json, "\"latitude\":");
        Double lon = extractNumber(json, "\"longitude\":");
        String name = extractString(json, "\"name\":\"");

        if (lat == null || lon == null) throw new IllegalArgumentException("Lieu introuvable: " + city);
        if (name == null) name = city;

        return new Geo(lat, lon, name);
    }

    private WeatherInfo forecastAtHour(Geo geo, String isoDateTime) throws Exception {
        // isoDateTime doit ressembler à: 2026-02-24T09:00 (ou 2026-02-24 09:00)
        String dt = isoDateTime.replace(" ", "T");
        String date = dt.substring(0, 10);

        String url =
                "https://api.open-meteo.com/v1/forecast" +
                        "?latitude=" + geo.lat +
                        "&longitude=" + geo.lon +
                        "&hourly=temperature_2m,precipitation,windspeed_10m" +
                        "&timezone=auto" +
                        "&start_date=" + date +
                        "&end_date=" + date;

        String json = httpGet(url);

        // On trouve l'index de l'heure demandée dans hourly.time[]
        int idx = indexOfTime(json, dt);

        // Si heure exacte introuvable (timezone auto), on prend le plus proche: on prend la première heure.
        if (idx < 0) idx = 0;

        Double temp = extractNumberAtIndex(json, "\"temperature_2m\":[", idx);
        Double wind = extractNumberAtIndex(json, "\"windspeed_10m\":[", idx);
        Double rain = extractNumberAtIndex(json, "\"precipitation\":[", idx);

        // Heure réellement utilisée
        String usedTime = extractStringAtIndex(json, "\"time\":[", idx);

        if (temp == null) temp = Double.NaN;
        if (wind == null) wind = Double.NaN;
        if (rain == null) rain = Double.NaN;

        return new WeatherInfo(geo.name, temp, wind, rain, usedTime != null ? usedTime : dt);
    }

    // ---------- HTTP ----------
    private String httpGet(String urlStr) throws Exception {
        URL url = new URL(urlStr);
        HttpURLConnection con = (HttpURLConnection) url.openConnection();
        con.setRequestMethod("GET");
        con.setConnectTimeout(8000);
        con.setReadTimeout(8000);

        int code = con.getResponseCode();
        if (code != 200) {
            throw new RuntimeException("HTTP " + code + " sur " + urlStr);
        }

        try (BufferedReader br = new BufferedReader(new InputStreamReader(con.getInputStream(), StandardCharsets.UTF_8))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) sb.append(line);
            return sb.toString();
        }
    }

    // ---------- PARSING MINIMAL ----------
    private Double extractNumber(String json, String key) {
        int i = json.indexOf(key);
        if (i < 0) return null;
        i += key.length();
        int j = i;
        while (j < json.length() && ("0123456789.-".indexOf(json.charAt(j)) >= 0)) j++;
        try { return Double.parseDouble(json.substring(i, j)); } catch (Exception e) { return null; }
    }

    private String extractString(String json, String key) {
        int i = json.indexOf(key);
        if (i < 0) return null;
        i += key.length();
        int j = json.indexOf("\"", i);
        if (j < 0) return null;
        return json.substring(i, j);
    }

    private int indexOfTime(String json, String target) {
        // target: 2026-02-24T09:00
        // dans JSON time est souvent "2026-02-24T09:00"
        int arr = json.indexOf("\"time\":[");
        if (arr < 0) return -1;
        int end = json.indexOf("]", arr);
        if (end < 0) return -1;
        String slice = json.substring(arr, end);

        // Compte l'index des occurrences
        int idx = 0;
        int pos = 0;
        while (true) {
            int q1 = slice.indexOf("\"", pos);
            if (q1 < 0) break;
            int q2 = slice.indexOf("\"", q1 + 1);
            if (q2 < 0) break;
            String t = slice.substring(q1 + 1, q2);
            if (t.equals(target)) return idx;
            idx++;
            pos = q2 + 1;
        }
        return -1;
    }

    private Double extractNumberAtIndex(String json, String arrayKey, int index) {
        int i = json.indexOf(arrayKey);
        if (i < 0) return null;
        i += arrayKey.length();
        int end = json.indexOf("]", i);
        if (end < 0) return null;
        String arr = json.substring(i, end);
        String[] parts = arr.split(",");
        if (index < 0 || index >= parts.length) return null;
        try { return Double.parseDouble(parts[index].trim()); } catch (Exception e) { return null; }
    }

    private String extractStringAtIndex(String json, String arrayKey, int index) {
        int i = json.indexOf(arrayKey);
        if (i < 0) return null;
        i += arrayKey.length();
        int end = json.indexOf("]", i);
        if (end < 0) return null;
        String arr = json.substring(i, end);

        // arr contient: "2026-02-24T00:00","2026-02-24T01:00",...
        String[] parts = arr.split("\",\"");
        if (parts.length == 0) return null;

        // Nettoyage 1er et dernier
        for (int k = 0; k < parts.length; k++) {
            parts[k] = parts[k].replace("\"", "").trim();
        }

        if (index < 0 || index >= parts.length) return null;
        return parts[index];
    }
}