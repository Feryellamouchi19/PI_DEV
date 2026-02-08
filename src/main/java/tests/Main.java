package tests;

import services.ServiceEvenement;

public class Main {
    public static void main(String[] args) throws Exception {
        ServiceEvenement se = new ServiceEvenement();
        System.out.println("✅ Projet prêt. Nb events = " + se.getAll().size());
    }
}
