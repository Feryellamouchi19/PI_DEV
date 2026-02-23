package controllers;

import entities.Evenement;
import interfaces.DataReceiver;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import services.EvenementService;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

public class ModifierEvenementController implements DataReceiver<Integer> {

    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm");

    @FXML private Label lblMsg;

    @FXML private TextField txtTitre;
    @FXML private ComboBox<String> cbType;
    @FXML private TextField txtLieu;

    @FXML private DatePicker dpDebut;
    @FXML private TextField txtHeureDebut;

    @FXML private DatePicker dpFin;
    @FXML private TextField txtHeureFin;

    @FXML private TextArea txtDescription;

    private final EvenementService service = new EvenementService();

    private int eventId;
    private Evenement event;

    @FXML
    public void initialize() {
        cbType.getItems().setAll("SOIREE", "RANDONNEE", "CAMPING", "SEJOUR");

        // Activer/Désactiver fin selon type
        cbType.getSelectionModel().selectedItemProperty().addListener((obs, oldV, newV) -> {
            boolean besoinFin = newV != null && (newV.equals("CAMPING") || newV.equals("SEJOUR"));
            setFinEnabled(besoinFin);

            // si pas besoin fin -> on vide
            if (!besoinFin) {
                dpFin.setValue(null);
                txtHeureFin.clear();
            }
        });
    }

    @Override
    public void setData(Integer id) {
        if (id == null || id <= 0) {
            showError("ID invalide");
            return;
        }
        this.eventId = id;
        loadEvent();
    }

    private void loadEvent() {
        event = service.getOneById(eventId);

        if (event == null) {
            showError("Événement introuvable");
            return;
        }

        txtTitre.setText(nullSafe(event.getTitre()));
        cbType.setValue(event.getType());
        txtLieu.setText(nullSafe(event.getLieu()));
        txtDescription.setText(nullSafe(event.getDescription()));

        if (event.getDateDebut() != null) {
            dpDebut.setValue(event.getDateDebut().toLocalDate());
            txtHeureDebut.setText(event.getDateDebut().toLocalTime().format(TIME_FMT));
        } else {
            dpDebut.setValue(LocalDate.now());
            txtHeureDebut.setText("10:00");
        }

        boolean besoinFin = event.getType() != null && (event.getType().equals("CAMPING") || event.getType().equals("SEJOUR"));
        setFinEnabled(besoinFin);

        if (event.getDateFin() != null) {
            dpFin.setValue(event.getDateFin().toLocalDate());
            txtHeureFin.setText(event.getDateFin().toLocalTime().format(TIME_FMT));
        } else {
            dpFin.setValue(null);
            txtHeureFin.clear();
        }

        lblMsg.setText("✏️ Modification: " + nullSafe(event.getTitre()));
    }

    private void setFinEnabled(boolean enabled) {
        dpFin.setDisable(!enabled);
        txtHeureFin.setDisable(!enabled);
    }

    @FXML
    private void onSave() {
        if (event == null) {
            showError("Aucun événement chargé");
            return;
        }

        String titre = safe(txtTitre.getText());
        String type = cbType.getValue();
        String lieu = safe(txtLieu.getText());
        String desc = (txtDescription.getText() == null) ? "" : txtDescription.getText().trim();

        if (titre.isEmpty() || type == null || type.isBlank() || lieu.isEmpty()) {
            showError("Titre, Type et Lieu sont obligatoires");
            return;
        }

        LocalDate dDebut = dpDebut.getValue();
        String hDebutStr = safe(txtHeureDebut.getText());

        if (dDebut == null || hDebutStr.isEmpty()) {
            showError("Date début et heure début obligatoires");
            return;
        }

        LocalDateTime debut;
        LocalDateTime fin = null;

        try {
            LocalTime hDebut = LocalTime.parse(hDebutStr, TIME_FMT);
            debut = LocalDateTime.of(dDebut, hDebut);

            boolean besoinFin = type.equals("CAMPING") || type.equals("SEJOUR");
            if (besoinFin) {
                LocalDate dFin = dpFin.getValue();
                String hFinStr = safe(txtHeureFin.getText());

                if (dFin == null || hFinStr.isEmpty()) {
                    showError("Date fin et heure fin obligatoires pour " + type);
                    return;
                }

                LocalTime hFin = LocalTime.parse(hFinStr, TIME_FMT);
                fin = LocalDateTime.of(dFin, hFin);

                if (fin.isBefore(debut) || fin.isEqual(debut)) {
                    showError("La date/heure fin doit être après le début");
                    return;
                }
            }

        } catch (Exception ex) {
            showError("Format heure invalide (ex: 10:00)");
            return;
        }

        // Update entity
        event.setTitre(titre);
        event.setType(type);
        event.setLieu(lieu);
        event.setDescription(desc);
        event.setDateDebut(debut);
        event.setDateFin(fin);

        // ✅ update() ne throw pas SQLException dans ton EvenementService
        service.update(event);

        lblMsg.setText("✅ Événement mis à jour");
        SceneUtil.switchToWithData("/DetailsEvenement.fxml", "Détails Événement", eventId);
    }

    @FXML
    private void onCancel() {
        SceneUtil.switchToWithData("/DetailsEvenement.fxml", "Détails Événement", eventId);
    }

    // ===== helpers =====
    private void showError(String msg) {
        lblMsg.setText("❌ " + msg);
    }

    private String safe(String s) {
        return s == null ? "" : s.trim();
    }

    private String nullSafe(String s) {
        return s == null ? "" : s;
    }
}