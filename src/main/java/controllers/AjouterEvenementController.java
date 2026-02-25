package controllers;

import entities.Evenement;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.FileChooser;
import services.EvenementService;
import services.EventImageApi;
import services.ImageAiService;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

public class AjouterEvenementController {

    @FXML private TextField txtTitre;
    @FXML private TextArea txtDesc;
    @FXML private ComboBox<String> cbType;

    @FXML private DatePicker dpDebut;
    @FXML private Spinner<Integer> spDebutH;
    @FXML private Spinner<Integer> spDebutM;

    @FXML private DatePicker dpFin;
    @FXML private Spinner<Integer> spFinH;
    @FXML private Spinner<Integer> spFinM;

    @FXML private TextField txtLieu;

    // ✅ NEW
    @FXML private TextField txtSpotifyUrl;

    @FXML private Label lblMsg;
    @FXML private Label lblImageName;

    @FXML private ImageView imgPreview;
    @FXML private ImageView bgImage;
    @FXML private ImageView imgLogo;

    private final EvenementService service = new EvenementService();
    private final EventImageApi imageApi = new EventImageApi();

    private String imageFileName = "logo.png";
    private static final Path UPLOAD_DIR = Paths.get(System.getProperty("user.dir"), "uploads", "images");

    @FXML
    public void initialize() {
        SceneUtil.loadBackgroundImage(bgImage);
        SceneUtil.loadLogoImage(imgLogo);

        cbType.getItems().setAll("SOIREE", "RANDONNEE", "CAMPING", "SEJOUR");
        cbType.setValue("SOIREE");

        spDebutH.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 23, 9));
        spDebutM.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 59, 0));
        spFinH.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 23, 10));
        spFinM.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 59, 0));

        spDebutH.setEditable(true);
        spDebutM.setEditable(true);
        spFinH.setEditable(true);
        spFinM.setEditable(true);

        dpDebut.setValue(LocalDate.now());
        dpFin.setValue(LocalDate.now());

        if (lblImageName != null) lblImageName.setText(imageFileName);
        showInfo("");
    }

    @FXML
    private void onChooseImage(ActionEvent event) {
        try {
            Files.createDirectories(UPLOAD_DIR);

            FileChooser fc = new FileChooser();
            fc.setTitle("Choisir une image");
            fc.getExtensionFilters().addAll(
                    new FileChooser.ExtensionFilter("Images", "*.png", "*.jpg", "*.jpeg", "*.webp")
            );

            File chosen = fc.showOpenDialog(txtTitre.getScene().getWindow());
            if (chosen == null) return;

            String ext = getExt(chosen.getName());
            String safeBase = safe(txtTitre.getText()).replaceAll("[^a-zA-Z0-9-_]", "_");
            if (safeBase.isBlank()) safeBase = "event";

            String newName = safeBase + "_" + System.currentTimeMillis() + ext;
            Path target = UPLOAD_DIR.resolve(newName);

            Files.copy(chosen.toPath(), target, StandardCopyOption.REPLACE_EXISTING);

            imageFileName = newName;
            if (lblImageName != null) lblImageName.setText(newName);

            if (imgPreview != null) {
                imgPreview.setImage(new Image(target.toUri().toString(), true));
            }

            showSuccess("✅ Image sélectionnée");

        } catch (IOException ex) {
            showError("❌ Erreur upload image");
            ex.printStackTrace();
        }
    }

    @FXML
    private void onGenerateImage(ActionEvent event) {
        String titre = safe(txtTitre.getText());
        String desc = safe(txtDesc.getText());
        String type = cbType.getValue() == null ? "" : cbType.getValue();
        String lieu = safe(txtLieu.getText());

        if (titre.isBlank()) {
            showError("❌ Mets au moins un titre pour générer l'image.");
            return;
        }

        showInfo("⏳ Génération image IA ...");

        Task<ImageAiService.GeneratedImage> task = new Task<>() {
            @Override
            protected ImageAiService.GeneratedImage call() throws Exception {
                return imageApi.generateForEvent(titre, desc, type, lieu);
            }
        };

        task.setOnSucceeded(e -> {
            ImageAiService.GeneratedImage gen = task.getValue();
            imageFileName = gen.fileName;

            if (lblImageName != null) lblImageName.setText(gen.fileName);

            if (imgPreview != null && gen.bytes != null) {
                Image img = new Image(new ByteArrayInputStream(gen.bytes));
                imgPreview.setImage(img);
            }

            showSuccess("✅ Image IA générée et enregistrée");
        });

        task.setOnFailed(e -> {
            Throwable ex = task.getException();
            String msg = (ex == null) ? "Erreur inconnue" : ex.getMessage();
            showError("❌ Erreur génération IA: " + msg);
            if (ex != null) ex.printStackTrace();
        });

        Thread th = new Thread(task, "ai-image-generation");
        th.setDaemon(true);
        th.start();
    }

    @FXML
    private void onAjouterEvenement(ActionEvent event) {

        String titre = safe(txtTitre.getText());
        String desc = safe(txtDesc.getText());
        String type = cbType.getValue();
        String lieu = safe(txtLieu.getText());

        // ✅ NEW
        String spotifyUrl = safe(txtSpotifyUrl == null ? "" : txtSpotifyUrl.getText());
        if (!spotifyUrl.isBlank() && !(spotifyUrl.startsWith("https://open.spotify.com/"))) {
            showError("❌ Lien Spotify invalide (ex: https://open.spotify.com/playlist/...)");
            return;
        }

        if (titre.isBlank() || titre.length() < 3) { showError("❌ Titre invalide (min 3)."); return; }
        if (desc.isBlank() || desc.length() < 3) { showError("❌ Description invalide (min 3)."); return; }
        if (lieu.isBlank() || lieu.length() < 2) { showError("❌ Lieu invalide (min 2)."); return; }
        if (type == null || type.isBlank()) { showError("❌ Type obligatoire."); return; }
        if (dpDebut.getValue() == null) { showError("❌ Date début obligatoire."); return; }

        LocalDateTime dateDebut = LocalDateTime.of(
                dpDebut.getValue(),
                LocalTime.of(spDebutH.getValue(), spDebutM.getValue())
        );

        LocalDateTime dateFin = null;
        if (dpFin.getValue() != null) {
            dateFin = LocalDateTime.of(
                    dpFin.getValue(),
                    LocalTime.of(spFinH.getValue(), spFinM.getValue())
            );
            if (!dateFin.isAfter(dateDebut)) {
                showError("❌ Date fin doit être après date début.");
                return;
            }
        }

        Evenement ev = new Evenement();
        ev.setTitre(titre);
        ev.setDescription(desc);
        ev.setType(type);
        ev.setDateDebut(dateDebut);
        ev.setDateFin(dateFin);
        ev.setLieu(lieu);
        ev.setImage(imageFileName);

        // ✅ NEW
        ev.setSpotifyUrl(spotifyUrl.isBlank() ? null : spotifyUrl);

        try {
            service.add(ev);
            showSuccess("✅ Événement ajouté !");
        } catch (Exception ex) {
            showError("❌ Erreur DB ajout événement");
            ex.printStackTrace();
            return;
        }

        Alert a = new Alert(Alert.AlertType.CONFIRMATION);
        a.setTitle("Événement ajouté");
        a.setHeaderText("✅ Événement ajouté !");
        a.setContentText("Voulez-vous ajouter un programme à cet événement ?");

        a.showAndWait().ifPresent(btn -> {
            if (btn == ButtonType.OK) {
                if (ev.getIdEvent() > 0) {
                    SceneUtil.switchToWithData("/AjouterProgramme.fxml", "Ajouter Programme", ev.getIdEvent());
                } else {
                    SceneUtil.switchTo("/ListeEvenements.fxml", "Liste des Événements");
                }
            } else {
                SceneUtil.switchTo("/ListeEvenements.fxml", "Liste des Événements");
            }
        });
    }

    @FXML
    private void onRetour(ActionEvent event) {
        SceneUtil.switchTo("/ListeEvenements.fxml", "Liste des Événements");
    }

    // ===================== HELPERS =====================

    private String safe(String s) { return s == null ? "" : s.trim(); }

    private String getExt(String name) {
        int i = name.lastIndexOf('.');
        if (i == -1) return ".png";
        return name.substring(i).toLowerCase();
    }

    private void showSuccess(String msg) { if (lblMsg != null) lblMsg.setText(msg); }
    private void showError(String msg) { if (lblMsg != null) lblMsg.setText(msg); }
    private void showInfo(String msg) { if (lblMsg != null) lblMsg.setText(msg); }
}