package services;

import entities.Evenement;

import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

public class RecommendationService {

    public List<Evenement> recommendFromFilter(FilterCriteria c,
                                               List<Evenement> allEvents,
                                               List<Evenement> filteredEvents,
                                               int limit) {

        if (allEvents == null) return List.of();
        if (filteredEvents == null) filteredEvents = List.of();

        Set<Integer> filteredIds = filteredEvents.stream()
                .map(Evenement::getIdEvent)
                .collect(Collectors.toSet());

        java.time.LocalDateTime now = java.time.LocalDateTime.now();
        return allEvents.stream()
                .filter(e -> e != null)
                .filter(e -> !filteredIds.contains(e.getIdEvent())) // pas dupliquer
                .map(e -> new Scored(e, score(e, c)))
                .filter(s -> s.score > 0)
                .sorted((a, b) -> {
                    int cmp = Integer.compare(b.score, a.score);
                    if (cmp != 0) return cmp;
                    // Même score : tri par proximité à maintenant (plus proche en premier)
                    if (a.event.getDateDebut() == null && b.event.getDateDebut() == null) return 0;
                    if (a.event.getDateDebut() == null) return 1;
                    if (b.event.getDateDebut() == null) return -1;
                    long aDist = Math.abs(ChronoUnit.MINUTES.between(a.event.getDateDebut(), now));
                    long bDist = Math.abs(ChronoUnit.MINUTES.between(b.event.getDateDebut(), now));
                    return Long.compare(aDist, bDist);
                })
                .limit(limit)
                .map(s -> s.event)
                .toList();
    }

    private int score(Evenement e, FilterCriteria c) {
        if (e == null || c == null) return 0;

        int s = 0;

        // 1) TYPE (gros poids)
        if (notBlank(c.getType()) && !"TOUS".equalsIgnoreCase(c.getType()) && notBlank(e.getType())) {
            if (c.getType().trim().equalsIgnoreCase(e.getType().trim())) s += 60;
        }

        // 2) DATE (proximité)
        if (c.getDateFrom() != null && e.getDateDebut() != null) {
            long diff = Math.abs(ChronoUnit.DAYS.between(e.getDateDebut(), c.getDateFrom()));
            if (diff <= 7) s += 25;
            else if (diff <= 30) s += 10;
        }

        if (c.getDateTo() != null && e.getDateDebut() != null) {
            if (!e.getDateDebut().isAfter(c.getDateTo())) s += 10;
        }

        // 3) KEYWORD (titre/description/lieu)
        if (notBlank(c.getKeyword())) {
            String kw = c.getKeyword().trim().toLowerCase(Locale.ROOT);
            String hay = (safe(e.getTitre()) + " " + safe(e.getDescription()) + " " + safe(e.getLieu()))
                    .toLowerCase(Locale.ROOT);

            if (hay.contains(kw)) s += 30;
        }

        // Si l'utilisateur n'a rien filtré, on donne quand même un score pour les "suggestions"
        // (événements à venir, par proximité de date)
        boolean noFilter =
                (c.getType() == null || c.getType().isBlank() || "TOUS".equalsIgnoreCase(c.getType())) &&
                        c.getDateFrom() == null &&
                        c.getDateTo() == null &&
                        (c.getKeyword() == null || c.getKeyword().isBlank());

        if (noFilter) {
            // Suggestions : événements à venir en priorité (score 10), sinon passés récents (score 5) pour toujours afficher quelque chose
            java.time.LocalDateTime now = java.time.LocalDateTime.now();
            if (e.getDateDebut() == null) return 5; // sans date, on affiche quand même
            if (e.getDateDebut().isAfter(now)) return 10; // à venir
            return 5; // passé : on affiche aussi en suggestions
        }

        return s;
    }

    private static class Scored {
        Evenement event;
        int score;
        Scored(Evenement e, int s) { this.event = e; this.score = s; }
    }

    private boolean notBlank(String s) { return s != null && !s.trim().isBlank(); }
    private String safe(String s) { return s == null ? "" : s; }
}