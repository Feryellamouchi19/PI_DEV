package controllers;

import interfaces.DataReceiver;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.Stage;

import java.net.URL;

public class SceneUtil {

    private static Stage stage;

    private SceneUtil() {}

    public static void setStage(Stage s) {
        stage = s;
    }

    public static Stage getStage() {
        return stage;
    }

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
                                "\n➡️ Vérifie src/main/resources et le chemin: /ModifierEvenement.fxml"
                );
            }

            FXMLLoader loader = new FXMLLoader(url);
            loader.setClassLoader(SceneUtil.class.getClassLoader());
            Parent root = loader.load();

            // ✅ passer data même si data == null (on laisse le controller décider)
            Object controller = loader.getController();
            if (controller instanceof DataReceiver<?> dr) {
                @SuppressWarnings("unchecked")
                DataReceiver<T> receiver = (DataReceiver<T>) dr;
                receiver.setData(data);
            }

            Scene scene = new Scene(root);
            URL cssUrl = SceneUtil.class.getResource("/css/style.css");
            if (cssUrl != null) scene.getStylesheets().add(cssUrl.toExternalForm());

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