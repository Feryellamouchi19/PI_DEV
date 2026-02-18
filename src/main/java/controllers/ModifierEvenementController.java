package controllers;

import entities.Evenement;
import interfaces.DataReceiver;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import services.EvenementService;

import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

public class ModifierEvenementController implements DataReceiver<Integer> {

    @FXML private Label lblMsg;

    @FXML private TextField txtTitre;
    @FXML private ComboBox<String> cbType;
    @FXML private TextField txtLieu;

    @FXML private DatePicker dpDebut;
    @FXML private TextField txtHeureDebut;

    @FXML private DatePicker dpFin;
    @FXML private TextField txtHeureFin;

    @FXML private TextArea txtDescription;

    private EvenementService service;
    private int eventId;
    private Evenement event;

    @FXML
    public void initialize() {
        try {
            service = new EvenementService();
        } catch (SQLException e) {
            lblMsg.setText("❌ Erreur connexion DB");
            e.printStackTrace();
            return;
        }

        // Types (mets exactement ceux de ta DB)
        cbType.getItems().setAll("SOIREE", "RANDONNEE", "CAMPING", "SEJOUR");
    }

    @Override
    public void setData(Integer id) {
        if (id == null || id <= 0) {
            lblMsg.setText("❌ ID invalide");
            return;
        }
        this.eventId = id;
        loadEvent();
    }

    private void loadEvent() {
        try {
            event = service.getOneById(eventId);
            if (event == null) {
                lblMsg.setText("❌ Événement introuvable");
                return;
            }

            txtTitre.setText(event.getTitre());
            cbType.setValue(event.getType());
            txtLieu.setText(event.getLieu());
            txtDescription.setText(event.getDescription());

            if (event.getDateDebut() != null) {
                dpDebut.setValue(event.getDateDebut().toLocalDate());
                txtHeureDebut.setText(event.getDateDebut().toLocalTime().toString().substring(0,5));
            }

            if (event.getDateFin() != null) {
                dpFin.setValue(event.getDateFin().toLocalDate());
                txtHeureFin.setText(event.getDateFin().toLocalTime().toString().substring(0,5));
            } else {
                txtHeureFin.setText("");
            }

            lblMsg.setText("✏️ Modification: " + event.getTitre());

        } catch (SQLException e) {
            lblMsg.setText("❌ Erreur chargement événement");
            e.printStackTrace();
        }
    }

    @FXML
    private void onSave() {
        if (event == null) {
            lblMsg.setText("❌ Aucun événement chargé");
            return;
        }

        String titre = txtTitre.getText() == null ? "" : txtTitre.getText().trim();
        String type = cbType.getValue();
        String lieu = txtLieu.getText() == null ? "" : txtLieu.getText().trim();
        String desc = txtDescription.getText() == null ? "" : txtDescription.getText().trim();

        if (titre.isEmpty() || type == null || type.isBlank() || lieu.isEmpty()) {
            lblMsg.setText("❌ Titre, Type et Lieu sont obligatoires");
            return;
        }

        LocalDate dDebut = dpDebut.getValue();
        String hDebutStr = txtHeureDebut.getText() == null ? "" : txtHeureDebut.getText().trim();

        if (dDebut == null || hDebutStr.isEmpty()) {
            lblMsg.setText("❌ Date début et heure début obligatoires");
            return;
        }

        LocalDateTime debut;
        LocalDateTime fin = null;

        try {
            LocalTime hDebut = LocalTime.parse(hDebutStr); // format HH:mm
            debut = LocalDateTime.of(dDebut, hDebut);

            LocalDate dFin = dpFin.getValue();
            String hFinStr = txtHeureFin.getText() == null ? "" : txtHeureFin.getText().trim();

            if (dFin != null && !hFinStr.isEmpty()) {
                LocalTime hFin = LocalTime.parse(hFinStr);
                fin = LocalDateTime.of(dFin, hFin);
                if (fin.isBefore(debut)) {
                    lblMsg.setText("❌ La date/heure fin doit être après le début");
                    return;
                }
            }

        } catch (Exception ex) {
            lblMsg.setText("❌ Format heure invalide (ex: 10:00)");
            return;
        }

        // Mettre à jour l'objet
        event.setTitre(titre);
        event.setType(type);
        event.setLieu(lieu);
        event.setDescription(desc);
        event.setDateDebut(debut);
        event.setDateFin(fin);

        try {
            service.update(event); // ✅ à ajouter dans EvenementService
            lblMsg.setText("✅ Événement mis à jour");

            // Retour vers details (pour voir la mise à jour)
            SceneUtil.switchToWithData("/DetailsEvenement.fxml", "Détails Événement", eventId);

        } catch (SQLException e) {
            lblMsg.setText("❌ Erreur mise à jour");
            e.printStackTrace();
        }
    }

    @FXML
    private void onCancel() {
        SceneUtil.switchToWithData("/DetailsEvenement.fxml", "Détails Événement", eventId);
    }
}