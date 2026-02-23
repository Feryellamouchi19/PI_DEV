package controllers;

import entities.Evenement;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.Consumer;

public class EventCardController {

    @FXML private StackPane rootCard;
    @FXML private ImageView imgEvent;
    @FXML private Label lblTitre;
    @FXML private Label lblType;

    private Evenement current;
    private Consumer<Evenement> onSelect;

    public void setData(Evenement e, Consumer<Evenement> onSelect) {
        this.current = e;
        this.onSelect = onSelect;

        if (rootCard == null || imgEvent == null || lblTitre == null || lblType == null) {
            throw new IllegalStateException("EventCard.fxml: fx:id manquant (rootCard/imgEvent/lblTitre/lblType)");
        }

        lblTitre.setText(e.getTitre() == null ? "" : e.getTitre());
        lblType.setText(e.getType() == null ? "" : e.getType());

        String file = (e.getImage() == null || e.getImage().isBlank()) ? "logo.png" : e.getImage().trim();

        // 1) uploads/images
        try {
            Path p = Path.of("uploads/images").resolve(file);
            if (Files.exists(p)) {
                imgEvent.setImage(new Image(p.toUri().toString()));
            } else {
                loadFromResources(file);
            }
        } catch (Exception ex) {
            loadFromResources("logo.png");
        }

        rootCard.setOnMouseClicked(ev -> {
            if (this.onSelect != null && current != null) this.onSelect.accept(current);
        });
    }

    private void loadFromResources(String file) {
        String path = "/images/" + file;
        try (InputStream is = getClass().getResourceAsStream(path)) {
            if (is != null) {
                imgEvent.setImage(new Image(is));
            } else {
                try (InputStream is2 = getClass().getResourceAsStream("/images/logo.png")) {
                    if (is2 != null) imgEvent.setImage(new Image(is2));
                }
            }
        } catch (Exception ex) {
            System.out.println("⚠️ Image load error: " + ex.getMessage() + " (" + path + ")");
        }
    }
}