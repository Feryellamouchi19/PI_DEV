package controllers;

import entities.Evenement;
import interfaces.DataReceiver;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import services.EvenementService;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

public class ModifierEvenementController implements DataReceiver<Integer> {

    @FXML private ImageView bgImage;

    @FXML private TextField txtTitre;
    @FXML private TextField txtType;
    @FXML private TextField txtLieu;
    @FXML private TextArea txtDescription;

    @FXML private DatePicker dpDebut;
    @FXML private Spinner<Integer> spDebutH;
    @FXML private Spinner<Integer> spDebutM;

    @FXML private DatePicker dpFin;
    @FXML private Spinner<Integer> spFinH;
    @FXML private Spinner<Integer> spFinM;

    @FXML private Label lblMsg;

    private final EvenementService evenementService = new EvenementService();
    private int eventId;
    private Evenement event;

    @FXML
    public void initialize() {
        // ✅ Charge le bg space_bg.png
        SceneUtil.loadBackgroundImage(bgImage);

        spDebutH.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 23, 0));
        spDebutM.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 59, 0));
        spFinH.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 23, 0));
        spFinM.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 59, 0));

        lblMsg.setText("");
    }

    @Override
    public void setData(Integer id) {
        if (id == null || id <= 0) {
            lblMsg.setText("❌ ID événement invalide");
            return;
        }
        this.eventId = id;

        event = evenementService.getOneById(eventId);
        if (event == null) {
            lblMsg.setText("❌ Événement introuvable");
            return;
        }

        txtTitre.setText(safe(event.getTitre()));
        txtType.setText(safe(event.getType()));
        txtLieu.setText(safe(event.getLieu()));
        txtDescription.setText(safe(event.getDescription()));

        if (event.getDateDebut() != null) {
            dpDebut.setValue(event.getDateDebut().toLocalDate());
            spDebutH.getValueFactory().setValue(event.getDateDebut().getHour());
            spDebutM.getValueFactory().setValue(event.getDateDebut().getMinute());
        }

        if (event.getDateFin() != null) {
            dpFin.setValue(event.getDateFin().toLocalDate());
            spFinH.getValueFactory().setValue(event.getDateFin().getHour());
            spFinM.getValueFactory().setValue(event.getDateFin().getMinute());
        }

        lblMsg.setText("✓ Modification: " + safe(event.getTitre()));
    }

    @FXML
    private void onEnregistrer() {
        if (event == null) {
            lblMsg.setText("❌ Aucun événement chargé");
            return;
        }

        String titre = safe(txtTitre.getText());
        String type = safe(txtType.getText());
        String lieu = safe(txtLieu.getText());
        String desc = safe(txtDescription.getText());

        if (titre.isBlank() || type.isBlank() || lieu.isBlank()) {
            lblMsg.setText("❌ Titre, Type et Lieu sont obligatoires");
            return;
        }

        LocalDate d1 = dpDebut.getValue();
        LocalDate d2 = dpFin.getValue();
        if (d1 == null || d2 == null) {
            lblMsg.setText("❌ Dates obligatoires");
            return;
        }

        LocalDateTime debut = LocalDateTime.of(d1, LocalTime.of(spDebutH.getValue(), spDebutM.getValue()));
        LocalDateTime fin   = LocalDateTime.of(d2, LocalTime.of(spFinH.getValue(), spFinM.getValue()));

        if (fin.isBefore(debut)) {
            lblMsg.setText("❌ Fin doit être après début");
            return;
        }

        event.setTitre(titre);
        event.setType(type);
        event.setLieu(lieu);
        event.setDescription(desc);
        event.setDateDebut(debut);
        event.setDateFin(fin);

        try {
            evenementService.update(event); // ⚠️ doit exister
            lblMsg.setText("✅ Modifié !");
            SceneUtil.switchToWithData("/DetailsEvenement.fxml", "Détails Événement", eventId);
        } catch (Exception ex) {
            ex.printStackTrace();
            lblMsg.setText("❌ Erreur: " + ex.getMessage());
        }
    }

    @FXML
    private void onAnnuler() {
        SceneUtil.switchToWithData("/DetailsEvenement.fxml", "Détails Événement", eventId);
    }

    private String safe(String s) {
        return s == null ? "" : s.trim();
    }
}