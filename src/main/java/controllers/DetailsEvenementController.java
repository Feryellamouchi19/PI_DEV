package controllers;

import entities.Evenement;
import entities.Programme;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import services.EvenementService;
import services.ProgrammeService;

import java.sql.SQLException;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class DetailsEvenementController implements SceneUtil.DataReceiver<Integer> {

    private final DateTimeFormatter F = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    @FXML private Label lblTitre;
    @FXML private Label lblType;
    @FXML private Label lblLieu;
    @FXML private Label lblDebut;
    @FXML private Label lblFin;
    @FXML private Label lblDescription;

    @FXML private TableView<Programme> tableProg;
    @FXML private TableColumn<Programme, Integer> colProgId;
    @FXML private TableColumn<Programme, String> colProgTitre;
    @FXML private TableColumn<Programme, String> colProgDebut;
    @FXML private TableColumn<Programme, String> colProgFin;

    @FXML private Label lblMsg;

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
            lblMsg.setText("❌ Erreur connexion DB");
            e.printStackTrace();
        }

        colProgId.setCellValueFactory(d -> new javafx.beans.property.SimpleIntegerProperty(d.getValue().getIdProg()).asObject());
        colProgTitre.setCellValueFactory(d -> new javafx.beans.property.SimpleStringProperty(d.getValue().getTitre()));
        colProgDebut.setCellValueFactory(d -> new javafx.beans.property.SimpleStringProperty(
                d.getValue().getDebut() == null ? "" : d.getValue().getDebut().format(F)
        ));
        colProgFin.setCellValueFactory(d -> new javafx.beans.property.SimpleStringProperty(
                d.getValue().getFin() == null ? "" : d.getValue().getFin().format(F)
        ));
    }

    /** reçoit eventId depuis la liste */
    @Override
    public void initData(Integer id) {
        this.eventId = id;
        loadDetails();
        loadProgrammes();
    }

    private void loadDetails() {
        try {
            event = evenementService.getOneById(eventId);
            if (event == null) {
                lblMsg.setText("❌ Événement introuvable");
                return;
            }

            lblTitre.setText(event.getTitre());
            lblType.setText(event.getType());
            lblLieu.setText(event.getLieu());
            lblDebut.setText(event.getDateDebut() == null ? "" : event.getDateDebut().format(F));
            lblFin.setText(event.getDateFin() == null ? "—" : event.getDateFin().format(F));
            lblDescription.setText(event.getDescription());

        } catch (SQLException e) {
            lblMsg.setText("❌ Erreur chargement détails");
            e.printStackTrace();
        }
    }

    private void loadProgrammes() {
        try {
            List<Programme> list = programmeService.getByEventId(eventId);
            tableProg.getItems().setAll(list);
            lblMsg.setText("✅ " + list.size() + " programme(s)");
        } catch (SQLException e) {
            lblMsg.setText("❌ Erreur chargement programmes");
            e.printStackTrace();
        }
    }

    @FXML
    private void onAjouterProgramme() {
        // Aller vers AjouterProgramme en passant eventId
        SceneUtil.switchToWithData("/AjouterProgramme.fxml", "Ajouter Programme", eventId);
    }

    @FXML
    private void onSupprimerProgramme() {
        Programme sel = tableProg.getSelectionModel().getSelectedItem();
        if (sel == null) {
            lblMsg.setText("❌ Sélectionne un programme");
            return;
        }

        Alert a = new Alert(Alert.AlertType.CONFIRMATION);
        a.setTitle("Confirmation");
        a.setHeaderText("Supprimer ce programme ?");
        a.setContentText(sel.getTitre());

        a.showAndWait().ifPresent(btn -> {
            if (btn == ButtonType.OK) {
                try {
                    programmeService.delete(sel.getIdProg());
                    lblMsg.setText("✅ Programme supprimé");
                    loadProgrammes();
                } catch (SQLException e) {
                    lblMsg.setText("❌ Erreur suppression");
                    e.printStackTrace();
                }
            }
        });
    }

    @FXML
    private void onRetour() {
        SceneUtil.switchTo("/ListeEvenements.fxml", "Liste Evenements");
    }
}