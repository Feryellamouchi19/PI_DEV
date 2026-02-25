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
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import services.EvenementService;
import services.ProgrammeService;

import java.awt.Desktop;
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
import java.util.stream.Collectors;

public class DetailsEvenementController implements DataReceiver<Integer> {

    private final DateTimeFormatter F = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private final DateTimeFormatter H = DateTimeFormatter.ofPattern("HH:mm");

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

    @FXML private VBox progContainer;

    // ✅ QR
    @FXML private ImageView imgQr;
    @FXML private Label lblQrInfo;
    @FXML private Button btnRegenQr;

    // ✅ Meteo
    @FXML private Label lblMeteo;
    private final services.WeatherService weatherService = new services.WeatherService();

    // ✅ Spotify (doit matcher ton FXML)
    @FXML private VBox spotifyBox;
    @FXML private Label lblSpotifyMsg;
    @FXML private ImageView imgSpotifyCover;
    @FXML private Label lblSpotifyTitle;
    @FXML private Label lblSpotifyProvider;
    @FXML private Button btnOpenSpotify;

    // ✅ Admin buttons (pour cacher en mode USER)
    @FXML private HBox adminButtons;

    private final EvenementService evenementService = new EvenementService();
    private ProgrammeService programmeService;

    private int eventId = 0;
    private Evenement event;

    @FXML
    public void initialize() {
        SceneUtil.loadBackgroundImage(bgImage);
        SceneUtil.loadLogoImage(imgLogo);
        programmeService = new ProgrammeService();

        if (lblMsg != null) lblMsg.setText("");
        if (lblQrInfo != null) lblQrInfo.setText("");
        if (lblMeteo != null) lblMeteo.setText("");

        // Par défaut cacher spotify tant qu’on n’a pas chargé l’event
        hideSpotifyBox();
    }

    @Override
    public void setData(Integer id) {
        if (id == null || id <= 0) {
            showError("Aucun ID reçu / ID invalide");
            clearDetails();
            return;
        }
        this.eventId = id;

        loadDetails();
        loadProgrammes();
    }

    private void loadDetails() {
        event = evenementService.getOneById(eventId);

        if (event == null) {
            showError("Événement introuvable");
            clearDetails();
            return;
        }

        lblTitre.setText(safe(event.getTitre()));
        lblType.setText(safe(event.getType()));
        lblLieu.setText(safe(event.getLieu()));
        lblDebut.setText(event.getDateDebut() == null ? "—" : event.getDateDebut().format(F));
        lblFin.setText(event.getDateFin() == null ? "—" : event.getDateFin().format(F));
        lblDescription.setText(safe(event.getDescription()));

        loadEventImage(event.getImage());

        // ✅ QR auto
        onGenererQr();

        // ✅ Météo auto
        loadMeteoAsync();

        // ✅ Spotify auto (IMPORTANT)
        applySpotifyUI();
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

    private void clearDetails() {
        lblTitre.setText("—");
        lblType.setText("—");
        lblLieu.setText("—");
        lblDebut.setText("—");
        lblFin.setText("—");
        lblDescription.setText("");
        if (lblMeteo != null) lblMeteo.setText("");

        if (imgEvent != null) imgEvent.setImage(null);
        if (imgQr != null) imgQr.setImage(null);
        if (lblQrInfo != null) lblQrInfo.setText("");

        if (progContainer != null) progContainer.getChildren().clear();

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
        timeBox.setPrefWidth(90);
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

        VBox details = new VBox(4, title, timeRange);
        details.getStyleClass().add("prog-card-content");

        Region colorBar = new Region();
        colorBar.getStyleClass().add("prog-color-bar");

        HBox card = new HBox(colorBar, details);
        card.getStyleClass().add("prog-card");
        HBox.setHgrow(details, Priority.ALWAYS);

        card.setOnMouseClicked(ev -> {
            if (ev.getClickCount() == 2) {
                onDeleteProgramme(p);
            }
        });

        HBox row = new HBox(12, timeBox, card);
        row.setAlignment(Pos.TOP_LEFT);
        row.getStyleClass().add("prog-row");

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

    // ===================== ✅ QR CODE (sans ID) =====================

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

    // ===================== ✅ SPOTIFY (FIX) =====================

    private void applySpotifyUI() {
        if (event == null) { hideSpotifyBox(); return; }

        String type = safe(event.getType()).toUpperCase(); // important
        String url  = safe(event.getSpotifyUrl());          // IMPORTANT

        // DEBUG (tu peux laisser pour vérifier)
        System.out.println("DEBUG type=" + type + " spotifyUrl=" + url);

        boolean isSoiree = "SOIREE".equalsIgnoreCase(type.trim());
        boolean hasUrl   = !url.isBlank();

        // on affiche seulement si SOIREE + URL
        if (!isSoiree || !hasUrl) {
            hideSpotifyBox();
            return;
        }

        // ✅ rendre visible + managed (sinon il ne prend pas de place)
        if (spotifyBox != null) {
            spotifyBox.setVisible(true);
            spotifyBox.setManaged(true);
        }

        if (lblSpotifyMsg != null) lblSpotifyMsg.setText("Playlist associée à l'événement");
        if (lblSpotifyTitle != null) lblSpotifyTitle.setText("Playlist Spotify");
        if (lblSpotifyProvider != null) lblSpotifyProvider.setText(url);

        // ✅ cover (option: mets /images/spotify.png)
        if (imgSpotifyCover != null) {
            try (InputStream is = getClass().getResourceAsStream("/images/spotify.png")) {
                if (is != null) imgSpotifyCover.setImage(new Image(is));
            } catch (Exception ignored) {}
        }

        // bouton
        if (btnOpenSpotify != null) btnOpenSpotify.setDisable(false);
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