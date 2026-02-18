package controllers;

import interfaces.DataReceiver;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

public class SceneUtil {

    private static Stage primaryStage;

    /** À appeler une seule fois dans Home.start(primaryStage) */
    public static void init(Stage stage) {
        primaryStage = stage;
    }

    public static void switchTo(String fxml, String title) {
        ensureStage();
        try {
            FXMLLoader loader = new FXMLLoader(SceneUtil.class.getResource(fxml));
            Parent root = loader.load();

            primaryStage.setScene(new Scene(root));
            primaryStage.setTitle(title);
            primaryStage.show();

        } catch (IOException e) {
            throw new RuntimeException("Erreur chargement FXML: " + fxml, e);
        }
    }

    public static <T> void switchToWithData(String fxml, String title, T data) {
        ensureStage();
        try {
            FXMLLoader loader = new FXMLLoader(SceneUtil.class.getResource(fxml));
            Parent root = loader.load();

            Object controller = loader.getController();

            // ✅ Transmettre la donnée si le controller implémente DataReceiver
            if (controller instanceof DataReceiver<?> dr) {
                @SuppressWarnings("unchecked")
                DataReceiver<T> receiver = (DataReceiver<T>) dr;
                receiver.setData(data);
            } else {
                System.out.println("⚠️ Controller ne reçoit pas de data (pas DataReceiver): " + controller);
            }

            primaryStage.setScene(new Scene(root));
            primaryStage.setTitle(title);
            primaryStage.show();

        } catch (IOException e) {
            throw new RuntimeException("Erreur chargement FXML: " + fxml, e);
        }
    }

    private static void ensureStage() {
        if (primaryStage == null) {
            throw new IllegalStateException(
                    "SceneUtil.primaryStage est null. " +
                            "Appelle SceneUtil.init(primaryStage) dans Home.start(...) avant switchTo."
            );
        }
    }
}