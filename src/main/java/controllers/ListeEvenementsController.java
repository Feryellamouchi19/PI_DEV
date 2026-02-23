package controllers;

import entities.Evenement;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.FlowPane;
import services.EvenementService;

import java.net.URL;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

public class ListeEvenementsController {

    @FXML private TextField txtSearch;
    @FXML private ComboBox<String> cbType;
    @FXML private ComboBox<String> cbSort;
    @FXML private DatePicker dpFrom;
    @FXML private DatePicker dpTo;
    @FXML private FlowPane flowEvents;
    @FXML private Label lblMsg;

    @FXML private Button btnRechercher;
    @FXML private Button btnReset;
    @FXML private Button btnFiltrer;
    @FXML private Button btnSupprimer;
    @FXML private Button btnAjouterBottom;

    private final EvenementService service = new EvenementService();
    private List<Evenement> all = new ArrayList<>();
    private Evenement selected;

    @FXML
    public void initialize() {

        System.out.println("ListeEvenements init OK");

        // ✅ Combo Type
        cbType.getItems().setAll("TOUS", "SOIREE", "RANDONNEE", "CAMPING", "SEJOUR");
        cbType.setValue("TOUS");

        // ✅ Combo Sort
        cbSort.getItems().setAll("Titre", "Date début", "Type");
        cbSort.setValue("Titre");

        dpFrom.setValue(null);
        dpTo.setValue(null);

        // ✅ Bind buttons
        if (btnRechercher != null) btnRechercher.setOnAction(e -> onSearch());
        if (btnReset != null) btnReset.setOnAction(e -> onReset());
        if (btnFiltrer != null) btnFiltrer.setOnAction(e -> onFiltrer());
        if (btnSupprimer != null) btnSupprimer.setOnAction(e -> onSupprimer());
        if (btnAjouterBottom != null) btnAjouterBottom.setOnAction(e -> onGoAjouter());

        loadFromDB();
    }

    private void loadFromDB() {
        try {
            all = service.getAll();
            System.out.println("✅ DB getAll() => " + all.size() + " événements");

            refreshCards(all);
            lblMsg.setText("✓ " + all.size() + " événement(s)");

        } catch (Exception ex) {
            lblMsg.setText("❌ Erreur chargement DB");
            ex.printStackTrace();
        }
    }

    private void refreshCards(List<Evenement> events) {
        flowEvents.getChildren().clear();
        selected = null;

        if (events == null || events.isEmpty()) {
            lblMsg.setText("ℹ️ Aucun événement à afficher");
            return;
        }

        // ✅ IMPORTANT: FXML est dans resources à la racine => "/EventCard.fxml"
        URL url = getClass().getResource("/EventCard.fxml");
        System.out.println("EventCard URL = " + url);

        if (url == null) {
            lblMsg.setText("❌ EventCard.fxml introuvable (resources)");
            throw new IllegalStateException("EventCard.fxml introuvable. Chemin attendu: /EventCard.fxml");
        }

        for (Evenement ev : events) {
            try {
                FXMLLoader loader = new FXMLLoader(url);
                Node card = loader.load();

                EventCardController c = loader.getController();
                c.setData(ev, e -> {
                    selected = e;
                    lblMsg.setText("✓ Sélectionné: " + safe(e.getTitre()));
                });

                flowEvents.getChildren().add(card);

            } catch (Exception ex) {
                lblMsg.setText("❌ Erreur carte (EventCard)");
                System.out.println("❌ EventCard load error: " + ex.getMessage());
                ex.printStackTrace();
            }
        }
    }

    // ==========================
    // Actions
    // ==========================

    @FXML
    private void onGoAjouter() {
        SceneUtil.switchTo("/AjouterEvenement.fxml", "Ajouter Événement");
    }

    @FXML
    private void onRetour() {
        SceneUtil.switchTo("/Home.fxml", "Home");
    }

    @FXML
    private void onReset() {
        txtSearch.clear();
        cbType.setValue("TOUS");
        cbSort.setValue("Titre");
        dpFrom.setValue(null);
        dpTo.setValue(null);

        refreshCards(all);
        lblMsg.setText("✓ " + all.size() + " événement(s)");
    }

    @FXML
    private void onSearch() {
        String q = safe(txtSearch.getText()).toLowerCase(Locale.ROOT);

        if (q.isEmpty()) {
            refreshCards(all);
            lblMsg.setText("✓ " + all.size() + " événement(s)");
            return;
        }

        List<Evenement> filtered = all.stream()
                .filter(e ->
                        safe(e.getTitre()).toLowerCase(Locale.ROOT).contains(q)
                                || safe(e.getLieu()).toLowerCase(Locale.ROOT).contains(q)
                                || safe(e.getDescription()).toLowerCase(Locale.ROOT).contains(q)
                )
                .collect(Collectors.toList());

        refreshCards(filtered);
        lblMsg.setText("✓ Résultats: " + filtered.size());
    }

    @FXML
    private void onTrier() {
        String sort = cbSort.getValue();
        List<Evenement> tmp = new ArrayList<>(getCurrentFiltered());

        if ("Titre".equals(sort)) {
            tmp.sort(Comparator.comparing(e -> safe(e.getTitre()).toLowerCase(Locale.ROOT)));
        } else if ("Date début".equals(sort)) {
            tmp.sort(Comparator.comparing(e -> e.getDateDebut() == null ? LocalDateTime.MAX : e.getDateDebut()));
        } else if ("Type".equals(sort)) {
            tmp.sort(Comparator.comparing(e -> safe(e.getType()).toLowerCase(Locale.ROOT)));
        }

        refreshCards(tmp);
        lblMsg.setText("✓ tri: " + sort);
    }

    @FXML
    private void onFiltrer() {
        List<Evenement> tmp = getCurrentFiltered();

        String type = cbType.getValue();
        if (type != null && !"TOUS".equalsIgnoreCase(type)) {
            tmp = tmp.stream()
                    .filter(e -> type.equalsIgnoreCase(safe(e.getType())))
                    .collect(Collectors.toList());
        }

        LocalDate from = dpFrom.getValue();
        LocalDate to = dpTo.getValue();

        if (from != null) {
            tmp = tmp.stream()
                    .filter(e -> e.getDateDebut() != null && !e.getDateDebut().toLocalDate().isBefore(from))
                    .collect(Collectors.toList());
        }

        if (to != null) {
            tmp = tmp.stream()
                    .filter(e -> e.getDateDebut() != null && !e.getDateDebut().toLocalDate().isAfter(to))
                    .collect(Collectors.toList());
        }

        refreshCards(tmp);
        lblMsg.setText("✓ filtré: " + tmp.size());
    }

    @FXML
    private void onSupprimer() {
        if (selected == null) {
            lblMsg.setText("⚠️ Sélectionne un événement d’abord.");
            return;
        }

        Alert a = new Alert(Alert.AlertType.CONFIRMATION);
        a.setTitle("Confirmation");
        a.setHeaderText("Supprimer cet événement ?");
        a.setContentText(safe(selected.getTitre()));

        a.showAndWait().ifPresent(btn -> {
            if (btn == ButtonType.OK) {
                try {
                    service.delete(selected.getIdEvent());
                    lblMsg.setText("✅ Supprimé");
                    loadFromDB();
                } catch (Exception ex) {
                    lblMsg.setText("❌ Erreur suppression");
                    ex.printStackTrace();
                }
            }
        });
    }

    // ==========================
    // Helpers
    // ==========================

    private List<Evenement> getCurrentFiltered() {
        String q = safe(txtSearch.getText()).toLowerCase(Locale.ROOT);
        if (q.isEmpty()) return new ArrayList<>(all);

        return all.stream()
                .filter(e ->
                        safe(e.getTitre()).toLowerCase(Locale.ROOT).contains(q)
                                || safe(e.getLieu()).toLowerCase(Locale.ROOT).contains(q)
                                || safe(e.getDescription()).toLowerCase(Locale.ROOT).contains(q)
                )
                .collect(Collectors.toList());
    }

    private String safe(String s) {
        return s == null ? "" : s.trim();
    }
}