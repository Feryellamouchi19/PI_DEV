package services;

import java.nio.file.Files;
import java.nio.file.Path;

public class GeminiImageTest {
    public static void main(String[] args) throws Exception {
        GeminiImageService s = new GeminiImageService();

        byte[] img = s.generateImageBytes(
                "Professional event poster, camping at night, tent, campfire, stars, cinematic lighting, no text."
        );

        Files.createDirectories(Path.of("uploads/images"));
        Path out = Path.of("uploads/images/gemini_test.png");
        Files.write(out, img);

        System.out.println("✅ Saved: " + out.toAbsolutePath());
    }
}