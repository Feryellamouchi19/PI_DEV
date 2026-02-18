package controllers;

import javafx.application.Application;
import javafx.stage.Stage;

public class Home extends Application {

    @Override
    public void start(Stage primaryStage) {
        SceneUtil.init(primaryStage); // ✅ obligatoire
        SceneUtil.switchTo("/ListeEvenements.fxml", "Liste des Événements");
    }

    public static void main(String[] args) {
        launch(args);
    }
}