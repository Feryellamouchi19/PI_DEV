package services;

import javax.mail.*;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import java.io.InputStream;
import java.time.format.DateTimeFormatter;
import java.util.Properties;

/**
 * Envoi d'emails (SMTP). Config dans mail.properties (classpath) ou variables d'environnement.
 * Pour la réservation coin maquillage : envoi d'un email de confirmation.
 */
public class EmailService {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy à HH:mm");
    private final Properties config = new Properties();

    public EmailService() {
        try (InputStream is = getClass().getResourceAsStream("/mail.properties")) {
            if (is != null) config.load(is);
        } catch (Exception ignored) { }

        String host = System.getProperty("mail.smtp.host");
        if (host != null) config.setProperty("mail.smtp.host", host);
        String port = System.getProperty("mail.smtp.port");
        if (port != null) config.setProperty("mail.smtp.port", port);
        String user = System.getProperty("mail.smtp.user");
        if (user != null) config.setProperty("mail.smtp.user", user);
        String pass = System.getProperty("mail.smtp.password");
        if (pass != null) config.setProperty("mail.smtp.password", pass);
    }

    private boolean isConfigured() {
        String host = config.getProperty("mail.smtp.host");
        return host != null && !host.isBlank();
    }

    /**
     * Envoie l'email de confirmation de réservation coin maquillage.
     * @return true si l'email a été envoyé, false sinon (config manquante ou erreur)
     */
    public boolean sendReservationMaquillageConfirmation(String toEmail, String eventTitre, String lieu, java.time.LocalDateTime dateDebut) {
        if (!isConfigured()) {
            System.err.println("EmailService: mail non configuré (mail.properties ou mail.smtp.host)");
            return false;
        }

        String from = config.getProperty("mail.from", config.getProperty("mail.smtp.user", "noreply@example.com"));
        String subject = "Confirmation réservation coin maquillage - " + (eventTitre != null ? eventTitre : "Événement");
        String dateStr = dateDebut != null ? dateDebut.format(DATE_FMT) : "—";
        String body = "Bonjour,\n\n"
                + "Votre réservation au coin maquillage a bien été enregistrée.\n\n"
                + "Événement : " + (eventTitre != null ? eventTitre : "—") + "\n"
                + "Lieu : " + (lieu != null ? lieu : "—") + "\n"
                + "Date : " + dateStr + "\n\n"
                + "À bientôt !";

        try {
            ensureDefaults();
            Session session = Session.getInstance(config, new Authenticator() {
                @Override
                protected PasswordAuthentication getPasswordAuthentication() {
                    String u = config.getProperty("mail.smtp.user");
                    String p = config.getProperty("mail.smtp.password");
                    return new PasswordAuthentication(u != null ? u : "", p != null ? p : "");
                }
            });

            MimeMessage msg = new MimeMessage(session);
            msg.setFrom(new InternetAddress(from));
            msg.setRecipients(Message.RecipientType.TO, InternetAddress.parse(toEmail));
            msg.setSubject(subject);
            msg.setText(body, "UTF-8");

            Transport.send(msg);
            return true;
        } catch (Exception ex) {
            System.err.println("EmailService.sendReservationMaquillageConfirmation: " + ex.getMessage());
            ex.printStackTrace();
            return false;
        }
    }

    private void ensureDefaults() {
        if (!config.containsKey("mail.smtp.auth")) config.setProperty("mail.smtp.auth", "true");
        if (!config.containsKey("mail.smtp.starttls.enable")) config.setProperty("mail.smtp.starttls.enable", "true");
        if (!config.containsKey("mail.smtp.port")) config.setProperty("mail.smtp.port", "587");
    }
}