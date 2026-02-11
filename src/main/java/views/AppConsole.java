package views;

import entities.Evenement;
import entities.Programme;
import services.EvenementService;
import services.ProgrammeService;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Scanner;

public class AppConsole {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final Scanner sc = new Scanner(System.in);

    private final EvenementService se;
    private final ProgrammeService sp;

    public AppConsole() throws Exception {
        se = new EvenementService();
        sp = new ProgrammeService();
    }

    public static void main(String[] args) throws Exception {
        new AppConsole().run();
    }

    private void run() throws Exception {
        while (true) {
            System.out.println("\n==============================");
            System.out.println("   GESTION EVENEMENTS");
            System.out.println("==============================");
            System.out.println("1) Ajouter un evenement + programmes");
            System.out.println("2) Afficher tous les evenements");
            System.out.println("3) Rechercher un evenement (keyword)");
            System.out.println("4) Filtrer par type");
            System.out.println("5) Filtrer par lieu");
            System.out.println("6) Trier par date (ASC / DESC)");
            System.out.println("7) Afficher un evenement + ses programmes (par ID)");
            System.out.println("0) Quitter");
            System.out.print("Choix: ");

            String choix = sc.nextLine().trim();
            switch (choix) {
                case "1" -> ajouterEvenementEtProgrammes();
                case "2" -> afficherTous();
                case "3" -> rechercher();
                case "4" -> filtrerType();
                case "5" -> filtrerLieu();
                case "6" -> trierDate();
                case "7" -> afficherEventEtProg();
                case "0" -> {
                    System.out.println("Bye 👋");
                    return;
                }
                default -> System.out.println("❌ Choix invalide.");
            }
        }
    }

    // ===================== 1) AJOUT EVENT + PROGRAMMES =====================

    private void ajouterEvenementEtProgrammes() throws Exception {
        System.out.println("\n=== AJOUT EVENEMENT ===");

        String titre = lireTexte("Titre: ", 3, 150);
        String desc  = lireTexte("Description: ", 3, 500);
        String lieu  = lireTexte("Lieu: ", 2, 150);
        String type  = lireType();

        LocalDateTime debut = lireDate("Date debut (yyyy-MM-dd HH:mm): ");
        LocalDateTime fin = null;

        if (type.equals("CAMPING") || type.equals("SEJOUR")) {
            fin = lireDateApres("Date fin   (yyyy-MM-dd HH:mm): ", debut);
        } else {
            System.out.println("ℹ️ Type " + type + " => date_fin non requise.");
        }

        Evenement e = new Evenement(titre, desc, type, debut, fin, lieu);
        se.add(e);

        int eventId = e.getIdEvent();
        System.out.println("✅ Evenement ajouté avec ID = " + eventId);

        // Ajouter programme(s) liés
        while (lireOuiNon("Ajouter un programme pour cet evenement ? (oui/non): ")) {
            ajouterProgrammePourEvent(e);
        }

        System.out.println("➡️ Terminé : event + programmes enregistrés.");
    }

    private void ajouterProgrammePourEvent(Evenement e) throws Exception {
        System.out.println("\n=== AJOUT PROGRAMME (event_id=" + e.getIdEvent() + ") ===");

        String ptitre = lireTexte("Titre programme: ", 2, 150);
        LocalDateTime pdebut = lireDate("Debut programme (yyyy-MM-dd HH:mm): ");
        LocalDateTime pfin   = lireDateApres("Fin programme   (yyyy-MM-dd HH:mm): ", pdebut);

        // Contrôles selon type d'événement
        if (e.getType().equals("CAMPING") || e.getType().equals("SEJOUR")) {
            if (e.getDateFin() == null) {
                System.out.println("❌ Erreur: date_fin de l'événement est NULL alors que type = " + e.getType());
                return;
            }
            if (pdebut.isBefore(e.getDateDebut()) || pfin.isAfter(e.getDateFin())) {
                System.out.println("❌ Programme doit être entre " + e.getDateDebut().format(FMT) + " et " + e.getDateFin().format(FMT));
                return;
            }
        } else {
            if (pdebut.isBefore(e.getDateDebut())) {
                System.out.println("❌ Programme doit commencer après le début de l'événement: " + e.getDateDebut().format(FMT));
                return;
            }
        }

        Programme p = new Programme(e.getIdEvent(), ptitre, pdebut, pfin);
        sp.add(p);

        System.out.println("✅ Programme ajouté (id_prog=" + p.getIdProg() + ")");
    }

    // ===================== 2) AFFICHAGE =====================

    private void afficherTous() throws Exception {
        List<Evenement> events = se.getAll();
        System.out.println("\n=== EVENEMENTS (" + events.size() + ") ===");
        events.forEach(System.out::println);
    }

    private void afficherEventEtProg() throws Exception {
        int id = lireInt("ID evenement: ");
        Evenement e = se.getOneById(id);
        if (e == null) {
            System.out.println("❌ Evenement introuvable.");
            return;
        }
        System.out.println("Event: " + e);

        List<Programme> progs = sp.getByEventId(id);
        System.out.println("Programmes (" + progs.size() + "):");
        progs.forEach(p -> System.out.println("  - " + p));
    }

    // ===================== 3) RECHERCHE / FILTRE / TRI =====================

    // Recherche keyword dans titre/description/lieu (Stream)
    private void rechercher() throws Exception {
        System.out.print("Keyword: ");
        String keyword = sc.nextLine().trim().toLowerCase(Locale.ROOT);

        List<Evenement> events = se.getAll();
        List<Evenement> result = events.stream()
                .filter(e ->
                        safe(e.getTitre()).contains(keyword) ||
                                safe(e.getDescription()).contains(keyword) ||
                                safe(e.getLieu()).contains(keyword)
                )
                .toList();

        System.out.println("\nRésultats (" + result.size() + "):");
        result.forEach(System.out::println);
    }

    private void filtrerType() throws Exception {
        System.out.print("Type (SOIREE/RANDONNEE/CAMPING/SEJOUR): ");
        String t = sc.nextLine().trim().toUpperCase(Locale.ROOT);

        List<Evenement> events = se.getAll();
        List<Evenement> result = events.stream()
                .filter(e -> safeUpper(e.getType()).equals(t))
                .toList();

        System.out.println("\nRésultats (" + result.size() + "):");
        result.forEach(System.out::println);
    }

    private void filtrerLieu() throws Exception {
        System.out.print("Lieu contient: ");
        String k = sc.nextLine().trim().toLowerCase(Locale.ROOT);

        List<Evenement> events = se.getAll();
        List<Evenement> result = events.stream()
                .filter(e -> safe(e.getLieu()).contains(k))
                .toList();

        System.out.println("\nRésultats (" + result.size() + "):");
        result.forEach(System.out::println);
    }

    private void trierDate() throws Exception {
        System.out.print("ASC ou DESC ?: ");
        String mode = sc.nextLine().trim().toUpperCase(Locale.ROOT);

        List<Evenement> events = se.getAll();

        Comparator<Evenement> cmp = Comparator.comparing(Evenement::getDateDebut,
                Comparator.nullsLast(Comparator.naturalOrder()));

        if (mode.equals("DESC")) {
            events = events.stream().sorted(cmp.reversed()).toList();
        } else {
            events = events.stream().sorted(cmp).toList();
        }

        System.out.println("\nTri (" + mode + "):");
        events.forEach(System.out::println);
    }

    // ===================== HELPERS SAISIE =====================

    private int lireInt(String msg) {
        while (true) {
            try {
                System.out.print(msg);
                return Integer.parseInt(sc.nextLine().trim());
            } catch (Exception e) {
                System.out.println("❌ Entier invalide.");
            }
        }
    }

    private String lireTexte(String msg, int min, int max) {
        while (true) {
            System.out.print(msg);
            String s = sc.nextLine().trim();
            if (s.isEmpty()) { System.out.println("❌ Champ obligatoire."); continue; }
            if (s.length() < min) { System.out.println("❌ Trop court (min " + min + ")."); continue; }
            if (s.length() > max) { System.out.println("❌ Trop long (max " + max + ")."); continue; }
            return s;
        }
    }

    private String lireType() {
        while (true) {
            System.out.print("Type (SOIREE/RANDONNEE/CAMPING/SEJOUR): ");
            String t = sc.nextLine().trim().toUpperCase(Locale.ROOT);
            if (t.equals("SOIREE") || t.equals("RANDONNEE") || t.equals("CAMPING") || t.equals("SEJOUR")) return t;
            System.out.println("❌ Type invalide.");
        }
    }

    private LocalDateTime lireDate(String msg) {
        while (true) {
            try {
                System.out.print(msg);
                return LocalDateTime.parse(sc.nextLine().trim(), FMT);
            } catch (Exception e) {
                System.out.println("❌ Format invalide. Exemple: 2026-02-06 20:00");
            }
        }
    }

    private LocalDateTime lireDateApres(String msg, LocalDateTime min) {
        while (true) {
            LocalDateTime d = lireDate(msg);
            if (d.isAfter(min)) return d;
            System.out.println("❌ Doit être après " + min.format(FMT));
        }
    }

    private boolean lireOuiNon(String msg) {
        while (true) {
            System.out.print(msg);
            String rep = sc.nextLine().trim().toLowerCase(Locale.ROOT);
            if (rep.equals("oui")) return true;
            if (rep.equals("non")) return false;
            System.out.println("❌ Réponds par 'oui' ou 'non'.");
        }
    }

    private String safe(String s) {
        return s == null ? "" : s.toLowerCase(Locale.ROOT).trim();
    }

    private String safeUpper(String s) {
        return s == null ? "" : s.toUpperCase(Locale.ROOT).trim();
    }
}
