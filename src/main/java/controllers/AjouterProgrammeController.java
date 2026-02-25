package controllers;

import entities.Evenement;
import entities.Programme;
import interfaces.DataReceiver;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
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
    @FXML private ImageView bgImage;
    @FXML private ImageView imgLogo;

    private final ProgrammeService programmeService = new ProgrammeService();
    private final EvenementService evenementService = new EvenementService();

    private int idEvent = 0;

    @Override
    public void setData(Integer data) {
        this.idEvent = (data == null) ? 0 : data;
        chargerInfosEvenement();
    }

    @FXML
    public void initialize() {
        SceneUtil.loadBackgroundImage(bgImage);
        SceneUtil.loadLogoImage(imgLogo);
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

        lblMessage.setText("");
        lblEventInfo.setText("—");
    }

    private void chargerInfosEvenement() {
        if (idEvent <= 0) {
            lblEventInfo.setText("⚠️ Événement non sélectionné");
            return;
        }

        Evenement e = evenementService.getOneById(idEvent);

        if (e == null) {
            lblEventInfo.setText("⚠️ Événement introuvable");
            return;
        }

        // ✅ Sans ID (titre + type)
        lblEventInfo.setText(safe(e.getTitre()) + " (" + safe(e.getType()) + ")");
    }

    @FXML
    private void onAjouterProgramme(ActionEvent event) {

        if (idEvent <= 0) {
            showError("❌ Aucun événement sélectionné.");
            return;
        }

        String titre = safe(txtTitre.getText());
        if (titre.isEmpty()) {
            showError("❌ Titre programme obligatoire.");
            return;
        }
        if (titre.length() < 2) {
            showError("❌ Titre trop court (min 2).");
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

        // ✅ CONTRÔLE CHEVAUCHEMENT
        try {
            if (programmeService.existsOverlap(idEvent, debut, fin)) {

                Programme overlap = programmeService.getFirstOverlap(idEvent, debut, fin);

                if (overlap != null && overlap.getDebut() != null && overlap.getFin() != null) {
                    showError("❌ Conflit: un programme existe déjà entre "
                            + overlap.getDebut().toLocalTime() + " et " + overlap.getFin().toLocalTime());
                } else {
                    showError("❌ Conflit: un programme existe déjà dans ce créneau.");
                }
                return;
            }
        } catch (SQLException ex) {
            showError("❌ Erreur DB (vérif chevauchement)");
            ex.printStackTrace();
            return;
        }

        Programme p = new Programme();
        p.setEventId(idEvent);
        p.setTitre(titre);
        p.setDebut(debut);
        p.setFin(fin);

        try {
            programmeService.add(p);
            showSuccess("✅ Programme ajouté !");
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
        lblMessage.getStyleClass().removeAll("error", "success");
        lblMessage.getStyleClass().add("success");
    }

    private void showError(String msg) {
        lblMessage.setText(msg);
        lblMessage.getStyleClass().removeAll("success", "error");
        lblMessage.getStyleClass().add("error");
    }

    private String safe(String s) {
        return s == null ? "" : s.trim();
    }
}