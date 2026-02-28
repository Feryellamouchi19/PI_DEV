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
        return aiService.generateSaveAndGet(prompt, baseName, titre, description, type, lieu);
    }

    /**
     * Construit un prompt optimisé pour une vraie affiche d'événement :
     * style professionnel, scène visuelle riche, pas de texte dans l'image.
     */
    public static String buildPrompt(String titre, String desc, String type, String lieu) {
        StringBuilder sb = new StringBuilder();

        // Style affiche réelle en premier (guide le modèle)
        sb.append("Professional event poster, high quality, photorealistic or detailed illustration. ");
        sb.append("Like a real concert poster or festival flyer. ");

        // 1) Sujet principal
        String mainSubject = nullSafe(titre);
        if (!mainSubject.isBlank()) {
            sb.append("Main subject: ").append(mainSubject).append(". ");
        }

        // 2) Scène visuelle issue de la description
        String sceneDesc = extractSceneFromDescription(desc);
        if (!sceneDesc.isBlank()) {
            sb.append("Scene: ").append(sceneDesc).append(". ");
        }

        // 3) Éléments visuels selon le type
        String typeVisuals = getTypeVisualKeywords(type);
        if (!typeVisuals.isBlank()) {
            sb.append("Visual elements: ").append(typeVisuals).append(". ");
        }

        // 4) Lieu / décor
        if (lieu != null && !lieu.isBlank()) {
            sb.append("Setting: ").append(lieu.trim()).append(". ");
        }

        // 5) Contraintes
        sb.append("Vibrant colors, dynamic composition, artistic. ");
        sb.append("No text, no words, no letters in the image. ");
        sb.append("Safe for work.");

        return sb.toString();
    }

    /**
     * Extrait une description de scène exploitable (limite 180 caractères, première phrase prioritaire).
     */
    private static String extractSceneFromDescription(String desc) {
        if (desc == null || desc.isBlank()) return "";
        String s = desc.trim()
                .replaceAll("\\s+", " ")
                .replace("\r", " ")
                .replace("\n", " ");
        if (s.length() <= 180) return s;
        // Prendre la première phrase ou les 180 premiers caractères
        int cut = s.indexOf('.');
        if (cut > 0 && cut <= 200) return s.substring(0, cut).trim();
        return s.substring(0, 177).trim() + "...";
    }

    /**
     * Retourne des mots-clés visuels selon le type d'événement pour guider l'IA.
     */
    private static String getTypeVisualKeywords(String type) {
        if (type == null || type.isBlank()) return "";
        String t = type.trim().toUpperCase();
        return switch (t) {
            case "SOIREE" -> "nightlife, party, disco lights, dance floor, neon lights, crowd, music, celebration";
            case "RANDONNEE" -> "hiking trail, mountains, nature walk, forest path, outdoor adventure, landscape";
            case "CAMPING" -> "camping tent, campfire, forest, nature, outdoor, stars, wilderness";
            case "SEJOUR" -> "travel, vacation, scenic view, relaxation, destination, journey";
            default -> type.toLowerCase() + " event, gathering, people";
        };
    }

    private static String nullSafe(String s) {
        return s == null ? "" : s.trim();
    }
}
