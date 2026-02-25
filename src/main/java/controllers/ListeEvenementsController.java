package controllers;

import entities.Evenement;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;
import services.EvenementService;
import services.FilterCriteria;
import services.RecommendationService;
import utils.Session;

import java.net.URL;
import java.time.LocalDate;
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

    // ✅ Reco UI
    @FXML private VBox boxReco;
    @FXML private Label lblReco;
    @FXML private FlowPane flowRecommended;

    @FXML private Button btnRechercher;
    @FXML private Button btnReset;
    @FXML private Button btnFiltrer;
    @FXML private Button btnVoirDetails;
    @FXML private Button btnSupprimer;
    @FXML private Button btnAjouterBottom;

    @FXML private ImageView bgImage;
    @FXML private ImageView imgLogo;

    @FXML private ChoiceBox<String> cbRole;

    private final EvenementService service = new EvenementService();
    private final RecommendationService recoService = new RecommendationService();

    private List<Evenement> all = new ArrayList<>();
    private Evenement selected;

    private EventCardController lastSelectedCard;

    @FXML
    public void initialize() {
        SceneUtil.loadBackgroundImage(bgImage);
        SceneUtil.loadLogoImage(imgLogo);

        if (cbType != null) {
            cbType.getItems().setAll("TOUS", "SOIREE", "RANDONNEE", "CAMPING", "SEJOUR");
            cbType.setValue("TOUS");
        }

        if (cbSort != null) {
            cbSort.getItems().setAll("Titre", "Date début", "Type");
            cbSort.setValue("Titre");
        }

        if (dpFrom != null) dpFrom.setValue(null);
        if (dpTo != null) dpTo.setValue(null);

        initRoleChoiceBox();
        applyRoleUi();

        if (btnRechercher != null) btnRechercher.setOnAction(e -> onSearch());
        if (btnReset != null) btnReset.setOnAction(e -> onReset());
        if (btnFiltrer != null) btnFiltrer.setOnAction(e -> onFiltrer());
        if (btnVoirDetails != null) btnVoirDetails.setOnAction(e -> onVoirDetails());
        if (btnSupprimer != null) btnSupprimer.setOnAction(e -> onSupprimer());
        if (btnAjouterBottom != null) btnAjouterBottom.setOnAction(e -> onGoAjouter());

        loadFromDB();
    }

    // ===================== ROLE =====================

    private void initRoleChoiceBox() {
        if (cbRole == null) return;

        cbRole.getItems().setAll("USER", "ADMIN");
        cbRole.setValue(Session.isAdmin() ? "ADMIN" : "USER");

        cbRole.getSelectionModel().selectedItemProperty().addListener((obs, oldV, newV) -> {
            if (newV == null) return;

            Session.setRole("ADMIN".equalsIgnoreCase(newV) ? Session.Role.ADMIN : Session.Role.USER);
            applyRoleUi();

            if (lblMsg != null) lblMsg.setText("✓ Mode: " + newV);
        });
    }

    private void applyRoleUi() {
        boolean admin = Session.isAdmin();

        if (btnSupprimer != null) {
            btnSupprimer.setVisible(admin);
            btnSupprimer.setManaged(admin);
        }
        if (btnAjouterBottom != null) {
            btnAjouterBottom.setVisible(admin);
            btnAjouterBottom.setManaged(admin);
        }
    }

    // ===================== DB =====================

    private void loadFromDB() {
        try {
            all = service.getAll();
            refreshCards(all);
            clearSelection();
            updateRecoVisibilityAndContent(Collections.emptyList());
            if (lblMsg != null) lblMsg.setText("✓ " + all.size() + " événement(s)");
        } catch (Exception ex) {
            if (lblMsg != null) lblMsg.setText("❌ Erreur chargement DB");
            ex.printStackTrace();
        }
    }

    // ===================== RENDER =====================

    private void refreshCards(List<Evenement> events) {
        if (flowEvents == null) return;

        flowEvents.getChildren().clear();
        clearSelection();

        if (events == null || events.isEmpty()) {
            if (lblMsg != null) lblMsg.setText("ℹ️ Aucun événement à afficher");
            return;
        }

        URL url = getClass().getResource("/EventCard.fxml");
        if (url == null) throw new IllegalStateException("EventCard.fxml introuvable. Chemin attendu: /EventCard.fxml");

        for (Evenement ev : events) {
            try {
                FXMLLoader loader = new FXMLLoader(url);
                Node card = loader.load();

                EventCardController c = loader.getController();
                c.setSelected(false);

                c.setData(ev,
                        e -> {
                            selected = e;
                            if (lastSelectedCard != null) lastSelectedCard.setSelected(false);
                            lastSelectedCard = c;
                            lastSelectedCard.setSelected(true);

                            if (lblMsg != null) lblMsg.setText("✓ Sélectionné: " + safe(e.getTitre()));
                        },
                        e -> {
                            selected = e;
                            SceneUtil.switchToWithData("/DetailsEvenement.fxml", "Détails Événement", e.getIdEvent());
                        }
                );

                flowEvents.getChildren().add(card);

            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }

    private void refreshRecommended(List<Evenement> events) {
        if (flowRecommended == null) return;

        flowRecommended.getChildren().clear();

        if (events == null || events.isEmpty()) {
            if (lblReco != null) lblReco.setText("⭐ Recommandés pour vous");
            return;
        }

        if (lblReco != null) lblReco.setText("⭐ Recommandés pour vous (" + events.size() + ")");

        URL url = getClass().getResource("/EventCard.fxml");
        if (url == null) throw new IllegalStateException("EventCard.fxml introuvable. Chemin attendu: /EventCard.fxml");

        for (Evenement ev : events) {
            try {
                FXMLLoader loader = new FXMLLoader(url);
                Node card = loader.load();

                EventCardController c = loader.getController();
                c.setSelected(false);

                c.setData(ev,
                        e -> {
                            selected = e;
                            if (lastSelectedCard != null) lastSelectedCard.setSelected(false);
                            lastSelectedCard = c;
                            lastSelectedCard.setSelected(true);

                            if (lblMsg != null) lblMsg.setText("⭐ Recommandé sélectionné: " + safe(e.getTitre()));
                        },
                        e -> {
                            selected = e;
                            SceneUtil.switchToWithData("/DetailsEvenement.fxml", "Détails Événement", e.getIdEvent());
                        }
                );

                flowRecommended.getChildren().add(card);

            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }

    private void clearSelection() {
        selected = null;
        if (lastSelectedCard != null) {
            lastSelectedCard.setSelected(false);
            lastSelectedCard = null;
        }
    }

    // ===================== RECO VISIBILITY =====================

    private boolean hasAnyFilterApplied() {
        boolean hasSearch = txtSearch != null && !safe(txtSearch.getText()).isBlank();

        String type = cbType == null ? "TOUS" : cbType.getValue();
        boolean hasType = type != null && !"TOUS".equalsIgnoreCase(type);

        boolean hasFrom = dpFrom != null && dpFrom.getValue() != null;
        boolean hasTo   = dpTo != null && dpTo.getValue() != null;

        return hasSearch || hasType || hasFrom || hasTo;
    }

    private void setRecoVisible(boolean visible) {
        if (boxReco == null) return;
        boxReco.setVisible(visible);
        boxReco.setManaged(visible);
    }

    private void updateRecoVisibilityAndContent(List<Evenement> rec) {
        boolean show = hasAnyFilterApplied() && rec != null && !rec.isEmpty();
        setRecoVisible(show);

        if (show) {
            refreshRecommended(rec);
        } else {
            refreshRecommended(Collections.emptyList());
        }
    }

    // ===================== ACTIONS =====================

    @FXML
    private void onGoAjouter() {
        SceneUtil.switchTo("/AjouterEvenement.fxml", "Ajouter Événement");
    }

    @FXML
    private void onVoirDetails() {
        if (selected == null) {
            if (lblMsg != null) lblMsg.setText("⚠️ Sélectionnez un événement.");
            return;
        }
        SceneUtil.switchToWithData("/DetailsEvenement.fxml", "Détails Événement", selected.getIdEvent());
    }

    @FXML
    private void onReset() {
        if (txtSearch != null) txtSearch.clear();
        if (cbType != null) cbType.setValue("TOUS");
        if (cbSort != null) cbSort.setValue("Titre");
        if (dpFrom != null) dpFrom.setValue(null);
        if (dpTo != null) dpTo.setValue(null);

        refreshCards(all);
        updateRecoVisibilityAndContent(Collections.emptyList());

        if (lblMsg != null) lblMsg.setText("✓ " + all.size() + " événement(s)");
    }

    @FXML
    private void onSearch() {
        String q = safe(txtSearch.getText()).toLowerCase(Locale.ROOT);

        if (q.isEmpty()) {
            refreshCards(all);
            updateRecoVisibilityAndContent(Collections.emptyList());
            if (lblMsg != null) lblMsg.setText("✓ " + all.size() + " événement(s)");
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
        if (lblMsg != null) lblMsg.setText("✓ Résultats: " + filtered.size());

        FilterCriteria c = new FilterCriteria();
        c.setKeyword(safe(txtSearch.getText()));
        c.setType(cbType == null ? "TOUS" : cbType.getValue());

        List<Evenement> rec = recoService.recommendFromFilter(c, all, filtered, 6);
        updateRecoVisibilityAndContent(rec);
    }

    @FXML
    private void onFiltrer() {
        List<Evenement> tmp = getCurrentFiltered();

        String type = (cbType == null) ? "TOUS" : cbType.getValue();
        if (type != null && !"TOUS".equalsIgnoreCase(type)) {
            tmp = tmp.stream()
                    .filter(e -> type.equalsIgnoreCase(safe(e.getType())))
                    .collect(Collectors.toList());
        }

        LocalDate from = (dpFrom == null) ? null : dpFrom.getValue();
        LocalDate to   = (dpTo == null) ? null : dpTo.getValue();

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
        if (lblMsg != null) lblMsg.setText("✓ filtré: " + tmp.size());

        FilterCriteria c = new FilterCriteria();
        c.setType(type);
        c.setKeyword(safe(txtSearch.getText()));
        if (from != null) c.setDateFrom(from.atStartOfDay());
        if (to != null) c.setDateTo(to.atTime(23, 59, 59));

        List<Evenement> rec = recoService.recommendFromFilter(c, all, tmp, 6);
        updateRecoVisibilityAndContent(rec);
    }

    @FXML
    private void onSupprimer() {
        if (!Session.isAdmin()) {
            if (lblMsg != null) lblMsg.setText("⛔ Action réservée à l'admin.");
            return;
        }

        if (selected == null) {
            if (lblMsg != null) lblMsg.setText("⚠️ Sélectionne un événement d’abord.");
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
                    if (lblMsg != null) lblMsg.setText("✅ Supprimé");
                    loadFromDB();
                } catch (Exception ex) {
                    if (lblMsg != null) lblMsg.setText("❌ Erreur suppression");
                    ex.printStackTrace();
                }
            }
        });
    }

    // ===================== HELPERS =====================

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