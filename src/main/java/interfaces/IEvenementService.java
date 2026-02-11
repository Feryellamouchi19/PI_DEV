package interfaces;

import entities.Evenement;

import java.sql.SQLException;
import java.util.List;

public interface IEvenementService extends IService<Evenement> {

    // Streams (recherche/filtre/tri) sur une liste
    List<Evenement> rechercher(List<Evenement> events, String keyword);
    List<Evenement> filtrerParType(List<Evenement> events, String type);
    List<Evenement> filtrerParLieu(List<Evenement> events, String keyword);
    List<Evenement> trierParDateAsc(List<Evenement> events);
    List<Evenement> trierParDateDesc(List<Evenement> events);
}
