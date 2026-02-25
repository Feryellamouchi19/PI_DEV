package services;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.api.client.util.DateTime;
import com.google.api.services.calendar.Calendar;
import com.google.api.services.calendar.CalendarScopes;
import com.google.api.services.calendar.model.Event;
import com.google.api.services.calendar.model.EventDateTime;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;

import entities.Evenement;

import java.awt.Desktop;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.nio.file.Path;
import java.time.ZoneId;
import java.util.Collections;
import java.util.Date;

public class GoogleCalendarService {

    private static final String APPLICATION_NAME = "PI_DEV Events";
    private static final GsonFactory JSON_FACTORY = GsonFactory.getDefaultInstance();
    private static final String TOKENS_DIR = "tokens";

    private Calendar getCalendar() throws Exception {
        final NetHttpTransport httpTransport = GoogleNetHttpTransport.newTrustedTransport();

        InputStream in = getClass().getResourceAsStream("/credentials.json");
        if (in == null) throw new IllegalStateException("credentials.json introuvable dans src/main/resources");

        GoogleClientSecrets clientSecrets =
                GoogleClientSecrets.load(JSON_FACTORY, new InputStreamReader(in));

        GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(
                httpTransport,
                JSON_FACTORY,
                clientSecrets,
                Collections.singletonList(CalendarScopes.CALENDAR_EVENTS)
        )
                .setDataStoreFactory(new FileDataStoreFactory(Path.of(TOKENS_DIR).toFile()))
                .setAccessType("offline")
                .build();

        // Serveur local pour récupérer le code OAuth
        LocalServerReceiver receiver = new LocalServerReceiver.Builder().setPort(8888).build();

        // 1) Générer URL d'autorisation
        String authUrl = flow.newAuthorizationUrl()
                .setRedirectUri(receiver.getRedirectUri())
                .build();

        // 2) Ouvrir navigateur
        try {
            if (Desktop.isDesktopSupported()) {
                Desktop.getDesktop().browse(new URI(authUrl));
            } else {
                System.out.println("Ouvre ce lien dans ton navigateur:\n" + authUrl);
            }
        } catch (Exception ex) {
            System.out.println("Ouvre ce lien dans ton navigateur:\n" + authUrl);
        }

        // 3) Attendre le code via localhost
        String code = receiver.waitForCode();

        // 4) Echanger code -> token
        var tokenResponse = flow.newTokenRequest(code)
                .setRedirectUri(receiver.getRedirectUri())
                .execute();

        // 5) Stocker credential
        Credential credential = flow.createAndStoreCredential(tokenResponse, "user");

        receiver.stop();

        return new Calendar.Builder(httpTransport, JSON_FACTORY, credential)
                .setApplicationName(APPLICATION_NAME)
                .build();
    }

    public String addToPrimaryCalendar(Evenement ev) throws Exception {
        Calendar service = getCalendar();

        Event event = new Event()
                .setSummary(ev.getTitre())
                .setDescription(ev.getDescription())
                .setLocation(ev.getLieu());

        ZoneId zone = ZoneId.of("Africa/Tunis");

        if (ev.getDateDebut() != null) {
            Date startDate = Date.from(ev.getDateDebut().atZone(zone).toInstant());
            event.setStart(new EventDateTime().setDateTime(new DateTime(startDate)));
        }
        if (ev.getDateFin() != null) {
            Date endDate = Date.from(ev.getDateFin().atZone(zone).toInstant());
            event.setEnd(new EventDateTime().setDateTime(new DateTime(endDate)));
        }

        Event created = service.events().insert("primary", event).execute();
        return created.getHtmlLink();
    }
}