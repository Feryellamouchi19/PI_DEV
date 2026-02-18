package controllers;

import entities.Evenement;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import services.EvenementService;

import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

public class AjouterEvenementController {

    @FXML private TextField txtTitre;
    @FXML private TextArea txtDescription;
    @FXML private ChoiceBox<String> cbType;

    // Date début
    @FXML private DatePicker dpDebut;
    @FXML private Spinner<Integer> spDebutH;
    @FXML private Spinner<Integer> spDebutM;

    // Date fin
    @FXML private DatePicker dpFin;
    @FXML private Spinner<Integer> spFinH;
    @FXML private Spinner<Integer> spFinM;

    @FXML private TextField txtLieu;
    @FXML private Label lblMessage;

    private EvenementService service;

    @FXML
    public void initialize() {

        // Types
        cbType.getItems().setAll("SOIREE", "RANDONNEE", "CAMPING", "SEJOUR");
        cbType.setValue("SOIREE");

        // Spinners
        spDebutH.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 23, 20));
        spDebutM.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 59, 0));
        spFinH.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 23, 10));
        spFinM.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 59, 0));

        spDebutH.setEditable(true);
        spDebutM.setEditable(true);
        spFinH.setEditable(true);
        spFinM.setEditable(true);

        // Dates par défaut
        dpDebut.setValue(LocalDate.now());
        dpFin.setValue(LocalDate.now().plusDays(1));

        // DB
        try {
            service = new EvenementService();
        } catch (SQLException e) {
            showError("❌ Erreur connexion DB");
            e.printStackTrace();
        }

        // Activer/désactiver date fin selon type
        cbType.getSelectionModel().selectedItemProperty().addListener((obs, oldV, newV) -> {
            boolean besoinFin = "CAMPING".equals(newV) || "SEJOUR".equals(newV);
            setFinEnabled(besoinFin);
        });

        // Etat initial : SOIREE => pas besoin fin
        setFinEnabled(false);
        lblMessage.setText("");
    }

    private void setFinEnabled(boolean enabled) {
        dpFin.setDisable(!enabled);
        spFinH.setDisable(!enabled);
        spFinM.setDisable(!enabled);

        if (!enabled) {
            dpFin.setValue(null);
            spFinH.getValueFactory().setValue(0);
            spFinM.getValueFactory().setValue(0);
        } else {
            if (dpFin.getValue() == null) dpFin.setValue(dpDebut.getValue().plusDays(1));
        }
    }

    @FXML
    private void onAjouterEvenement(ActionEvent event) {
        clearMessageStyles();

        try {
            // Lire champs
            String titre = safe(txtTitre.getText());
            String desc  = safe(txtDescription.getText());
            String lieu  = safe(txtLieu.getText());
            String type  = cbType.getValue();

            // Validations
            if (titre.isEmpty() || desc.isEmpty() || lieu.isEmpty()) {
                showError("❌ Remplis Titre / Description / Lieu.");
                return;
            }

            if (dpDebut.getValue() == null) {
                showError("❌ Choisis une date début.");
                return;
            }

            LocalDateTime debut = LocalDateTime.of(
                    dpDebut.getValue(),
                    LocalTime.of(spDebutH.getValue(), spDebutM.getValue())
            );

            LocalDateTime fin = null;
            boolean needFin = "CAMPING".equals(type) || "SEJOUR".equals(type);

            if (needFin) {
                if (dpFin.getValue() == null) {
                    showError("❌ Date fin obligatoire pour CAMPING / SEJOUR.");
                    return;
                }

                fin = LocalDateTime.of(
                        dpFin.getValue(),
                        LocalTime.of(spFinH.getValue(), spFinM.getValue())
                );

                if (!fin.isAfter(debut)) {
                    showError("❌ Date fin doit être après date début.");
                    return;
                }
            }

            // Insert DB
            Evenement e = new Evenement(titre, desc, type, debut, fin, lieu);
            service.add(e);

            showSuccess("✅ Événement ajouté ! ID = " + e.getIdEvent());

            // Alert Oui/Non
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.setTitle("Ajouter Programme");
            alert.setHeaderText("Événement ajouté avec succès !");
            alert.setContentText("Voulez-vous ajouter un programme pour cet événement ?");

            ButtonType btnOui = new ButtonType("Oui");
            ButtonType btnNon = new ButtonType("Non", ButtonBar.ButtonData.CANCEL_CLOSE);
            alert.getButtonTypes().setAll(btnOui, btnNon);

            alert.showAndWait().ifPresent(response -> {
                if (response == btnOui) {
                    SceneUtil.switchToWithData("/AjouterProgramme.fxml", "Ajouter Programme", e.getIdEvent());
                } else {
                    SceneUtil.switchTo("/ListeEvenements.fxml", "Liste des Événements");
                }
            });

        } catch (SQLException ex) {
            showError("❌ Erreur DB (insert)");
            ex.printStackTrace();
        } catch (Exception ex) {
            showError("❌ Erreur: " + ex.getMessage());
            ex.printStackTrace();
        }
    }

    @FXML
    private void onRetour() {
        SceneUtil.switchTo("/ListeEvenements.fxml", "Liste des Événements");
    }

    // ===================== UI Helpers =====================

    private void clearMessageStyles() {
        lblMessage.getStyleClass().removeAll("success", "error");
        lblMessage.setText("");
    }

    private void showSuccess(String msg) {
        lblMessage.setText(msg);
        lblMessage.getStyleClass().removeAll("error");
        if (!lblMessage.getStyleClass().contains("success")) lblMessage.getStyleClass().add("success");
    }

    private void showError(String msg) {
        lblMessage.setText(msg);
        lblMessage.getStyleClass().removeAll("success");
        if (!lblMessage.getStyleClass().contains("error")) lblMessage.getStyleClass().add("error");
    }

    private String safe(String s) {
        return s == null ? "" : s.trim();
    }
}