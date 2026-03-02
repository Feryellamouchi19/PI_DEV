package controllers;

import entities.Evenement;
import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;
import services.EvenementService;
import services.FilterCriteria;
import services.RecommendationService;
import utils.Session;

import java.net.URL;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

public class ListeEvenementsController {

    @FXML private StackPane root;

    @FXML private TextField txtSearch;
    @FXML private Button btnClearSearch;

    @FXML private ComboBox<String> cbType;
    @FXML private ComboBox<String> cbSort;
    @FXML private DatePicker dpFrom;
    @FXML private DatePicker dpTo;

    @FXML private FlowPane flowEvents;
    @FXML private Label lblMsg;

    // Reco UI
    @FXML private VBox boxReco;
    @FXML private Label lblReco;
    @FXML private FlowPane flowRecommended;

    @FXML private Button btnFiltrer;
    @FXML private Button btnSupprimer;
    @FXML private Button btnAjouterBottom;

    @FXML private ImageView bgImage;
    @FXML private ImageView imgLogo;

    @FXML private ChoiceBox<String> cbRole;
    @FXML private ToggleButton btnTheme;

    private final EvenementService service = new EvenementService();
    private final RecommendationService recoService = new RecommendationService();

    private List<Evenement> all = new ArrayList<>();
    private List<Evenement> lastDisplayedList = new ArrayList<>();
    private Evenement selected;

    private EventCardController lastSelectedCard;

    // Theme
    private static final String CSS_BASE  = "/css/events-base.css";
    private static final String CSS_DARK  = "/css/theme-dark.css";
    private static final String CSS_LIGHT = "/css/theme-light.css";
    private boolean darkMode = true;

    // debounce for live search
    private final PauseTransition searchDebounce = new PauseTransition(Duration.millis(300));

    @FXML
    public void initialize() {
        SceneUtil.loadBackgroundImage(bgImage);
        SceneUtil.loadLogoImage(imgLogo);

        // Apply theme when scene exists
        Platform.runLater(() -> {
            if (root != null && root.getScene() != null) {
                applyTheme(root.getScene(), true); // start dark
            }
        });

        initRoleChoiceBox();
        applyRoleUi();

        initFilters();
        initLiveSearch();

        if (btnFiltrer != null) btnFiltrer.setOnAction(e -> onFiltrer());
        if (btnSupprimer != null) btnSupprimer.setOnAction(e -> onSupprimer());
        if (btnAjouterBottom != null) btnAjouterBottom.setOnAction(e -> onGoAjouter());

        loadFromDB();
        PauseTransition pause = new PauseTransition(Duration.millis(300));

        txtSearch.textProperty().addListener((obs, oldVal, newVal) -> {
            btnClearSearch.setVisible(newVal != null && !newVal.isEmpty());
            pause.setOnFinished(e -> onSearch());
            pause.playFromStart();
        });

    }

    // ===================== THEME =====================

    @FXML
    private void toggleTheme() {
        utils.ThemeManager.toggle();
        if (root != null && root.getScene() != null) {
            utils.ThemeManager.apply(root.getScene());
        }
        if (btnTheme != null) {
            btnTheme.setText(utils.ThemeManager.getTheme() == utils.ThemeManager.Theme.DARK ? "🌙" : "☀️");
        }
    }

    private void applyTheme(Scene scene, boolean dark) {
        if (scene == null) return;

        scene.getStylesheets().removeIf(s ->
                s.endsWith("theme-dark.css") ||
                        s.endsWith("theme-light.css") ||
                        s.endsWith("events-base.css"));

        URL themeUrl = getClass().getResource(dark ? CSS_DARK : CSS_LIGHT);
        URL baseUrl  = getClass().getResource(CSS_BASE);

        if (themeUrl == null) throw new IllegalStateException("CSS introuvable: " + (dark ? CSS_DARK : CSS_LIGHT));
        if (baseUrl  == null) throw new IllegalStateException("CSS introuvable: " + CSS_BASE);

        scene.getStylesheets().add(themeUrl.toExternalForm());
        scene.getStylesheets().add(baseUrl.toExternalForm());

        darkMode = dark;

        if (btnTheme != null) {
            btnTheme.setText(darkMode ? "🌙" : "☀️");
        }
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

    // ===================== FILTERS + SEARCH =====================

    private void initFilters() {
        if (cbType != null) {
            cbType.getItems().setAll("TOUS", "SOIREE", "RANDONNEE", "CAMPING", "SEJOUR");
            cbType.setValue("TOUS");
        }

        if (cbSort != null) {
            cbSort.getItems().setAll("Titre", "Date début", "Type", "Le plus vu");
            cbSort.setValue("Titre");
            cbSort.getSelectionModel().selectedItemProperty().addListener((o, ov, nv) -> {
                if (lastDisplayedList != null && !lastDisplayedList.isEmpty()) {
                    refreshCards(applySort(lastDisplayedList));
                }
            });
        }

        if (dpFrom != null) dpFrom.setValue(null);
        if (dpTo != null) dpTo.setValue(null);
    }

    private void initLiveSearch() {
        if (txtSearch == null) return;

        // clear button visibility
        if (btnClearSearch != null) {
            btnClearSearch.setVisible(false);
            btnClearSearch.setManaged(false);
        }

        searchDebounce.setOnFinished(e -> onSearch());

        txtSearch.textProperty().addListener((obs, oldVal, newVal) -> {
            boolean hasText = newVal != null && !newVal.trim().isEmpty();
            if (btnClearSearch != null) {
                btnClearSearch.setVisible(hasText);
                btnClearSearch.setManaged(hasText);
            }
            searchDebounce.playFromStart();
        });
    }

    @FXML
    private void onClearSearch() {
        if (txtSearch != null) txtSearch.clear();
        onSearch(); // refresh list
    }

    // ===================== DB =====================

    private void loadFromDB() {
        try {
            all = service.getAll();
            refreshCards(applySort(all));
            clearSelection();
            updateRecoAfterLoad();
            if (lblMsg != null) lblMsg.setText("✓ " + all.size() + " événement(s)");
        } catch (Exception ex) {
            if (lblMsg != null) lblMsg.setText("❌ Erreur chargement DB");
            ex.printStackTrace();
        }
    }

    // ===================== RENDER =====================

    private void refreshCards(List<Evenement> events) {
        if (flowEvents == null) return;

        lastDisplayedList = events == null ? new ArrayList<>() : new ArrayList<>(events);
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

    // ===================== RECO =====================

    private boolean hasAnyFilterApplied() {
        boolean hasSearch = txtSearch != null && !safe(txtSearch.getText()).isBlank();

        String type = cbType == null ? "TOUS" : cbType.getValue();
        boolean hasType = type != null && !"TOUS".equalsIgnoreCase(type);

        boolean hasFrom = dpFrom != null && dpFrom.getValue() != null;
        boolean hasTo   = dpTo != null && dpTo.getValue() != null;

        return hasSearch || hasType || hasFrom || hasTo;
    }

    private void updateRecoVisibilityAndContent(List<Evenement> rec) {
        if (rec == null) rec = Collections.emptyList();
        if (boxReco != null) {
            boxReco.setVisible(true);
            boxReco.setManaged(true);
        }

        if (rec.isEmpty()) {
            if (lblReco != null) lblReco.setText(hasAnyFilterApplied()
                    ? "⭐ Recommandés pour vous (aucun)"
                    : "⭐ Suggestions (événements à venir)");
            if (flowRecommended != null) flowRecommended.getChildren().clear();
        } else {
            refreshRecommended(rec);
        }
    }

    private void updateRecoAfterLoad() {
        FilterCriteria c = new FilterCriteria();
        c.setType("TOUS");
        List<Evenement> rec = recoService.recommendFromFilter(c, all, Collections.emptyList(), 6);
        updateRecoVisibilityAndContent(rec);
    }

    // ===================== ACTIONS =====================

    @FXML
    private void onGoAjouter() {
        SceneUtil.switchTo("/AjouterEvenement.fxml", "Ajouter Événement");
    }

    @FXML
    private void onSearch() {
        String q = safe(txtSearch != null ? txtSearch.getText() : "").toLowerCase(Locale.ROOT);

        List<Evenement> filtered;
        if (q.isEmpty()) {
            filtered = new ArrayList<>(all);
        } else {
            filtered = all.stream()
                    .filter(e ->
                            safe(e.getTitre()).toLowerCase(Locale.ROOT).contains(q)
                                    || safe(e.getLieu()).toLowerCase(Locale.ROOT).contains(q)
                                    || safe(e.getDescription()).toLowerCase(Locale.ROOT).contains(q)
                    )
                    .collect(Collectors.toList());
        }

        // Apply current filters too (type/date) when typing
        filtered = applyCurrentTypeAndDateFilters(filtered);

        refreshCards(applySort(filtered));
        if (lblMsg != null) lblMsg.setText(q.isEmpty()
                ? "✓ " + filtered.size() + " événement(s)"
                : "✓ Résultats: " + filtered.size());

        FilterCriteria c = new FilterCriteria();
        c.setKeyword(safe(txtSearch != null ? txtSearch.getText() : ""));
        c.setType(cbType == null ? "TOUS" : cbType.getValue());
        if (dpFrom != null && dpFrom.getValue() != null) c.setDateFrom(dpFrom.getValue().atStartOfDay());
        if (dpTo != null && dpTo.getValue() != null) c.setDateTo(dpTo.getValue().atTime(23, 59, 59));

        List<Evenement> rec = recoService.recommendFromFilter(c, all, filtered, 6);
        updateRecoVisibilityAndContent(rec);
    }

    @FXML
    private void onFiltrer() {
        // when user hits "Filtrer", we apply filters AND current search text
        onSearch();
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

    private List<Evenement> applyCurrentTypeAndDateFilters(List<Evenement> base) {
        List<Evenement> tmp = new ArrayList<>(base);

        String type = cbType == null ? "TOUS" : cbType.getValue();
        if (type != null && !"TOUS".equalsIgnoreCase(type)) {
            tmp = tmp.stream()
                    .filter(e -> type.equalsIgnoreCase(safe(e.getType())))
                    .collect(Collectors.toList());
        }

        LocalDate from = dpFrom == null ? null : dpFrom.getValue();
        LocalDate to   = dpTo == null ? null : dpTo.getValue();

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

        return tmp;
    }

    private List<Evenement> applySort(List<Evenement> list) {
        if (list == null) return List.of();

        String sort = cbSort == null ? "Titre" : cbSort.getValue();
        if (sort == null) sort = "Titre";

        List<Evenement> sorted = new ArrayList<>(list);
        switch (sort) {
            case "Date début" -> sorted.sort(Comparator.comparing(
                    e -> e.getDateDebut() == null ? java.time.LocalDateTime.MIN : e.getDateDebut()));
            case "Type" -> sorted.sort(Comparator.comparing(e -> safe(e.getType()).toLowerCase(Locale.ROOT)));
            case "Le plus vu" -> sorted.sort(Comparator.comparingInt(Evenement::getNbVues).reversed());
            default -> sorted.sort(Comparator.comparing(e -> safe(e.getTitre()).toLowerCase(Locale.ROOT)));
        }
        return sorted;
    }

    private String safe(String s) {
        return s == null ? "" : s.trim();
    }

}