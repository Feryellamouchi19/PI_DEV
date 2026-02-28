package controllers;

import entities.Equipment;
import entities.Evenement;
import interfaces.DataReceiver;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;
import services.EquipmentService;
import services.EvenementService;
import utils.Session;

import java.util.ArrayList;
import java.util.List;
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

    @FXML private VBox boxEquipment;
    @FXML private TextField txtEquipment;
    @FXML private FlowPane flowEquipmentSuggestions;
    @FXML private FlowPane flowEquipmentSelected;

    private final EvenementService evenementService = new EvenementService();
    private final EquipmentService equipmentService = new EquipmentService();
    private int eventId;
    private Evenement event;
    private final List<String> equipmentSelected = new ArrayList<>();

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

        applyEquipmentVisibility();
        loadEquipmentForEdit();

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
            evenementService.update(event);
            if (Session.isAdmin()) {
                equipmentService.deleteByEventId(eventId);
                if (!equipmentSelected.isEmpty()) {
                    equipmentService.addAll(eventId, equipmentSelected);
                }
            }
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

    private void applyEquipmentVisibility() {
        if (boxEquipment != null) {
            boolean admin = Session.isAdmin();
            boxEquipment.setVisible(admin);
            boxEquipment.setManaged(admin);
        }
    }

    private void loadEquipmentForEdit() {
        equipmentSelected.clear();
        List<Equipment> list = equipmentService.getByEventId(eventId);
        if (list != null) for (Equipment e : list) equipmentSelected.add(safe(e.getLibelle()));

        refreshEquipmentSuggestions();
        refreshEquipmentSelected();
    }

    private void refreshEquipmentSuggestions() {
        if (flowEquipmentSuggestions == null) return;
        flowEquipmentSuggestions.getChildren().clear();
        String type = safe(txtType.getText());
        for (String s : EquipmentService.getSuggestionsByType(type)) {
            Button btn = new Button("+ " + s);
            btn.getStyleClass().add("chip");
            btn.setOnAction(e -> addEquipmentItem(s));
            flowEquipmentSuggestions.getChildren().add(btn);
        }
    }

    private void refreshEquipmentSelected() {
        if (flowEquipmentSelected == null) return;
        flowEquipmentSelected.getChildren().clear();
        for (String lib : equipmentSelected) {
            Button chip = new Button("✕ " + lib);
            chip.getStyleClass().add("chip");
            chip.setOnAction(e -> removeEquipmentItem(lib));
            flowEquipmentSelected.getChildren().add(chip);
        }
    }

    @FXML
    private void onAddEquipment() {
        String custom = txtEquipment != null ? safe(txtEquipment.getText()) : "";
        if (!custom.isBlank()) {
            addEquipmentItem(custom);
            if (txtEquipment != null) txtEquipment.clear();
        }
    }

    private void addEquipmentItem(String libelle) {
        if (libelle == null || libelle.trim().isBlank()) return;
        String lib = libelle.trim();
        if (equipmentSelected.contains(lib)) return;
        equipmentSelected.add(lib);
        refreshEquipmentSelected();
    }

    private void removeEquipmentItem(String libelle) {
        equipmentSelected.remove(libelle);
        refreshEquipmentSelected();
    }

    private String safe(String s) {
        return s == null ? "" : s.trim();
    }
}