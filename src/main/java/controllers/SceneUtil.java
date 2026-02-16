package controllers;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;

/**
 * SceneUtil: utilitaire pour naviguer entre les pages FXML.
 * ✅ switchTo(...) : navigation simple
 * ✅ switchToAndGetController(...) : navigation + récupérer le controller
 * ✅ switchToWithData(...) : navigation + passer une donnée (ex: id_event)
 */
public class SceneUtil {

    private static Stage stage;

    /** À appeler une seule fois dans Home.start(primaryStage) */
    public static void setStage(Stage s) {
        stage = s;
    }

    private static void ensureStage() {
        if (stage == null) {
            throw new IllegalStateException(
                    "Stage non initialisé ! Appelle SceneUtil.setStage(primaryStage) dans Home.start()."
            );
        }
    }

    private static URL requireResource(String path) {
        URL url = SceneUtil.class.getResource(path);
        if (url == null) {
            throw new RuntimeException("FXML/CSS introuvable: " + path + " (vérifie src/main/resources)");
        }
        return url;
    }

    /** Navigation simple */
    public static void switchTo(String fxmlPath, String title) {
        ensureStage();
        try {
            URL url = requireResource(fxmlPath);

            Parent root = FXMLLoader.load(url);
            Scene scene = new Scene(root);

            stage.setScene(scene);
            stage.setTitle(title);
            stage.show();

        } catch (IOException e) {
            throw new RuntimeException("Erreur chargement FXML: " + fxmlPath, e);
        }
    }

    /**
     * Navigation + récupérer le controller.
     * Utile si tu veux appeler une méthode après chargement.
     */
    public static <T> T switchToAndGetController(String fxmlPath, String title) {
        ensureStage();
        try {
            URL url = requireResource(fxmlPath);

            FXMLLoader loader = new FXMLLoader(url);
            Parent root = loader.load();

            Scene scene = new Scene(root);
            stage.setScene(scene);
            stage.setTitle(title);
            stage.show();

            return loader.getController();

        } catch (IOException e) {
            throw new RuntimeException("Erreur chargement FXML: " + fxmlPath, e);
        }
    }

    /**
     * Navigation + passer une donnée au controller suivant.
     * Le controller cible doit implémenter DataReceiver<T>.
     *
     * Exemple:
     * SceneUtil.switchToWithData("/AjouterProgramme.fxml","Ajouter Programme", eventId);
     */
    public static <T> void switchToWithData(String fxmlPath, String title, T data) {
        ensureStage();
        try {
            URL url = requireResource(fxmlPath);

            FXMLLoader loader = new FXMLLoader(url);
            Parent root = loader.load();

            Object controller = loader.getController();
            if (controller instanceof DataReceiver<?> receiver) {
                @SuppressWarnings("unchecked")
                DataReceiver<T> r = (DataReceiver<T>) receiver;
                r.initData(data);
            }

            Scene scene = new Scene(root);
            stage.setScene(scene);
            stage.setTitle(title);
            stage.show();

        } catch (IOException e) {
            throw new RuntimeException("Erreur chargement FXML: " + fxmlPath, e);
        }
    }

    /** Interface à implémenter si un controller doit recevoir une donnée */
    public interface DataReceiver<T> {
        void initData(T data);
    }
}