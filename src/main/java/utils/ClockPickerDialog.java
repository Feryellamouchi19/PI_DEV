package utils;

import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Button;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import java.time.LocalTime;
import java.util.Optional;

/**
 * Dialog avec une horloge pour choisir l'heure et les minutes.
 * Clic 1 : sur le cercle extérieur = heure (1-12), puis clic 2 : sur le cercle intérieur = minutes.
 */
public class ClockPickerDialog {

    private static final int SIZE = 240;
    private static final double RADIUS = 95;
    private static final double R_MIN = RADIUS * 0.5;

    private int hour = 9;
    private int minute = 0;
    private boolean selectingMinute = false;
    private final Canvas canvas;
    private final GraphicsContext gc;
    private LocalTime result;

    public ClockPickerDialog(int initialHour, int initialMinute) {
        this.hour = Math.max(0, Math.min(23, initialHour));
        this.minute = Math.max(0, Math.min(59, initialMinute));

        canvas = new Canvas(SIZE, SIZE);
        gc = canvas.getGraphicsContext2D();

        canvas.setOnMouseClicked(e -> {
            if (e.getButton() != MouseButton.PRIMARY) return;
            double x = e.getX() - SIZE / 2.0;
            double y = e.getY() - SIZE / 2.0;
            double dist = Math.sqrt(x * x + y * y);

            if (selectingMinute) {
                if (dist < R_MIN * 0.7) return;
                double angle = Math.atan2(-y, x);
                int min = (int) Math.round((angle + Math.PI) / (2 * Math.PI) * 60) % 60;
                if (min < 0) min += 60;
                this.minute = min;
                selectingMinute = false;
            } else {
                if (dist > R_MIN && dist <= RADIUS + 5) {
                    double angle = Math.atan2(-y, x);
                    int h12 = (int) Math.round((angle + Math.PI) / (2 * Math.PI) * 12) % 12;
                    if (h12 < 0) h12 += 12;
                    this.hour = (hour >= 12) ? (h12 == 0 ? 12 : h12 + 12) : (h12 == 0 ? 0 : h12);
                }
                selectingMinute = true;
            }
            draw();
        });

        draw();
    }

    private void draw() {
        gc.clearRect(0, 0, SIZE, SIZE);

        double cx = SIZE / 2.0;
        double cy = SIZE / 2.0;

        gc.setStroke(Color.web("#4a5568"));
        gc.setLineWidth(2);
        gc.strokeOval(cx - RADIUS, cy - RADIUS, RADIUS * 2, RADIUS * 2);

        gc.setStroke(Color.web("#718096"));
        gc.setLineWidth(1.5);
        gc.strokeOval(cx - R_MIN, cy - R_MIN, R_MIN * 2, R_MIN * 2);

        gc.setFill(Color.WHITE);
        gc.setFont(Font.font(14));

        for (int h = 0; h < 12; h++) {
            double angle = (h - 3) * Math.PI / 6;
            String num = h == 0 ? "12" : String.valueOf(h);
            double x = cx + (RADIUS - 20) * Math.cos(angle) - 6;
            double y = cy + (RADIUS - 20) * Math.sin(angle) + 4;
            gc.fillText(num, x, y);
        }

        double hourAngle = (hour % 12 + minute / 60.0 - 3) * Math.PI / 6;
        double hx = cx + (RADIUS * 0.4) * Math.cos(hourAngle);
        double hy = cy + (RADIUS * 0.4) * Math.sin(hourAngle);
        gc.setStroke(Color.web("#e2e8f0"));
        gc.setLineWidth(3);
        gc.strokeLine(cx, cy, hx, hy);

        double minAngle = (minute - 15) * Math.PI / 30;
        double mx = cx + R_MIN * Math.cos(minAngle);
        double my = cy + R_MIN * Math.sin(minAngle);
        gc.setStroke(Color.web("#63b3ed"));
        gc.setLineWidth(2);
        gc.strokeLine(cx, cy, mx, my);

        gc.setFill(Color.web("#63b3ed"));
        gc.fillOval(cx - 4, cy - 4, 8, 8);

        gc.setFill(Color.web("#a0aec0"));
        gc.setFont(Font.font(11));
        String hint = selectingMinute ? "Cliquez sur le cercle pour les minutes" : "Cliquez sur le cercle pour l'heure";
        gc.fillText(hint, 8, SIZE - 6);
        gc.fillText(String.format("%02d:%02d", hour, minute), cx - 18, 22);
    }

    public Optional<LocalTime> showAndWait() {
        Stage stage = new Stage(StageStyle.UTILITY);
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.setTitle("Choisir l'heure");

        Button ok = new Button("OK");
        ok.setStyle("-fx-background-color: #3182ce; -fx-text-fill: white; -fx-padding: 8 24;");
        ok.setOnAction(e -> {
            result = LocalTime.of(hour, minute);
            stage.close();
        });

        VBox root = new VBox(12, canvas, ok);
        root.setStyle("-fx-background-color: #2d3748; -fx-padding: 16; -fx-alignment: center;");

        stage.setScene(new Scene(root));
        stage.showAndWait();

        return Optional.ofNullable(result);
    }
}
