package utils;

import javafx.scene.Scene;
import java.net.URL;

public final class ThemeManager {

    private ThemeManager(){}

    public enum Theme { DARK, LIGHT }

    private static Theme current = Theme.DARK;

    private static final String CSS_BASE  = "/css/events-base.css";
    private static final String CSS_DARK  = "/css/theme-dark.css";
    private static final String CSS_LIGHT = "/css/theme-light.css";

    public static Theme getTheme() {
        return current;
    }

    public static void setTheme(Theme theme) {
        current = theme == null ? Theme.DARK : theme;
    }

    public static void toggle() {
        current = (current == Theme.DARK) ? Theme.LIGHT : Theme.DARK;
    }

    public static void apply(Scene scene) {
        if (scene == null) return;

        // remove only our css
        scene.getStylesheets().removeIf(s ->
                s.endsWith("events-base.css") ||
                        s.endsWith("theme-dark.css") ||
                        s.endsWith("theme-light.css"));

        URL base = ThemeManager.class.getResource(CSS_BASE);
        URL theme = ThemeManager.class.getResource(current == Theme.DARK ? CSS_DARK : CSS_LIGHT);

        if (base == null) throw new IllegalStateException("CSS introuvable: " + CSS_BASE);
        if (theme == null) throw new IllegalStateException("CSS introuvable: " + (current == Theme.DARK ? CSS_DARK : CSS_LIGHT));

        scene.getStylesheets().add(theme.toExternalForm());
        scene.getStylesheets().add(base.toExternalForm());
    }
}