package controllers;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
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

    public static void switchTo(String fxmlPath, String title) {
        switchToWithData(fxmlPath, title, null);
    }

    public static <T> void switchToWithData(String fxmlPath, String title, T data) {
        try {
            if (stage == null) {
                throw new IllegalStateException(
                        "Stage non initialisé. Dans ton Application.start(), appelle SceneUtil.setStage(primaryStage);"
                );
            }

            URL url = SceneUtil.class.getResource(fxmlPath);
            if (url == null) {
                throw new IllegalStateException(
                        "FXML introuvable: " + fxmlPath +
                                "\n➡️ Vérifie que le fichier est dans src/main/resources et que tu passes un chemin du style: /ListeEvenements.fxml"
                );
            }

            FXMLLoader loader = new FXMLLoader(url);
            Parent root = loader.load();

            // Passer data si controller l'accepte
            Object controller = loader.getController();
            if (data != null && controller instanceof interfaces.DataReceiver<?> dr) {
                @SuppressWarnings("unchecked")
                interfaces.DataReceiver<T> receiver = (interfaces.DataReceiver<T>) dr;
                receiver.setData(data);
            }

            Scene scene = new Scene(root);

            stage.setTitle(title);
            stage.setScene(scene);
            stage.centerOnScreen();
            stage.show();

        } catch (Exception e) {
            System.out.println("❌ SceneUtil error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /** Optionnel : si tu veux récupérer le controller après chargement */
    public static <C> C switchToAndReturnController(String fxmlPath, String title) {
        try {
            if (stage == null) {
                throw new IllegalStateException("Stage non initialisé. Appelle SceneUtil.setStage(primaryStage).");
            }

            URL url = SceneUtil.class.getResource(fxmlPath);
            if (url == null) {
                throw new IllegalStateException("FXML introuvable: " + fxmlPath);
            }

            FXMLLoader loader = new FXMLLoader(url);
            Parent root = loader.load();

            Scene scene = new Scene(root);

            stage.setTitle(title);
            stage.setScene(scene);
            stage.centerOnScreen();
            stage.show();

            @SuppressWarnings("unchecked")
            C controller = (C) loader.getController();
            return controller;

        } catch (Exception e) {
            System.out.println("❌ SceneUtil error: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }
}