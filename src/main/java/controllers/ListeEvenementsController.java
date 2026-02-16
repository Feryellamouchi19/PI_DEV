package controllers;

import entities.Evenement;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import services.EvenementService;

import java.sql.SQLException;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class ListeEvenementsController {

    private final DateTimeFormatter F = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    @FXML private TableView<Evenement> table;
    @FXML private TableColumn<Evenement, Integer> colId;
    @FXML private TableColumn<Evenement, String> colTitre;
    @FXML private TableColumn<Evenement, String> colType;
    @FXML private TableColumn<Evenement, String> colDebut;
    @FXML private TableColumn<Evenement, String> colFin;
    @FXML private TableColumn<Evenement, String> colLieu;

    @FXML private TextField txtSearch;
    @FXML private ChoiceBox<String> cbTri;
    @FXML private ChoiceBox<String> cbTypeFilter;
    @FXML private Label lblInfo;

    private EvenementService service;
    private ObservableList<Evenement> master = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        try {
            service = new EvenementService();
        } catch (SQLException e) {
            lblInfo.setText("❌ Erreur connexion DB");
            e.printStackTrace();
            return;
        }

        // Colonnes (sans JavaFX Properties => on utilise des lambdas)
        colId.setCellValueFactory(data -> new javafx.beans.property.SimpleIntegerProperty(data.getValue().getIdEvent()).asObject());
        colTitre.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(data.getValue().getTitre()));
        colType.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(data.getValue().getType()));
        colLieu.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(data.getValue().getLieu()));

        colDebut.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(
                data.getValue().getDateDebut() == null ? "" : data.getValue().getDateDebut().format(F)
        ));

        colFin.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(
                data.getValue().getDateFin() == null ? "" : data.getValue().getDateFin().format(F)
        ));

        // Tri
        cbTri.getItems().addAll("Date ASC", "Date DESC", "ID DESC");
        cbTri.setValue("ID DESC");

        // Filtre type
        cbTypeFilter.getItems().addAll("TOUS", "SOIREE", "RANDONNEE", "CAMPING", "SEJOUR");
        cbTypeFilter.setValue("TOUS");

        // listeners: recherche + tri + filtre
        txtSearch.textProperty().addListener((obs, o, n) -> appliquer());
        cbTri.getSelectionModel().selectedItemProperty().addListener((obs, o, n) -> appliquer());
        cbTypeFilter.getSelectionModel().selectedItemProperty().addListener((obs, o, n) -> appliquer());

        reload();
        table.setRowFactory(tv -> {
            TableRow<Evenement> row = new TableRow<>();
            row.setOnMouseClicked(e -> {
                if (e.getClickCount() == 2 && !row.isEmpty()) {
                    Evenement selected = row.getItem();
                    SceneUtil.switchToWithData("/DetailsEvenement.fxml", "Détails Événement", selected.getIdEvent());
                }
            });
            return row;
        });
    }

    private void reload() {
        try {
            master.setAll(service.getAll());
            appliquer();
        } catch (SQLException e) {
            lblInfo.setText("❌ Erreur lecture DB");
            e.printStackTrace();
        }
    }

    private void appliquer() {
        try {
            List<Evenement> list = master;

            // 1) search
            String k = txtSearch.getText() == null ? "" : txtSearch.getText().trim();
            if (!k.isEmpty()) {
                list = service.rechercher(list, k);
            }

            // 2) filtre type
            String type = cbTypeFilter.getValue();
            if (type != null && !type.equals("TOUS")) {
                list = service.filtrerParType(list, type);
            }

            // 3) tri
            String tri = cbTri.getValue();
            if ("Date ASC".equals(tri)) list = service.trierParDateAsc(list);
            else if ("Date DESC".equals(tri)) list = service.trierParDateDesc(list);
            else { // ID DESC
                list = list.stream()
                        .sorted((a, b) -> Integer.compare(b.getIdEvent(), a.getIdEvent()))
                        .toList();
            }

            table.setItems(FXCollections.observableArrayList(list));
            lblInfo.setText("✅ " + list.size() + " événement(s) affiché(s)");

        } catch (Exception e) {
            lblInfo.setText("❌ Erreur filtre/tri");
            e.printStackTrace();
        }
    }

    @FXML
    private void onReload() {
        reload();
    }

    @FXML
    private void onDeleteSelected() {
        Evenement sel = table.getSelectionModel().getSelectedItem();
        if (sel == null) {
            lblInfo.setText("❌ Sélectionne un événement.");
            return;
        }

        Alert a = new Alert(Alert.AlertType.CONFIRMATION);
        a.setTitle("Confirmation");
        a.setHeaderText("Supprimer l'événement ID=" + sel.getIdEvent() + " ?");
        a.setContentText(sel.getTitre());

        a.showAndWait().ifPresent(btn -> {
            if (btn == ButtonType.OK) {
                try {
                    service.delete(sel.getIdEvent());
                    lblInfo.setText("✅ Supprimé !");
                    reload();
                } catch (SQLException e) {
                    lblInfo.setText("❌ Erreur suppression DB");
                    e.printStackTrace();
                }
            }
        });
    }

    @FXML
    private void onGoAjouter() {
        SceneUtil.switchTo("/AjouterEvenement.fxml", "Ajouter Evenement");
    }
}