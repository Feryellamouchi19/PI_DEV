package services;

import java.nio.file.*;

public class UnsplashTest {
    public static void main(String[] args) throws Exception {
        EventImageApi api = new EventImageApi();
        var gen = api.generateForEvent("Soirée Neon", "soirée", "SOIREE", "Tunis");
        System.out.println("OK file=" + gen.fileName + " bytes=" + gen.bytes.length);

        Path p = Paths.get(System.getProperty("user.dir"), "uploads", "images", gen.fileName);
        System.out.println("saved=" + p.toAbsolutePath());
    }
}