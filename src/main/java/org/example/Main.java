package tests;

import services.EvenementService;

public class Main {
    public static void main(String[] args) throws Exception {
        EvenementService se = new EvenementService();
        System.out.println("✅ Projet prêt. Nb events = " + se.getAll().size());
    }
}
