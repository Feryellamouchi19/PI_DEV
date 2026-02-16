package controllers;

import entities.Evenement;
import entities.Programme;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import services.EvenementService;
import services.ProgrammeService;

import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

public class AjouterProgrammeController implements SceneUtil.DataReceiver<Integer> {

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

    private int eventId;
    private Evenement evenement;

    @FXML
    public void initialize() {

        // Configuration spinners
        spDebutH.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(0,23,20));
        spDebutM.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(0,59,0));
        spFinH.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(0,23,22));
        spFinM.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(0,59,0));

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
            showError("Erreur connexion DB");
            e.printStackTrace();
        }
    }

    // 🔥 Cette méthode reçoit l'id_event depuis la page précédente
    @Override
    public void initData(Integer id) {
        this.eventId = id;

        try {
            evenement = evenementService.getOneById(id);
            if (evenement != null) {
                lblEventInfo.setText("Événement : " + evenement.getTitre()
                        + " | Début: " + evenement.getDateDebut());
            }
        } catch (SQLException e) {
            showError("Erreur chargement événement");
        }
    }

    @FXML
    private void onAjouterProgramme() {

        try {
            String titre = txtTitre.getText().trim();

            if (titre.isEmpty()) {
                showError("Titre obligatoire");
                return;
            }

            if (dpDebut.getValue() == null || dpFin.getValue() == null) {
                showError("Dates obligatoires");
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
                showError("Date fin doit être après date début");
                return;
            }

            // 🔥 Vérification par rapport à l'événement
            if (evenement != null) {
                if (debut.isBefore(evenement.getDateDebut())) {
                    showError("Programme avant début événement !");
                    return;
                }

                if (evenement.getDateFin() != null &&
                        fin.isAfter(evenement.getDateFin())) {
                    showError("Programme dépasse la date fin événement !");
                    return;
                }
            }

            Programme p = new Programme(eventId, titre, debut, fin);
            programmeService.add(p);

            showSuccess("Programme ajouté ! ID = " + p.getIdProg());

            resetForm();

        } catch (Exception e) {
            showError("Erreur insertion");
            e.printStackTrace();
        }
    }

    @FXML
    private void onRetour() {
        SceneUtil.switchTo("/ListeEvenements.fxml", "Liste Evenements");
    }

    private void resetForm() {
        txtTitre.clear();
        dpDebut.setValue(LocalDate.now());
        dpFin.setValue(LocalDate.now());
    }

    private void showError(String msg) {
        lblMessage.setText("❌ " + msg);
    }

    private void showSuccess(String msg) {
        lblMessage.setText("✅ " + msg);
    }
}