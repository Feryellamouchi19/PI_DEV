package controllers;

import javafx.application.Application;
import javafx.stage.Stage;

public class Home extends Application {

    @Override
    public void start(Stage primaryStage) {
        SceneUtil.setStage(primaryStage);
        SceneUtil.switchTo("/ListeEvenements.fxml", "Liste Evenements");
    }

    public static void main(String[] args) {
        launch(args);
    }
}