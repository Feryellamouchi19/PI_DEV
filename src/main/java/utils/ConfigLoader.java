package utils;

import java.io.InputStream;
import java.util.Properties;

public class ConfigLoader {

    public static Properties loadFromClasspath(String resourcePath) {
        Properties p = new Properties();
        try (InputStream is = ConfigLoader.class.getResourceAsStream(resourcePath)) {
            if (is == null) {
                System.err.println("ConfigLoader: resource introuvable: " + resourcePath);
                return p;
            }
            p.load(is);
        } catch (Exception e) {
            System.err.println("ConfigLoader: erreur chargement " + resourcePath + " : " + e.getMessage());
        }
        // remplace ${ENV_VAR} si présent
        resolveEnvPlaceholders(p);
        return p;
    }

    private static void resolveEnvPlaceholders(Properties p) {
        for (String key : p.stringPropertyNames()) {
            String v = p.getProperty(key);
            if (v == null) continue;

            // support exact: ${ENV_NAME}
            if (v.startsWith("${") && v.endsWith("}")) {
                String envName = v.substring(2, v.length() - 1).trim();
                String envVal = System.getenv(envName);
                if (envVal != null && !envVal.isBlank()) {
                    p.setProperty(key, envVal);
                } else {
                    // laisse vide (et on gère l'erreur plus tard)
                    p.setProperty(key, "");
                }
            }
        }
    }
}