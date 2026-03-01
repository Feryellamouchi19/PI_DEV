package controllers;

import interfaces.DataReceiver;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.Stage;
import javafx.fxml.FXMLLoader;

import java.net.URL;

public class SceneUtil {

    private static Stage stage;

    // ===================== THEME (GLOBAL) =====================
    private static final String CSS_BASE  = "/css/events-base.css";
    private static final String CSS_DARK  = "/css/theme-dark.css";
    private static final String CSS_LIGHT = "/css/theme-light.css";

    private static boolean darkMode = true; // ✅ thème par défaut

    private SceneUtil() {}

    public static void setStage(Stage s) {
        stage = s;
    }

    public static Stage getStage() {
        return stage;
    }

    // ✅ accessible pour tous les controllers
    public static boolean isDarkMode() {
        return darkMode;
    }

    // ✅ changer le thème global (et l’appliquer directement à la scène courante)
    public static void setDarkMode(boolean dark) {
        darkMode = dark;
        if (stage != null && stage.getScene() != null) {
            applyTheme(stage.getScene());
        }
    }

    // ✅ applique le thème (utilisé à chaque switchTo)
    public static void applyTheme(Scene scene) {
        if (scene == null) return;

        scene.getStylesheets().removeIf(s ->
                s.endsWith("theme-dark.css") ||
                        s.endsWith("theme-light.css") ||
                        s.endsWith("events-base.css")
        );

        URL themeUrl = SceneUtil.class.getResource(darkMode ? CSS_DARK : CSS_LIGHT);
        URL baseUrl  = SceneUtil.class.getResource(CSS_BASE);

        if (themeUrl == null) throw new IllegalStateException("CSS introuvable: " + (darkMode ? CSS_DARK : CSS_LIGHT));
        if (baseUrl  == null) throw new IllegalStateException("CSS introuvable: " + CSS_BASE);

        scene.getStylesheets().add(themeUrl.toExternalForm());
        scene.getStylesheets().add(baseUrl.toExternalForm());
    }

    // ===================== IMAGES =====================

    public static void loadBackgroundImage(ImageView imageView) {
        if (imageView == null) return;
        URL url = SceneUtil.class.getResource("/images/space_bg.png");
        if (url == null) url = SceneUtil.class.getResource("/images/logo.png");
        if (url != null) imageView.setImage(new Image(url.toExternalForm()));
    }

    public static void loadLogoImage(ImageView imageView) {
        if (imageView == null) return;
        URL url = SceneUtil.class.getResource("/images/logo.png");
        if (url != null) imageView.setImage(new Image(url.toExternalForm()));
    }

    // ===================== NAVIGATION =====================

    public static void switchTo(String fxmlPath, String title) {
        switchToWithData(fxmlPath, title, null);
    }

    public static <T> void switchToWithData(String fxmlPath, String title, T data) {
        try {
            if (stage == null) {
                throw new IllegalStateException(
                        "Stage non initialisé. Dans Application.start(): SceneUtil.setStage(primaryStage);"
                );
            }

            URL url = SceneUtil.class.getResource(fxmlPath);
            if (url == null) {
                throw new IllegalStateException(
                        "FXML introuvable: " + fxmlPath +
                                "\n➡️ Vérifie src/main/resources et le chemin."
                );
            }

            FXMLLoader loader = new FXMLLoader(url);
            loader.setClassLoader(SceneUtil.class.getClassLoader());
            Parent root = loader.load();

            // ✅ passer data (même si null)
            Object controller = loader.getController();
            if (controller instanceof DataReceiver<?> dr) {
                @SuppressWarnings("unchecked")
                DataReceiver<T> receiver = (DataReceiver<T>) dr;
                receiver.setData(data);
            }

            Scene scene = new Scene(root);

            // ✅ appliquer thème global (dark/light) + base
            applyTheme(scene);

            stage.setTitle(title);
            stage.setScene(scene);
            stage.centerOnScreen();
            stage.show();

        } catch (Exception e) {
            e.printStackTrace();
            showErrorPopup("Erreur d'ouverture de page", e.getMessage());
        }
    }

    private static void showErrorPopup(String title, String msg) {
        Alert a = new Alert(Alert.AlertType.ERROR);
        a.setTitle(title);
        a.setHeaderText(title);
        a.setContentText(msg == null ? "Erreur inconnue" : msg);
        a.showAndWait();
    }
}