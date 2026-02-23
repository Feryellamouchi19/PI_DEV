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

    @FXML private VBox progContainer;

    private final EvenementService evenementService = new EvenementService();
    private ProgrammeService programmeService; // ✅ plus besoin try/catch constructeur

    private int eventId = 0;
    private Evenement event;

    @FXML
    public void initialize() {
        // ✅ ProgrammeService() ne throw plus
        programmeService = new ProgrammeService();
        lblMsg.setText("");
    }

    /** reçoit eventId depuis SceneUtil.switchToWithData */
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
    }

    private void clearDetails() {
        lblTitre.setText("—");
        lblType.setText("—");
        lblLieu.setText("—");
        lblDebut.setText("—");
        lblFin.setText("—");
        lblDescription.setText("");
        if (progContainer != null) progContainer.getChildren().clear();
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

            // ✅ tri chrono (compatible Java 8/11)
            list = list.stream()
                    .sorted(Comparator.comparing(p -> p.getDebut() == null ? LocalDateTime.MAX : p.getDebut()))
                    .collect(Collectors.toList());

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

    /** Une ligne planning : [heure] | [carte] */
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

        // Double clic => supprimer
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
        if (programmeService == null) {
            showError("Service Programme non initialisé.");
            return;
        }

        Alert a = new Alert(Alert.AlertType.CONFIRMATION);
        a.setTitle("Confirmation");
        a.setHeaderText("Supprimer ce programme ?");
        a.setContentText(safe(p.getTitre()));

        a.showAndWait().ifPresent(btn -> {
            if (btn == ButtonType.OK) {
                try {
                    programmeService.delete(p.getIdProg());
                    lblMsg.setText("✅ Programme supprimé");
                    loadProgrammes();
                } catch (SQLException e) {
                    showError("Erreur suppression programme");
                    e.printStackTrace();
                }
            }
        });
    }

    @FXML
    private void onAjouterProgramme() {
        SceneUtil.switchToWithData("/AjouterProgramme.fxml", "Ajouter Programme", eventId);
    }

    @FXML
    private void onModifierEvenement() {
        SceneUtil.switchToWithData("/ModifierEvenement.fxml", "Modifier Événement", eventId);
    }

    @FXML
    private void onSupprimerProgramme() {
        lblMsg.setText("ℹ️ Double-clic sur un programme pour le supprimer.");
    }

    @FXML
    private void onRetour() {
        SceneUtil.switchTo("/ListeEvenements.fxml", "Liste Événements");
    }

    private void showError(String msg) {
        lblMsg.setText("❌ " + msg);
    }

    private String safe(String s) {
        return s == null ? "" : s.trim();
    }
}