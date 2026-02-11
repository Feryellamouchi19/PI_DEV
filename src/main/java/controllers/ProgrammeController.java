package controllers;

import entities.Programme;
import interfaces.IProgrammeService;

import java.sql.SQLException;
import java.util.List;

public class ProgrammeController {

    private final IProgrammeService service;

    public ProgrammeController(IProgrammeService service) {
        this.service = service;
    }

    public void ajouter(Programme p) throws SQLException { service.add(p); }
    public void modifier(Programme p) throws SQLException { service.update(p); }
    public void supprimer(int idProg) throws SQLException { service.delete(idProg); }

    public Programme afficherParId(int idProg) throws SQLException { return service.getOneById(idProg); }
    public List<Programme> afficherTous() throws SQLException { return service.getAll(); }
    public List<Programme> afficherParEvent(int eventId) throws SQLException { return service.getByEventId(eventId); }
}
