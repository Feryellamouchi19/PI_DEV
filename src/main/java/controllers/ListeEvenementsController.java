package controllers;

import entities.Evenement;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;
import services.EvenementService;

import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

public class ListeEvenementsController {

    @FXML private TextField txtSearch;
    @FXML private Label lblMsg;

    @FXML private FlowPane flowEvents;

    @FXML private ComboBox<String> cbType;
    @FXML private DatePicker dpFrom;
    @FXML private DatePicker dpTo;
    @FXML private ComboBox<String> cbSort;

    private EvenementService service;

    // ✅ sélection
    private Evenement selectedEvent;
    private VBox selectedCard;

    @FXML
    public void initialize() {
        try {
            service = new EvenementService();
            initFiltresEtTri();
            loadAll();
        } catch (SQLException e) {
            lblMsg.setText("❌ Erreur connexion DB");
            e.printStackTrace();
        }
    }

    private void initFiltresEtTri() {
        cbType.getItems().setAll("SOIREE", "RANDONNEE", "CAMPING", "SEJOUR");

        cbSort.getItems().setAll(
                "Titre (A→Z)",
                "Titre (Z→A)",
                "Date début (↑)",
                "Date début (↓)"
        );

        cbType.setValue(null);
        dpFrom.setValue(null);
        dpTo.setValue(null);
        cbSort.setValue(null);
    }

    private void loadAll() {
        try {
            List<Evenement> list = service.getAll();
            render(list);
            lblMsg.setText("✅ " + list.size() + " événement(s)");
        } catch (SQLException e) {
            lblMsg.setText("❌ Erreur chargement événements");
            e.printStackTrace();
        }
    }

    private void render(List<Evenement> list) {
        if (flowEvents == null) return;

        flowEvents.getChildren().clear();
        selectedEvent = null;

        if (selectedCard != null) {
            selectedCard.getStyleClass().remove("event-card-selected");
            selectedCard = null;
        }

        for (Evenement e : list) {
            VBox card = createEventCard(e);
            flowEvents.getChildren().add(card);
        }
    }

    private VBox createEventCard(Evenement e) {
        ImageView img = new ImageView();
        img.setFitWidth(200);
        img.setFitHeight(150);
        img.setPreserveRatio(true);

        // ✅ charger l'image de l'event si existe
        loadEventImage(img, e);

        Label title = new Label(e.getTitre() == null ? "" : e.getTitre());
        title.getStyleClass().add("subtitle");

        VBox card = new VBox(10, img, title);
        card.getStyleClass().add("event-card");
        card.setPrefWidth(220);

        // ✅ un seul handler click
        card.setOnMouseClicked(ev -> {
            if (ev.getClickCount() == 2) {
                SceneUtil.switchToWithData("/DetailsEvenement.fxml", "Détails Événement", e.getIdEvent());
            } else {
                selectCard(card, e);
            }
        });

        return card;
    }

    private void selectCard(VBox card, Evenement e) {
        selectedEvent = e;

        if (selectedCard != null) {
            selectedCard.getStyleClass().remove("event-card-selected");
        }

        selectedCard = card;
        selectedCard.getStyleClass().add("event-card-selected");

        lblMsg.setText("✅ Sélectionné: " + (e.getTitre() == null ? "" : e.getTitre()));
    }

    @FXML
    private void onSearch() {
        String q = txtSearch.getText() == null ? "" : txtSearch.getText().trim();

        if (q.isEmpty()) {
            loadAll();
            return;
        }

        try {
            List<Evenement> list = service.search(q);
            render(list);
            lblMsg.setText("🔎 " + list.size() + " résultat(s)");
        } catch (SQLException e) {
            lblMsg.setText("❌ Erreur recherche");
            e.printStackTrace();
        }
    }

    @FXML
    private void onFiltrer() {
        String q = txtSearch.getText() == null ? "" : txtSearch.getText().trim();
        String type = cbType.getValue();
        LocalDate from = dpFrom.getValue();
        LocalDate to = dpTo.getValue();

        try {
            List<Evenement> all = service.getAll();

            List<Evenement> filtered = all.stream()
                    .filter(e -> q.isEmpty()
                            || containsIgnoreCase(e.getTitre(), q)
                            || containsIgnoreCase(e.getDescription(), q)
                            || containsIgnoreCase(e.getLieu(), q))
                    .filter(e -> type == null || type.isBlank()
                            || (e.getType() != null && e.getType().trim().equalsIgnoreCase(type.trim())))
                    .filter(e -> {
                        if (from == null && to == null) return true;
                        if (e.getDateDebut() == null) return false;

                        LocalDate d = e.getDateDebut().toLocalDate();
                        boolean okFrom = (from == null) || !d.isBefore(from);
                        boolean okTo = (to == null) || !d.isAfter(to);
                        return okFrom && okTo;
                    })
                    .toList();

            String choice = cbSort.getValue();
            if (choice != null) {
                filtered = sortList(filtered, choice);
            }

            render(filtered);
            lblMsg.setText("🔎 " + filtered.size() + " résultat(s)");

        } catch (SQLException ex) {
            lblMsg.setText("❌ Erreur filtre");
            ex.printStackTrace();
        }
    }

    @FXML
    private void onTrier() {
        onFiltrer();
    }

    private List<Evenement> sortList(List<Evenement> list, String choice) {
        Comparator<Evenement> byTitre = Comparator.comparing(
                e -> e.getTitre() == null ? "" : e.getTitre().toLowerCase()
        );

        Comparator<Evenement> byDateDebut = Comparator.comparing(
                e -> e.getDateDebut() == null ? LocalDateTime.MIN : e.getDateDebut()
        );

        return switch (choice) {
            case "Titre (A→Z)" -> list.stream().sorted(byTitre).toList();
            case "Titre (Z→A)" -> list.stream().sorted(byTitre.reversed()).toList();
            case "Date début (↑)" -> list.stream().sorted(byDateDebut).toList();
            case "Date début (↓)" -> list.stream().sorted(byDateDebut.reversed()).toList();
            default -> list;
        };
    }

    @FXML
    private void onSupprimer() {
        if (selectedEvent == null) {
            lblMsg.setText("❌ Sélectionne un événement à supprimer (clic sur une carte)");
            return;
        }

        Alert a = new Alert(Alert.AlertType.CONFIRMATION);
        a.setTitle("Confirmation");
        a.setHeaderText("Supprimer cet événement ?");
        a.setContentText(selectedEvent.getTitre());

        a.showAndWait().ifPresent(btn -> {
            if (btn == ButtonType.OK) {
                try {
                    service.delete(selectedEvent.getIdEvent());
                    lblMsg.setText("✅ Événement supprimé");
                    loadAll();
                } catch (SQLException e) {
                    lblMsg.setText("❌ Erreur suppression");
                    e.printStackTrace();
                }
            }
        });
    }

    @FXML
    private void onReset() {
        txtSearch.clear();
        initFiltresEtTri();
        loadAll();
    }

    @FXML
    private void onGoAjouter() {
        SceneUtil.switchTo("/AjouterEvenement.fxml", "Ajouter Événement");
    }

    @FXML
    private void onRetour() {
        SceneUtil.switchTo("/Home.fxml", "Home");
    }

    // ===== helpers =====
    private boolean containsIgnoreCase(String src, String q) {
        if (src == null || q == null) return false;
        return src.toLowerCase().contains(q.toLowerCase());
    }

    // ✅ charge image depuis DB (resources / http / file)
    private void loadEventImage(ImageView img, Evenement e) {
        try {
            String path = (e == null) ? null : e.getImage();

            // fallback
            if (path == null || path.isBlank()) {
                img.setImage(new Image(getClass().getResourceAsStream("/images/logo.png")));
                return;
            }

            // resource path ex: "images/events/fabrika.png"
            if (!path.startsWith("http") && !path.matches("^[A-Za-z]:.*")) {
                if (!path.startsWith("/")) path = "/" + path;
                img.setImage(new Image(getClass().getResourceAsStream(path)));
                return;
            }

            // http url
            if (path.startsWith("http")) {
                img.setImage(new Image(path, true));
                return;
            }

            // local file Windows
            img.setImage(new Image("file:" + path));

        } catch (Exception ex) {
            img.setImage(new Image(getClass().getResourceAsStream("/images/logo.png")));
        }
    }
}