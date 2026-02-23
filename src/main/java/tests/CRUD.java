package tests;

import entities.Evenement;
import entities.Programme;
import services.EvenementService;
import services.ProgrammeService;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Scanner;

public class CRUD {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final Scanner sc = new Scanner(System.in);

    private final EvenementService se = new EvenementService();
    private final ProgrammeService sp = new ProgrammeService();

    public static void main(String[] args) {
        new CRUD().run();
    }

    private void run() {
        while (true) {
            System.out.println("\n==============================");
            System.out.println("   CRUD EVENEMENT / PROGRAMME ");
            System.out.println("==============================");
            System.out.println("1) Ajouter Evenement");
            System.out.println("2) Afficher Tous les Evenements");
            System.out.println("3) Afficher Evenement + Programmes (par ID)");
            System.out.println("4) Modifier Evenement");
            System.out.println("5) Supprimer Evenement");
            System.out.println("6) Ajouter Programme");
            System.out.println("7) Modifier Programme (par id_prog)");
            System.out.println("8) Supprimer Programme (par id_prog)");
            System.out.println("9) Afficher Tous les Programmes");
            System.out.println("0) Quitter");
            System.out.print("Choix: ");

            String choix = sc.nextLine().trim();

            try {
                switch (choix) {
                    case "1" -> ajouterEvenement();
                    case "2" -> afficherTousEvenements();
                    case "3" -> afficherEvenementEtProgrammes();
                    case "4" -> modifierEvenement();
                    case "5" -> supprimerEvenement();
                    case "6" -> ajouterProgramme();
                    case "7" -> modifierProgramme();
                    case "8" -> supprimerProgramme();
                    case "9" -> afficherTousProgrammes();
                    case "0" -> { System.out.println("Bye 👋"); return; }
                    default -> System.out.println("❌ Choix invalide.");
                }
            } catch (SQLException ex) {
                System.out.println("❌ SQL Error: " + ex.getMessage());
                ex.printStackTrace();
            } catch (Exception ex) {
                System.out.println("❌ Error: " + ex.getMessage());
                ex.printStackTrace();
            }
        }
    }

    // ===================== EVENEMENT =====================

    private void ajouterEvenement() {
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

        String imageFileName = lireTexteOptionnel("Image (ex: logo.png) [optionnel]: ", 1, 200);
        if (imageFileName == null) imageFileName = "";

        // ⚠️ Ce constructeur doit exister dans Evenement:
        // (titre, description, type, dateDebut, dateFin, lieu, image)
        Evenement e = new Evenement(titre, desc, type, debut, fin, lieu, imageFileName);

        // EvenementService.add() ne throw pas, il catch SQL à l'intérieur
        se.add(e);

        System.out.println("✅ Evenement ajouté.");
    }

    private void afficherTousEvenements() {
        List<Evenement> list = se.getAll();
        System.out.println("\n=== LISTE EVENEMENTS (" + list.size() + ") ===");
        list.forEach(System.out::println);
    }

    private void afficherEvenementEtProgrammes() throws SQLException {
        int id = lireInt("ID evenement: ");
        Evenement e = se.getOneById(id);

        if (e == null) {
            System.out.println("❌ Evenement introuvable.");
            return;
        }

        System.out.println("\nEvent: " + e);

        List<Programme> progs = sp.getByEventId(id);
        System.out.println("Programmes (" + progs.size() + "):");
        progs.forEach(p -> System.out.println("  - " + p));
    }

    private void modifierEvenement() {
        int id = lireInt("ID evenement à modifier: ");
        Evenement e = se.getOneById(id);

        if (e == null) {
            System.out.println("❌ Evenement introuvable.");
            return;
        }

        System.out.println("Actuel: " + e);
        System.out.println("Laisse vide pour garder la valeur.");

        String titre = lireTexteOptionnel("Nouveau titre: ", 3, 150);
        if (titre != null) e.setTitre(titre);

        String desc = lireTexteOptionnel("Nouvelle description: ", 3, 500);
        if (desc != null) e.setDescription(desc);

        String lieu = lireTexteOptionnel("Nouveau lieu: ", 2, 150);
        if (lieu != null) e.setLieu(lieu);

        String type = lireTypeOptionnel("Nouveau type (SOIREE/RANDONNEE/CAMPING/SEJOUR): ");
        if (type != null) e.setType(type);

        if (lireOuiNon("Modifier date_debut ? (oui/non): ")) {
            e.setDateDebut(lireDate("Date debut (yyyy-MM-dd HH:mm): "));
        }

        if (e.getType().equals("CAMPING") || e.getType().equals("SEJOUR")) {
            e.setDateFin(lireDateApres("Date fin (yyyy-MM-dd HH:mm): ", e.getDateDebut()));
        } else {
            e.setDateFin(null);
        }

        String image = lireTexteOptionnel("Nouvelle image (ex: logo.png) [optionnel]: ", 1, 200);
        if (image != null) e.setImage(image);

        se.update(e);
        System.out.println("✅ Evenement modifié.");
    }

    private void supprimerEvenement() {
        int id = lireInt("ID evenement à supprimer: ");
        se.delete(id);
        System.out.println("✅ Evenement supprimé.");
    }

    // ===================== PROGRAMME =====================

    private void ajouterProgramme() throws SQLException {
        int eventId = lireInt("event_id (ID evenement): ");
        Evenement e = se.getOneById(eventId);

        if (e == null) {
            System.out.println("❌ Evenement introuvable.");
            return;
        }

        System.out.println("Event: " + e.getTitre() + " [" + e.getType() + "]");

        String titre = lireTexte("Titre programme: ", 2, 150);
        LocalDateTime debut = lireDate("Debut programme (yyyy-MM-dd HH:mm): ");
        LocalDateTime fin = lireDateApres("Fin programme   (yyyy-MM-dd HH:mm): ", debut);

        if ((e.getType().equals("CAMPING") || e.getType().equals("SEJOUR")) && e.getDateFin() != null) {
            if (debut.isBefore(e.getDateDebut()) || fin.isAfter(e.getDateFin())) {
                System.out.println("❌ Programme doit être entre " + e.getDateDebut().format(FMT) + " et " + e.getDateFin().format(FMT));
                return;
            }
        } else {
            if (debut.isBefore(e.getDateDebut())) {
                System.out.println("❌ Programme doit commencer après le début de l'événement: " + e.getDateDebut().format(FMT));
                return;
            }
        }

        Programme p = new Programme(eventId, titre, debut, fin);
        sp.add(p);

        System.out.println("✅ Programme ajouté. id_prog = " + p.getIdProg());
    }

    private void modifierProgramme() throws SQLException {
        int idProg = lireInt("id_prog à modifier: ");
        Programme p = sp.getOneById(idProg);

        if (p == null) {
            System.out.println("❌ Programme introuvable.");
            return;
        }

        System.out.println("Actuel: " + p);

        String titre = lireTexteOptionnel("Nouveau titre: ", 2, 150);
        if (titre != null) p.setTitre(titre);

        if (lireOuiNon("Modifier dates ? (oui/non): ")) {
            LocalDateTime debut = lireDate("Debut (yyyy-MM-dd HH:mm): ");
            LocalDateTime fin = lireDateApres("Fin   (yyyy-MM-dd HH:mm): ", debut);
            p.setDebut(debut);
            p.setFin(fin);
        }

        sp.update(p);
        System.out.println("✅ Programme modifié.");
    }

    private void supprimerProgramme() throws SQLException {
        int idProg = lireInt("id_prog à supprimer: ");
        sp.delete(idProg);
        System.out.println("✅ Programme supprimé.");
    }

    private void afficherTousProgrammes() throws SQLException {
        List<Programme> list = sp.getAll();
        System.out.println("\n=== LISTE PROGRAMMES (" + list.size() + ") ===");
        list.forEach(System.out::println);
    }

    // ===================== HELPERS =====================

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

    private String lireTexteOptionnel(String msg, int min, int max) {
        System.out.print(msg);
        String s = sc.nextLine().trim();
        if (s.isEmpty()) return null;
        if (s.length() < min) { System.out.println("❌ Trop court. Ignoré."); return null; }
        if (s.length() > max) { System.out.println("❌ Trop long. Ignoré."); return null; }
        return s;
    }

    private String lireType() {
        while (true) {
            System.out.print("Type (SOIREE/RANDONNEE/CAMPING/SEJOUR): ");
            String t = sc.nextLine().trim().toUpperCase();
            if (t.equals("SOIREE") || t.equals("RANDONNEE") || t.equals("CAMPING") || t.equals("SEJOUR")) return t;
            System.out.println("❌ Type invalide.");
        }
    }

    private String lireTypeOptionnel(String msg) {
        System.out.print(msg);
        String t = sc.nextLine().trim();
        if (t.isEmpty()) return null;
        t = t.toUpperCase();
        if (t.equals("SOIREE") || t.equals("RANDONNEE") || t.equals("CAMPING") || t.equals("SEJOUR")) return t;
        System.out.println("❌ Type invalide. Ignoré.");
        return null;
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
            String rep = sc.nextLine().trim().toLowerCase();
            if (rep.equals("oui")) return true;
            if (rep.equals("non")) return false;
            System.out.println("❌ Réponds par 'oui' ou 'non'.");
        }
    }
}