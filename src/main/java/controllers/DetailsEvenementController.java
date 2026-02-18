package controllers;

import entities.Evenement;
import entities.Programme;
import interfaces.DataReceiver;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.layout.*;
import services.EvenementService;
import services.ProgrammeService;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;

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

    // ✅ conteneur planning (VBox dans ScrollPane)
    @FXML private VBox progContainer;

    private EvenementService evenementService;
    private ProgrammeService programmeService;

    private int eventId;
    private Evenement event;

    @FXML
    public void initialize() {
        try {
            evenementService = new EvenementService();
            programmeService = new ProgrammeService();
        } catch (SQLException e) {
            showError("Erreur connexion DB");
            e.printStackTrace();
        }
    }

    /** ✅ reçoit eventId depuis SceneUtil.switchToWithData */
    @Override
    public void setData(Integer id) {
        if (id == null) {
            showError("Aucun ID reçu");
            return;
        }
        this.eventId = id;

        loadDetails();
        loadProgrammes();
    }

    private void loadDetails() {
        try {
            event = evenementService.getOneById(eventId);

            if (event == null) {
                showError("Événement introuvable");
                return;
            }

            lblTitre.setText(nullSafe(event.getTitre()));
            lblType.setText(nullSafe(event.getType()));
            lblLieu.setText(nullSafe(event.getLieu()));
            lblDebut.setText(event.getDateDebut() == null ? "" : event.getDateDebut().format(F));
            lblFin.setText(event.getDateFin() == null ? "—" : event.getDateFin().format(F));
            lblDescription.setText(nullSafe(event.getDescription()));

        } catch (SQLException e) {
            showError("Erreur chargement détails");
            e.printStackTrace();
        }
    }

    private void loadProgrammes() {
        try {
            List<Programme> list = programmeService.getByEventId(eventId);

            // ✅ tri chrono (planning)
            list = list.stream()
                    .sorted(Comparator.comparing(
                            p -> p.getDebut() == null ? LocalDateTime.MAX : p.getDebut()
                    ))
                    .toList();

            progContainer.getChildren().clear();

            if (list.isEmpty()) {
                lblMsg.setText("ℹ️ Aucun programme pour cet événement");
                return;
            }

            for (Programme p : list) {
                progContainer.getChildren().add(createProgrammeRow(p));
            }

            lblMsg.setText("✅ " + list.size() + " programme(s)");

        } catch (SQLException e) {
            showError("Erreur chargement programmes");
            e.printStackTrace();
        }
    }

    /** ✅ Une ligne planning : [heure] | [carte avec barre colorée] */
    private HBox createProgrammeRow(Programme p) {

        // ---- Colonne heure (gauche) ----
        String heure = (p.getDebut() == null) ? "—" : p.getDebut().format(H);

        Label lblTime = new Label(heure);
        lblTime.getStyleClass().add("prog-time");

        VBox timeBox = new VBox(lblTime);
        timeBox.setAlignment(Pos.TOP_CENTER);
        timeBox.setPrefWidth(90);
        timeBox.getStyleClass().add("prog-time-box");

        // ---- Carte (droite) ----
        Label title = new Label(nullSafe(p.getTitre()));
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

        // Double clic => supprimer programme
        card.setOnMouseClicked(ev -> {
            if (ev.getClickCount() == 2) {
                onDeleteProgramme(p);
            }
        });

        // ---- Ligne complète ----
        HBox row = new HBox(12, timeBox, card);
        row.setAlignment(Pos.TOP_LEFT);
        row.getStyleClass().add("prog-row");

        return row;
    }

    private void onDeleteProgramme(Programme p) {
        Alert a = new Alert(Alert.AlertType.CONFIRMATION);
        a.setTitle("Confirmation");
        a.setHeaderText("Supprimer ce programme ?");
        a.setContentText(nullSafe(p.getTitre()));

        a.showAndWait().ifPresent(btn -> {
            if (btn == ButtonType.OK) {
                try {
                    programmeService.delete(p.getIdProg());
                    lblMsg.setText("✅ Programme supprimé");
                    loadProgrammes();
                } catch (SQLException e) {
                    showError("Erreur suppression");
                    e.printStackTrace();
                }
            }
        });
    }

    @FXML
    private void onAjouterProgramme() {
        SceneUtil.switchToWithData("/AjouterProgramme.fxml", "Ajouter Programme", eventId);
    }

    // ✅ bouton "Modifier" dans ton FXML
    @FXML
    private void onModifierEvenement() {
        SceneUtil.switchToWithData("/ModifierEvenement.fxml", "Modifier Événement", eventId);
    }

    // ✅ bouton "Supprimer Programme" dans ton FXML (optionnel)
    @FXML
    private void onSupprimerProgramme() {
        lblMsg.setText("ℹ️ Double-clic sur un programme pour le supprimer.");
    }

    @FXML
    private void onRetour() {
        SceneUtil.switchTo("/ListeEvenements.fxml", "Liste Événements");
    }

    // ===== helpers =====
    private void showError(String msg) {
        lblMsg.setText("❌ " + msg);
    }

    private String nullSafe(String s) {
        return s == null ? "" : s;
    }
}