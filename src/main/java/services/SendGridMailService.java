package services;

import javafx.concurrent.Task;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Properties;
import java.util.function.Consumer;

public class SendGridMailService {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy à HH:mm");

    private final HttpClient client = HttpClient.newHttpClient();
    private final Properties cfg = new Properties();

    public SendGridMailService() {
        try (var is = getClass().getResourceAsStream("/sendgrid.properties")) {
            if (is != null) cfg.load(is);
        } catch (Exception e) {
            System.err.println("SendGridMailService: erreur lecture sendgrid.properties: " + e.getMessage());
        }
    }

    private boolean enabled() {
        return "true".equalsIgnoreCase(cfg.getProperty("sendgrid.enabled", "true"));
    }

    private String apiKey() {
        String k = System.getenv("SENDGRID_API_KEY");
        return k == null ? "" : k.trim();
    }

    private String fromEmail() {
        return cfg.getProperty("sendgrid.from.email", "").trim();
    }

    private String fromName() {
        String n = cfg.getProperty("sendgrid.from.name", "LAMMA");
        return (n == null || n.isBlank()) ? "LAMMA" : n.trim();
    }

    /** ✅ Async (ne bloque pas JavaFX) */
    public void sendReservationMaquillageConfirmationAsync(
            String toEmail,
            String eventTitre,
            String lieu,
            LocalDateTime dateDebut,
            Consumer<Boolean> onDone
    ) {
        Task<Boolean> task = new Task<>() {
            @Override
            protected Boolean call() {
                return sendReservationMaquillageConfirmation(toEmail, eventTitre, lieu, dateDebut);
            }
        };

        task.setOnSucceeded(e -> onDone.accept(task.getValue()));
        task.setOnFailed(e -> {
            if (task.getException() != null) task.getException().printStackTrace();
            onDone.accept(false);
        });

        Thread th = new Thread(task, "sendgrid-mail-task");
        th.setDaemon(true);
        th.start();
    }

    /** ✅ Sync (appelé dans le Task) */
    private boolean sendReservationMaquillageConfirmation(
            String toEmail,
            String eventTitre,
            String lieu,
            LocalDateTime dateDebut
    ) {
        if (!enabled()) {
            System.out.println("SendGridMailService: disabled (sendgrid.enabled=false)");
            return false;
        }

        String key = apiKey();
        if (key.isBlank()) {
            System.err.println("SendGridMailService: SENDGRID_API_KEY manquante.");
            return false;
        }

        String from = fromEmail();
        if (from.isBlank()) {
            System.err.println("SendGridMailService: sendgrid.from.email manquant dans sendgrid.properties.");
            return false;
        }

        if (toEmail == null || toEmail.isBlank()) {
            System.err.println("SendGridMailService: toEmail vide.");
            return false;
        }

        String subject = "Confirmation réservation coin maquillage - " + safe(eventTitre, "Événement");
        String dateStr = dateDebut != null ? dateDebut.format(DATE_FMT) : "—";

        String text = "Bonjour,\n\n"
                + "Votre réservation au coin maquillage a bien été enregistrée.\n\n"
                + "Événement : " + safe(eventTitre, "—") + "\n"
                + "Lieu : " + safe(lieu, "—") + "\n"
                + "Date : " + dateStr + "\n\n"
                + "À bientôt !";

        String html =
                "<div style='font-family:Segoe UI,Arial,sans-serif;font-size:14px;color:#0f172a'>"
                        + "<h2 style='margin:0 0 12px'>Réservation confirmée</h2>"
                        + "<p>Bonjour,</p>"
                        + "<p>Votre réservation au <b>coin maquillage</b> a bien été enregistrée.</p>"
                        + "<div style='padding:12px;border:1px solid #e2e8f0;border-radius:10px;background:#f8fafc'>"
                        + "<p style='margin:6px 0'><b>Événement :</b> " + escapeHtml(safe(eventTitre, "—")) + "</p>"
                        + "<p style='margin:6px 0'><b>Lieu :</b> " + escapeHtml(safe(lieu, "—")) + "</p>"
                        + "<p style='margin:6px 0'><b>Date :</b> " + escapeHtml(dateStr) + "</p>"
                        + "</div>"
                        + "<p style='margin-top:16px'>À bientôt !</p>"
                        + "</div>";

        String json = "{"
                + "\"personalizations\":[{\"to\":[{\"email\":\"" + esc(toEmail) + "\"}],\"subject\":\"" + esc(subject) + "\"}],"
                + "\"from\":{\"email\":\"" + esc(from) + "\",\"name\":\"" + esc(fromName()) + "\"},"
                + "\"content\":["
                + "{\"type\":\"text/plain\",\"value\":\"" + esc(text) + "\"},"
                + "{\"type\":\"text/html\",\"value\":\"" + esc(html) + "\"}"
                + "]"
                + "}";

        try {
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create("https://api.sendgrid.com/v3/mail/send"))
                    .header("Authorization", "Bearer " + key)
                    .header("Content-Type", "application/json; charset=utf-8")
                    .POST(HttpRequest.BodyPublishers.ofString(json, StandardCharsets.UTF_8))
                    .build();

            HttpResponse<String> res = client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

            if (res.statusCode() == 202) return true;

            System.err.println("SendGridMailService: status=" + res.statusCode());
            System.err.println(res.body());
            return false;

        } catch (Exception e) {
            System.err.println("SendGridMailService: exception: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    private static String safe(String s, String def) {
        return (s == null || s.isBlank()) ? def : s;
    }

    private static String esc(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r");
    }

    private static String escapeHtml(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }
}