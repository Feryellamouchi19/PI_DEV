package controllers;
import interfaces.DataReceiver;

import entities.Evenement;
import entities.Programme;
import interfaces.DataReceiver;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import services.EvenementService;
import services.ProgrammeService;

import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

public class AjouterProgrammeController implements DataReceiver<Integer> {

    @FXML private Label lblEventInfo;

    @FXML private TextField txtTitre;

    @FXML private DatePicker dpDebut;
    @FXML private Spinner<Integer> spDebutH;
    @FXML private Spinner<Integer> spDebutM;

    @FXML private DatePicker dpFin;
    @FXML private Spinner<Integer> spFinH;
    @FXML private Spinner<Integer> spFinM;

    @FXML private Label lblMessage;

    private ProgrammeService programmeService;
    private EvenementService evenementService;

    private int idEvent; // ✅ reçu depuis SceneUtil

    @Override
    public void setData(Integer data) {
        this.idEvent = data;
        chargerInfosEvenement();
    }

    @FXML
    public void initialize() {
        // spinners
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

        try {
            programmeService = new ProgrammeService();
            evenementService = new EvenementService();
        } catch (SQLException e) {
            showError("❌ Erreur connexion DB");
            e.printStackTrace();
        }

        lblMessage.setText("");
    }

    private void chargerInfosEvenement() {
        try {
            Evenement e = evenementService.getOneById(idEvent);
            if (e != null) {
                lblEventInfo.setText("Event #" + e.getIdEvent() + " : " + e.getTitre() + " (" + e.getType() + ")");
            } else {
                lblEventInfo.setText("⚠️ Événement introuvable (id=" + idEvent + ")");
            }
        } catch (SQLException ex) {
            lblEventInfo.setText("❌ Erreur chargement événement");
            ex.printStackTrace();
        }
    }

    @FXML
    private void onAjouterProgramme(ActionEvent event) {
        lblMessage.setText("");

        try {
            String titre = safe(txtTitre.getText());
            if (titre.isEmpty()) {
                showError("❌ Titre programme obligatoire.");
                return;
            }

            if (dpDebut.getValue() == null || dpFin.getValue() == null) {
                showError("❌ Choisis date début et date fin.");
                return;
            }

            LocalDateTime debut = LocalDateTime.of(
                    dpDebut.getValue(),
                    LocalTime.of(spDebutH.getValue(), spDebutM.getValue())
            );

            LocalDateTime fin = LocalDateTime.of(
                    dpFin.getValue(),
                    LocalTime.of(spFinH.getValue(), spFinM.getValue())
            );

            if (!fin.isAfter(debut)) {
                showError("❌ Fin doit être après début.");
                return;
            }

            Programme p = new Programme(idEvent, titre, debut, fin);
            programmeService.add(p);

            showSuccess("✅ Programme ajouté ! ID=" + p.getIdProg());

            txtTitre.clear();
            dpDebut.setValue(LocalDate.now());
            dpFin.setValue(LocalDate.now());

        } catch (SQLException ex) {
            showError("❌ Erreur DB (insert programme)");
            ex.printStackTrace();
        }
    }

    @FXML
    private void onRetour(ActionEvent event) {
        SceneUtil.switchToWithData("/DetailsEvenement.fxml", "Détails Événement", idEvent);
    }

    private void showSuccess(String msg) {
        lblMessage.setText(msg);
        lblMessage.getStyleClass().removeAll("error");
        lblMessage.getStyleClass().add("success");
    }

    private void showError(String msg) {
        lblMessage.setText(msg);
        lblMessage.getStyleClass().removeAll("success");
        lblMessage.getStyleClass().add("error");
    }

    private String safe(String s) {
        return s == null ? "" : s.trim();
    }
}