package controllers;

import entities.Evenement;
import entities.Programme;
import interfaces.DataReceiver;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.TextInputDialog;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import services.EquipmentService;
import services.EvenementService;
import services.EventImageApi;
import services.ImageAiService;
import services.ProgrammeService;
import services.SpotifyOEmbedService;
import utils.Session;

// ✅ services ajoutés
import services.GeoCodingService;

import java.awt.Desktop;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

// HTTP
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class DetailsEvenementController implements DataReceiver<Integer> {

    private final DateTimeFormatter F = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private final DateTimeFormatter H = DateTimeFormatter.ofPattern("HH:mm");

    private final HttpClient http = HttpClient.newBuilder()
            .followRedirects(HttpClient.Redirect.ALWAYS)
            .build();

    @FXML private Label lblTitre;
    @FXML private Label lblType;
    @FXML private Label lblLieu;
    @FXML private Label lblDebut;
    @FXML private Label lblFin;
    @FXML private Label lblDescription;
    @FXML private Label lblMsg;

    @FXML private ImageView bgImage;
    @FXML private ImageView imgLogo;
    @FXML private ImageView imgEvent;
    @FXML private Button btnGenererImageIa;
    @FXML private Label lblImageIaMsg;

    @FXML private VBox progContainer;

    @FXML private VBox boxEquipmentBanner;
    @FXML private Label lblEquipmentTitle;
    @FXML private FlowPane flowEquipment;

    @FXML private VBox boxReservationMaquillage;
    @FXML private Label lblReservationMaquillage;
    @FXML private Button btnReserverMaquillage;

    // ✅ QR
    @FXML private ImageView imgQr;
    @FXML private Label lblQrInfo;
    @FXML private Button btnRegenQr;

    // ✅ Meteo
    @FXML private Label lblMeteo;
    private final services.WeatherService weatherService = new services.WeatherService();

    // ✅ ✅ CITATION
    @FXML private Label lblQuote;
    @FXML private Label lblQuoteAuthor;
    private final services.QuoteService quoteService = new services.QuoteService();

    // ✅ ✅ LOCALISATION (NOUVEAU)
    @FXML private Label lblLatitude;
    @FXML private Label lblLongitude;
    @FXML private Button btnOpenMap;

    private final GeoCodingService geoService = new GeoCodingService();
    private GeoCodingService.GeoPoint currentGeoPoint;

    // ✅ Spotify
    @FXML private VBox spotifyBox;
    @FXML private Label lblSpotifyMsg;
    @FXML private ImageView imgSpotifyCover;
    @FXML private Label lblSpotifyTitle;
    @FXML private Label lblSpotifyProvider;
    @FXML private Button btnOpenSpotify;

    // ✅ Admin buttons
    @FXML private HBox adminButtons;

    // ✅ USER buttons
    @FXML private HBox userButtons;
    @FXML private Button btnAddToGoogleCalendar;
    @FXML private Button btnToggleNotif;

    private boolean notificationsEnabled = false;

    private final EvenementService evenementService = new EvenementService();
    private final EquipmentService equipmentService = new EquipmentService();
    private final services.ReservationMaquillageService reservationMaquillageService = new services.ReservationMaquillageService();
    private final services.EmailService emailService = new services.EmailService();
    private ProgrammeService programmeService;

    private int eventId = 0;
    private Evenement event;

    private final SpotifyOEmbedService spotifyService = new SpotifyOEmbedService();
    private final EventImageApi imageApi = new EventImageApi();

    @FXML
    public void initialize() {
        SceneUtil.loadBackgroundImage(bgImage);
        SceneUtil.loadLogoImage(imgLogo);
        programmeService = new ProgrammeService();

        if (lblMsg != null) lblMsg.setText("");
        if (lblQrInfo != null) lblQrInfo.setText("");
        if (lblMeteo != null) lblMeteo.setText("");
        if (lblImageIaMsg != null) lblImageIaMsg.setText("");

        hideSpotifyBox();
        updateNotifButtonText();

        applyRoleUI();

        // ✅ citation au démarrage
        loadQuoteAsync();
    }

    @Override
    public void setData(Integer id) {
        if (id == null || id <= 0) {
            showError("Aucun ID reçu / ID invalide");
            clearDetails();
            return;
        }

        this.eventId = id;

        applyRoleUI();
        loadDetails();
        loadProgrammes();
    }

    // ===================== ✅ ROLE UI =====================
    private void applyRoleUI() {
        boolean admin = Session.isAdmin();

        if (adminButtons != null) {
            adminButtons.setVisible(admin);
            adminButtons.setManaged(admin);
        }

        if (btnRegenQr != null) {
            btnRegenQr.setVisible(admin);
            btnRegenQr.setManaged(admin);
        }

        if (btnGenererImageIa != null) {
            btnGenererImageIa.setVisible(admin);
            btnGenererImageIa.setManaged(admin);
        }

        System.out.println("DetailsEvenement ROLE = " + Session.getRole());
    }

    private void loadDetails() {
        event = evenementService.getOneById(eventId);

        if (event == null) {
            showError("Événement introuvable");
            clearDetails();
            return;
        }

        if (lblTitre != null) lblTitre.setText(safe(event.getTitre()));
        if (lblType != null) lblType.setText(safe(event.getType()));
        if (lblLieu != null) lblLieu.setText(safe(event.getLieu()));
        if (lblDebut != null) lblDebut.setText(event.getDateDebut() == null ? "—" : event.getDateDebut().format(F));
        if (lblFin != null) lblFin.setText(event.getDateFin() == null ? "—" : event.getDateFin().format(F));
        if (lblDescription != null) lblDescription.setText(safe(event.getDescription()));

        loadEventImage(event.getImage());
        if (lblImageIaMsg != null) lblImageIaMsg.setText("");

        loadEquipmentBanner();
        loadReservationMaquillageSection();

        evenementService.incrementVues(eventId);

        onGenererQr();
        loadMeteoAsync();
        applySpotifyUI();

        // ✅ ✅ LOCALISATION
        loadLocationAsync();
    }

    // ===================== ✅ LOCALISATION =====================
    private void loadLocationAsync() {
        if (event == null || event.getLieu() == null || event.getLieu().isBlank()) return;

        if (lblLatitude != null) lblLatitude.setText("⏳");
        if (lblLongitude != null) lblLongitude.setText("⏳");
        currentGeoPoint = null;

        Task<GeoCodingService.GeoPoint> task = new Task<>() {
            @Override
            protected GeoCodingService.GeoPoint call() throws Exception {
                return geoService.geocode(event.getLieu());
            }
        };

        task.setOnSucceeded(e -> {
            currentGeoPoint = task.getValue();
            if (currentGeoPoint == null) {
                if (lblLatitude != null) lblLatitude.setText("❌");
                if (lblLongitude != null) lblLongitude.setText("❌");
                return;
            }

            if (lblLatitude != null)  lblLatitude.setText(String.format("%.5f", currentGeoPoint.lat));
            if (lblLongitude != null) lblLongitude.setText(String.format("%.5f", currentGeoPoint.lon));
        });

        task.setOnFailed(e -> {
            if (lblLatitude != null) lblLatitude.setText("❌");
            if (lblLongitude != null) lblLongitude.setText("❌");
        });

        Thread th = new Thread(task, "geo-task");
        th.setDaemon(true);
        th.start();
    }

    @FXML
    private void onOpenMap() {
        if (currentGeoPoint == null) {
            if (lblMsg != null) lblMsg.setText("❌ Localisation indisponible");
            return;
        }

        try {
            String url = "https://www.google.com/maps?q=" + currentGeoPoint.lat + "," + currentGeoPoint.lon;

            if (Desktop.isDesktopSupported()) {
                Desktop.getDesktop().browse(new URI(url));
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            if (lblMsg != null) lblMsg.setText("❌ Impossible d'ouvrir Google Maps");
        }
    }

    private void loadEquipmentBanner() {
        if (boxEquipmentBanner == null || flowEquipment == null) return;

        List<entities.Equipment> list = equipmentService.getByEventId(eventId);

        // N'afficher que les équipements choisis par l'admin (jamais la liste de suggestions)
        if (list == null || list.isEmpty()) {
            boxEquipmentBanner.setVisible(false);
            boxEquipmentBanner.setManaged(false);
            return;
        }

        if (lblEquipmentTitle != null) lblEquipmentTitle.setText("📋 À prévoir pour cet événement");
        flowEquipment.getChildren().clear();
        for (entities.Equipment eq : list) {
            Label chip = new Label("✓ " + safe(eq.getLibelle()));
            chip.getStyleClass().add("chip");
            chip.setStyle("-fx-text-fill: white; -fx-background-color: rgba(255,255,255,0.25); -fx-padding: 6 12; -fx-background-radius: 4;");
            flowEquipment.getChildren().add(chip);
        }
        boxEquipmentBanner.setVisible(true);
        boxEquipmentBanner.setManaged(true);
    }

    private boolean eventHasMaquillageSpecial(List<entities.Equipment> list) {
        if (list == null) return false;
        for (entities.Equipment eq : list) {
            String lib = safe(eq.getLibelle()).toLowerCase(Locale.ROOT).replace("é", "e");
            if (lib.contains("maquillage") && (lib.contains("special") || lib.contains("spécial"))) return true;
        }
        return false;
    }

    private void loadReservationMaquillageSection() {
        if (boxReservationMaquillage == null) return;
        List<entities.Equipment> list = equipmentService.getByEventId(eventId);
        boolean show = eventHasMaquillageSpecial(list);
        boxReservationMaquillage.setVisible(show);
        boxReservationMaquillage.setManaged(show);
    }

    @FXML
    private void onReserverMaquillage() {
        if (event == null || eventId <= 0) return;

        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Réservation coin maquillage");
        dialog.setHeaderText("Réservez votre place au coin maquillage");
        dialog.setContentText("Votre email (confirmation envoyée à cette adresse) :");
        dialog.getEditor().setPromptText("exemple@email.com");

        dialog.showAndWait().ifPresent(email -> {
            String em = email == null ? "" : email.trim();
            if (em.isBlank()) {
                if (lblMsg != null) lblMsg.setText("❌ Indiquez votre email.");
                return;
            }
            if (!em.contains("@") || !em.contains(".")) {
                if (lblMsg != null) lblMsg.setText("❌ Email invalide.");
                return;
            }
            try {
                if (reservationMaquillageService.exists(eventId, em)) {
                    if (lblMsg != null) lblMsg.setText("ℹ️ Une réservation existe déjà pour cet email.");
                    return;
                }
                reservationMaquillageService.add(eventId, em);
                boolean emailSent = emailService.sendReservationMaquillageConfirmation(
                        em, event.getTitre(), event.getLieu(), event.getDateDebut());
                if (lblMsg != null) {
                    if (emailSent) {
                        lblMsg.setText("✅ Réservation enregistrée ! Un email de confirmation a été envoyé à " + em);
                    } else {
                        lblMsg.setText("✅ Réservation enregistrée. (Email non envoyé : config SMTP manquante ou erreur.)");
                    }
                }
                if (lblReservationMaquillage != null) {
                    lblReservationMaquillage.setText("Réservation enregistrée pour " + em + (emailSent ? ". Un mail de confirmation vous a été envoyé." : "."));
                }
                if (btnReserverMaquillage != null) btnReserverMaquillage.setDisable(true);
            } catch (java.sql.SQLException ex) {
                if (lblMsg != null) lblMsg.setText("❌ Erreur enregistrement réservation.");
                ex.printStackTrace();
            }
        });
    }

    private void loadEventImage(String file) {
        if (imgEvent == null) return;
        String name = (file == null || file.isBlank()) ? "logo.png" : file.trim();

        try {
            Path p = Path.of("uploads/images").resolve(name);
            if (Files.exists(p)) {
                imgEvent.setImage(new Image(p.toUri().toString(), true));
                return;
            }

            try (InputStream is = getClass().getResourceAsStream("/images/" + name)) {
                if (is != null) {
                    imgEvent.setImage(new Image(is));
                    return;
                }
            }

            try (InputStream is = getClass().getResourceAsStream("/images/logo.png")) {
                if (is != null) imgEvent.setImage(new Image(is));
            }

        } catch (Exception ignored) {}
    }

    // ===================== ✅ GÉNÉRATION IMAGE IA =====================
    @FXML
    private void onGenererImageIa() {
        if (event == null) {
            showError("Aucun événement chargé.");
            return;
        }

        String titre = safe(event.getTitre());
        String desc = safe(event.getDescription());
        String type = safe(event.getType());
        String lieu = safe(event.getLieu());

        if (titre.isBlank()) {
            if (lblImageIaMsg != null) lblImageIaMsg.setText("❌ Titre requis");
            return;
        }

        if (btnGenererImageIa != null) btnGenererImageIa.setDisable(true);
        if (lblImageIaMsg != null) lblImageIaMsg.setText("⏳ Génération en cours...");

        Task<ImageAiService.GeneratedImage> task = new Task<>() {
            @Override
            protected ImageAiService.GeneratedImage call() throws Exception {
                return imageApi.generateForEvent(titre, desc, type, lieu);
            }
        };

        task.setOnSucceeded(e -> {
            if (btnGenererImageIa != null) btnGenererImageIa.setDisable(false);
            ImageAiService.GeneratedImage gen = task.getValue();
            if (gen == null) {
                if (lblImageIaMsg != null) lblImageIaMsg.setText("❌ Échec génération");
                return;
            }

            event.setImage(gen.fileName);
            evenementService.update(event);

            loadEventImage(gen.fileName);
            if (imgEvent != null && gen.bytes != null) {
                imgEvent.setImage(new Image(new ByteArrayInputStream(gen.bytes)));
            }

            if (lblImageIaMsg != null) lblImageIaMsg.setText("✅ Image générée");
        });

        task.setOnFailed(e -> {
            if (btnGenererImageIa != null) btnGenererImageIa.setDisable(false);
            Throwable ex = task.getException();
            String msg = (ex == null) ? "Erreur inconnue" : ex.getMessage();
            if (lblImageIaMsg != null) lblImageIaMsg.setText("❌ " + msg);
            if (ex != null) ex.printStackTrace();
        });

        Thread th = new Thread(task, "ai-image-details");
        th.setDaemon(true);
        th.start();
    }

    private void clearDetails() {
        if (lblTitre != null) lblTitre.setText("—");
        if (lblType != null) lblType.setText("—");
        if (lblLieu != null) lblLieu.setText("—");
        if (lblDebut != null) lblDebut.setText("—");
        if (lblFin != null) lblFin.setText("—");
        if (lblDescription != null) lblDescription.setText("");
        if (lblMeteo != null) lblMeteo.setText("");

        if (lblLatitude != null) lblLatitude.setText("");
        if (lblLongitude != null) lblLongitude.setText("");
        currentGeoPoint = null;

        if (imgEvent != null) imgEvent.setImage(null);
        if (imgQr != null) imgQr.setImage(null);
        if (lblQrInfo != null) lblQrInfo.setText("");
        if (lblImageIaMsg != null) lblImageIaMsg.setText("");

        if (progContainer != null) progContainer.getChildren().clear();

        if (boxEquipmentBanner != null) {
            boxEquipmentBanner.setVisible(false);
            boxEquipmentBanner.setManaged(false);
        }
        if (flowEquipment != null) flowEquipment.getChildren().clear();
        if (boxReservationMaquillage != null) {
            boxReservationMaquillage.setVisible(false);
            boxReservationMaquillage.setManaged(false);
        }

        hideSpotifyBox();
    }

    private void loadProgrammes() {
        if (programmeService == null) {
            showError("Service Programme non initialisé.");
            return;
        }
        if (eventId <= 0) {
            showError("ID événement invalide.");
            return;
        }
        if (progContainer == null) return;

        try {
            List<Programme> list = programmeService.getByEventId(eventId);

            list = list.stream()
                    .sorted(Comparator.comparing(p -> p.getDebut() == null ? LocalDateTime.MAX : p.getDebut()))
                    .collect(Collectors.toList());

            progContainer.getChildren().clear();

            if (list.isEmpty()) {
                if (lblMsg != null) lblMsg.setText("ℹ️ Aucun programme pour cet événement");
                return;
            }

            for (Programme p : list) {
                progContainer.getChildren().add(createProgrammeRow(p));
            }

            if (lblMsg != null) lblMsg.setText("✅ " + list.size() + " programme(s)");

        } catch (SQLException e) {
            showError("Erreur chargement programmes");
            e.printStackTrace();
        }
    }

    private HBox createProgrammeRow(Programme p) {
        String heure = (p.getDebut() == null) ? "—" : p.getDebut().format(H);

        Label lblTime = new Label(heure);
        lblTime.getStyleClass().add("prog-time");

        VBox timeBox = new VBox(lblTime);
        timeBox.setAlignment(Pos.TOP_CENTER);
        timeBox.getStyleClass().add("prog-time-box");

        Label title = new Label(safe(p.getTitre()));
        title.getStyleClass().add("prog-title");

        String range = "";
        if (p.getDebut() != null && p.getFin() != null) {
            range = p.getDebut().format(H) + " - " + p.getFin().format(H);
        } else if (p.getDebut() != null) {
            range = p.getDebut().format(H);
        }

        Label timeRange = new Label(range);
        timeRange.getStyleClass().add("prog-range");

        // Hint (admin)
        Label hint = new Label(Session.isAdmin() ? "Double-clic pour supprimer" : "");
        hint.getStyleClass().add("prog-hint");
        hint.setVisible(Session.isAdmin());
        hint.setManaged(Session.isAdmin());

        VBox details = new VBox(4, title, timeRange, hint);
        details.getStyleClass().add("prog-card-content");
        HBox.setHgrow(details, Priority.ALWAYS);

        Region colorBar = new Region();
        colorBar.getStyleClass().add("prog-color-bar");

        HBox card = new HBox(12, colorBar, details);
        card.getStyleClass().add("prog-card");
        HBox.setHgrow(card, Priority.ALWAYS);

        // delete on double click (admin only)
        card.setOnMouseClicked(ev -> {
            if (ev.getClickCount() == 2 && Session.isAdmin()) {
                onDeleteProgramme(p);
            }
        });

        HBox row = new HBox(12, timeBox, card);
        row.setAlignment(Pos.TOP_LEFT);
        row.getStyleClass().add("prog-row");
        HBox.setHgrow(card, Priority.ALWAYS);

        return row;
    }

    private void onDeleteProgramme(Programme p) {
        Alert a = new Alert(Alert.AlertType.CONFIRMATION);
        a.setTitle("Confirmation");
        a.setHeaderText("Supprimer ce programme ?");
        a.setContentText(safe(p.getTitre()));

        a.showAndWait().ifPresent(btn -> {
            if (btn == ButtonType.OK) {
                try {
                    programmeService.delete(p.getIdProg());
                    if (lblMsg != null) lblMsg.setText("✅ Programme supprimé");
                    loadProgrammes();
                } catch (SQLException e) {
                    showError("Erreur suppression programme");
                    e.printStackTrace();
                }
            }
        });
    }

    // ===================== ✅ GOOGLE CALENDAR =====================
    @FXML
    private void onAddToGoogleCalendar() {
        if (event == null) {
            showError("Aucun événement chargé.");
            return;
        }

        try {
            String title = safe(event.getTitre());
            String location = safe(event.getLieu());
            String details = safe(event.getDescription());

            String datesParam = "";
            if (event.getDateDebut() != null && event.getDateFin() != null) {
                DateTimeFormatter gc = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss");
                String start = event.getDateDebut().format(gc);
                String end = event.getDateFin().format(gc);
                datesParam = "&dates=" + enc(start + "/" + end);
            }

            String url = "https://calendar.google.com/calendar/render?action=TEMPLATE"
                    + "&text=" + enc(title)
                    + "&location=" + enc(location)
                    + "&details=" + enc(details)
                    + datesParam;

            openUrl(url);
            if (lblMsg != null) lblMsg.setText("✅ Ouverture Google Calendar...");
        } catch (Exception ex) {
            ex.printStackTrace();
            showError("Impossible d'ouvrir Google Calendar.");
        }
    }

    private static String enc(String s) {
        return URLEncoder.encode(s == null ? "" : s, StandardCharsets.UTF_8);
    }

    private static void openUrl(String url) throws Exception {
        if (Desktop.isDesktopSupported()) {
            Desktop.getDesktop().browse(new URI(url));
        } else {
            Runtime.getRuntime().exec(new String[]{"cmd", "/c", "start", url});
        }
    }

    // ===================== ✅ NOTIF =====================
    @FXML
    private void onToggleNotification() {
        notificationsEnabled = !notificationsEnabled;
        updateNotifButtonText();

        if (lblMsg != null) {
            lblMsg.setText(notificationsEnabled ? "🔔 Notification activée (démo)." : "🔕 Notification désactivée (démo).");
        }
    }

    private void updateNotifButtonText() {
        if (btnToggleNotif == null) return;
        btnToggleNotif.setText(notificationsEnabled ? "Désactiver notification" : "Activer notification");
    }

    // ===================== ✅ QR =====================
    @FXML
    private void onGenererQr() {
        if (event == null) return;

        String titre = safe(event.getTitre());
        String type  = safe(event.getType());
        String lieu  = safe(event.getLieu());
        String debut = (event.getDateDebut() == null) ? "—" : event.getDateDebut().format(F);

        String desc = safe(event.getDescription());
        if (desc.length() > 180) desc = desc.substring(0, 180) + "...";

        String message =
                "LAMMA EVENT\n" +
                        "━━━━━━━━━━━━━━━━━━\n" +
                        "Titre : " + titre + "\n" +
                        "Type  : " + type + "\n" +
                        "Lieu  : " + lieu + "\n" +
                        "Début : " + debut + "\n" +
                        "━━━━━━━━━━━━━━━━━━\n" +
                        "Description :\n" +
                        desc + "\n" +
                        "━━━━━━━━━━━━━━━━━━\n" +
                        "Plus de détails : Ouvrir l'app LAMMA";

        String encoded = URLEncoder.encode(message, StandardCharsets.UTF_8);
        String url = "https://api.qrserver.com/v1/create-qr-code/?size=320x320&margin=12&data=" + encoded;

        if (imgQr != null) imgQr.setImage(new Image(url, true));
        if (lblQrInfo != null) lblQrInfo.setText("✓ QR généré");
    }

    // ===================== ✅ METEO =====================
    private void loadMeteoAsync() {
        if (lblMeteo == null || event == null || event.getDateDebut() == null) return;

        lblMeteo.setText("⏳ Chargement météo...");

        String city = safe(event.getLieu());
        String dt = event.getDateDebut().toString();

        Task<services.WeatherService.WeatherInfo> task = new Task<>() {
            @Override
            protected services.WeatherService.WeatherInfo call() throws Exception {
                return weatherService.getWeatherForCityAtHour(city, dt);
            }
        };

        task.setOnSucceeded(e -> {
            var w = task.getValue();
            String text = String.format("🌡 %.0f°C  ☔ %.1fmm  💨 %.0f km/h (%s)",
                    w.temperatureC, w.precipitationMm, w.windKmh, w.locationName);
            lblMeteo.setText(text);
        });

        task.setOnFailed(e -> lblMeteo.setText("❌ Météo indisponible"));

        Thread th = new Thread(task, "meteo-task");
        th.setDaemon(true);
        th.start();
    }

    // ===================== ✅ CITATION =====================
    private void loadQuoteAsync() {
        if (lblQuote == null) return;

        lblQuote.setText("⏳ Chargement citation...");
        if (lblQuoteAuthor != null) lblQuoteAuthor.setText("");

        Task<services.QuoteService.Quote> task = new Task<>() {
            @Override
            protected services.QuoteService.Quote call() throws Exception {
                return quoteService.getRandomQuote();
            }
        };

        task.setOnSucceeded(e -> {
            var q = task.getValue();
            if (q == null) {
                lblQuote.setText("❌ Citation indisponible");
                if (lblQuoteAuthor != null) lblQuoteAuthor.setText("");
                return;
            }
            lblQuote.setText("“ " + q.text + " ”");
            if (lblQuoteAuthor != null) {
                lblQuoteAuthor.setText(q.author == null || q.author.isBlank() ? "" : "— " + q.author);
            }
        });

        task.setOnFailed(e -> {
            lblQuote.setText("❌ Citation indisponible");
            if (lblQuoteAuthor != null) lblQuoteAuthor.setText("");
        });

        Thread th = new Thread(task, "quote-task");
        th.setDaemon(true);
        th.start();
    }

    // ===================== ✅ SPOTIFY =====================
    private void applySpotifyUI() {
        if (event == null) { hideSpotifyBox(); return; }

        String type = safe(event.getType()).toUpperCase();
        String url  = safe(event.getSpotifyUrl());

        boolean isSoiree = "SOIREE".equalsIgnoreCase(type.trim());
        boolean hasUrl   = !url.isBlank();

        if (!isSoiree || !hasUrl) {
            hideSpotifyBox();
            return;
        }

        if (spotifyBox != null) {
            spotifyBox.setVisible(true);
            spotifyBox.setManaged(true);
        }

        if (lblSpotifyMsg != null) lblSpotifyMsg.setText("⏳ Chargement playlist Spotify...");
        if (lblSpotifyTitle != null) lblSpotifyTitle.setText("");
        if (lblSpotifyProvider != null) lblSpotifyProvider.setText("Spotify");
        if (imgSpotifyCover != null) imgSpotifyCover.setImage(null);
        if (btnOpenSpotify != null) btnOpenSpotify.setDisable(false);

        loadSpotifyOEmbedAsync(url);
    }

    private void loadSpotifyOEmbedAsync(String spotifyUrl) {
        Task<SpotifyOEmbedService.SpotifyInfo> task = new Task<>() {
            @Override
            protected SpotifyOEmbedService.SpotifyInfo call() throws Exception {
                return spotifyService.fetchOEmbed(spotifyUrl);
            }
        };

        task.setOnSucceeded(e -> {
            SpotifyOEmbedService.SpotifyInfo info = task.getValue();
            if (info == null) {
                if (lblSpotifyMsg != null) lblSpotifyMsg.setText("❌ Spotify indisponible");
                return;
            }

            if (lblSpotifyMsg != null) lblSpotifyMsg.setText("✓ Playlist associée à l'événement");
            if (lblSpotifyTitle != null) lblSpotifyTitle.setText(safe(info.title));
            if (lblSpotifyProvider != null) lblSpotifyProvider.setText(safe(info.providerName));

            if (imgSpotifyCover != null && info.thumbnailUrl != null && !info.thumbnailUrl.isBlank()) {
                loadSpotifyCoverAsync(info.thumbnailUrl);
            } else {
                if (lblSpotifyMsg != null) lblSpotifyMsg.setText("⚠️ Cover Spotify introuvable");
            }
        });

        task.setOnFailed(e -> {
            if (lblSpotifyMsg != null) lblSpotifyMsg.setText("❌ Erreur Spotify");
            task.getException().printStackTrace();
        });

        Thread th = new Thread(task, "spotify-oembed-task");
        th.setDaemon(true);
        th.start();
    }

    private void loadSpotifyCoverAsync(String imageUrl) {
        Task<Image> task = new Task<>() {
            @Override
            protected Image call() throws Exception {
                HttpRequest req = HttpRequest.newBuilder()
                        .uri(URI.create(imageUrl))
                        .GET()
                        .header("User-Agent", "JavaFX")
                        .build();

                HttpResponse<byte[]> resp = http.send(req, HttpResponse.BodyHandlers.ofByteArray());
                if (resp.statusCode() != 200) return null;

                return new Image(new ByteArrayInputStream(resp.body()));
            }
        };

        task.setOnSucceeded(e -> {
            Image img = task.getValue();
            if (img != null && imgSpotifyCover != null) {
                imgSpotifyCover.setImage(img);
            } else if (lblSpotifyMsg != null) {
                lblSpotifyMsg.setText("⚠️ Cover Spotify indisponible");
            }
        });

        task.setOnFailed(e -> {
            if (lblSpotifyMsg != null) lblSpotifyMsg.setText("❌ Erreur chargement cover");
            task.getException().printStackTrace();
        });

        Thread th = new Thread(task, "spotify-cover-task");
        th.setDaemon(true);
        th.start();
    }

    private void hideSpotifyBox() {
        if (spotifyBox != null) {
            spotifyBox.setVisible(false);
            spotifyBox.setManaged(false);
        }
        if (lblSpotifyMsg != null) lblSpotifyMsg.setText("");
        if (lblSpotifyTitle != null) lblSpotifyTitle.setText("");
        if (lblSpotifyProvider != null) lblSpotifyProvider.setText("");
        if (btnOpenSpotify != null) btnOpenSpotify.setDisable(true);
        if (imgSpotifyCover != null) imgSpotifyCover.setImage(null);
    }

    @FXML
    private void onOpenSpotify() {
        if (event == null) return;

        String url = safe(event.getSpotifyUrl());
        if (url.isBlank()) {
            if (lblSpotifyMsg != null) lblSpotifyMsg.setText("❌ Aucune URL Spotify");
            return;
        }

        try {
            if (Desktop.isDesktopSupported()) {
                Desktop.getDesktop().browse(new URI(url));
            } else {
                if (lblSpotifyMsg != null) lblSpotifyMsg.setText("❌ Desktop non supporté");
            }
        } catch (Exception ex) {
            if (lblSpotifyMsg != null) lblSpotifyMsg.setText("❌ Impossible d'ouvrir Spotify");
            ex.printStackTrace();
        }
    }

    // ===================== NAV =====================
    @FXML
    private void onAjouterProgramme() {
        SceneUtil.switchToWithData("/AjouterProgramme.fxml", "Ajouter Programme", eventId);
    }

    @FXML
    private void onModifierEvenement() {
        if (eventId <= 0 || event == null) {
            showError("Aucun événement chargé.");
            return;
        }
        SceneUtil.switchToWithData("/ModifierEvenement.fxml", "Modifier Événement", eventId);
    }

    @FXML
    private void onSupprimerProgramme() {
        if (lblMsg != null) lblMsg.setText("ℹ️ Double-clic sur un programme pour le supprimer.");
    }

    @FXML
    private void onRetour() {
        SceneUtil.switchTo("/ListeEvenements.fxml", "Liste Événements");
    }

    // ===================== HELPERS =====================
    private void showError(String msg) {
        if (lblMsg != null) lblMsg.setText("❌ " + msg);
    }

    private String safe(String s) {
        return s == null ? "" : s.trim();
    }
}