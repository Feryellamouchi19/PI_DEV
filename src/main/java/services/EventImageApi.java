package services;

/**
 * API pour générer une image d'événement avec l'IA.
 * Utilisable depuis l'ajout ou la modification d'un événement.
 */
public class EventImageApi {

    private final ImageAiService aiService = new ImageAiService();

    /**
     * Génère et enregistre une image à partir des infos de l'événement.
     * Retourne le nom du fichier à stocker en base.
     */
    public ImageAiService.GeneratedImage generateForEvent(String titre, String description, String type, String lieu) throws Exception {
        String prompt = buildPrompt(titre, description, type, lieu);
        String baseName = (titre == null || titre.isBlank()) ? "event" : titre.trim().replaceAll("[^a-zA-Z0-9-_]", "_");
        return aiService.generateSaveAndGet(prompt, baseName);
    }

    /**
     * Construit le prompt anglais pour l'IA (image visuelle type affiche, pas de texte dans l'image).
     */
    public static String buildPrompt(String titre, String desc, String type, String lieu) {
        StringBuilder sb = new StringBuilder();
        sb.append("Modern event poster, graphic design, visual only, no text or words in the image. ");
        sb.append("Theme: ").append(nullSafe(titre));
        if (type != null && !type.isBlank()) sb.append(", ").append(type.toLowerCase());
        if (lieu != null && !lieu.isBlank()) sb.append(", ").append(lieu);
        sb.append(". ");
        if (desc != null && !desc.isBlank()) sb.append("Mood: ").append(desc.length() > 80 ? desc.substring(0, 80) : desc).append(". ");
        sb.append("Bold colors, clean composition, abstract or illustrated style, professional, no explicit content.");
        return sb.toString();
    }

    private static String nullSafe(String s) {
        return s == null ? "" : s.trim();
    }
}
