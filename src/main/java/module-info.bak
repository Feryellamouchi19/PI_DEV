module org.example {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.sql;
    requires java.net.http;
    requires java.desktop;

    requires mysql.connector.j;
    requires com.fasterxml.jackson.databind;

    opens controllers to javafx.fxml;
    opens entities to com.fasterxml.jackson.databind;

    exports controllers;
}
