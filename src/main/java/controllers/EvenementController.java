package controllers;

import entities.Evenement;
import interfaces.IEvenementService;

import java.sql.SQLException;
import java.util.List;

public class EvenementController {

    private final IEvenementService service;

    public EvenementController(IEvenementService service) {
        this.service = service;
    }

    // CRUD
    public void ajouter(Evenement e) throws SQLException { service.add(e); }
    public void modifier(Evenement e) throws SQLException { service.update(e); }
    public void supprimer(int id) throws SQLException { service.delete(id); }
    public List<Evenement> afficherTous() throws SQLException { return service.getAll(); }
    public Evenement afficherParId(int id) throws SQLException { return service.getOneById(id); }

    // Search/Filter/Sort (Streams)
    public List<Evenement> rechercher(String keyword) throws SQLException {
        return service.rechercher(service.getAll(), keyword);
    }
    public List<Evenement> filtrerType(String type) throws SQLException {
        return service.filtrerParType(service.getAll(), type);
    }
    public List<Evenement> filtrerLieu(String lieu) throws SQLException {
        return service.filtrerParLieu(service.getAll(), lieu);
    }
    public List<Evenement> trierDateAsc() throws SQLException {
        return service.trierParDateAsc(service.getAll());
    }
    public List<Evenement> trierDateDesc() throws SQLException {
        return service.trierParDateDesc(service.getAll());
    }
}
