package services;

import javafx.application.Platform;
import org.controlsfx.control.Notifications;

public class NotificationService {

    public static void info(String title, String text) {
        Platform.runLater(() ->
                Notifications.create()
                        .title(title)
                        .text(text)
                        .showInformation()
        );
    }

    public static void warn(String title, String text) {
        Platform.runLater(() ->
                Notifications.create()
                        .title(title)
                        .text(text)
                        .showWarning()
        );
    }

    public static void error(String title, String text) {
        Platform.runLater(() ->
                Notifications.create()
                        .title(title)
                        .text(text)
                        .showError()
        );
    }
}