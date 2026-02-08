package entities;

import java.time.LocalDateTime;

public class Programme {
    private int idProg;
    private int eventId;
    private String titre;
    private LocalDateTime debut;
    private LocalDateTime fin;

    public Programme() {}

    public Programme(int eventId, String titre, LocalDateTime debut, LocalDateTime fin) {
        this.eventId = eventId;
        this.titre = titre;
        this.debut = debut;
        this.fin = fin;
    }

    public int getIdProg() { return idProg; }
    public void setIdProg(int idProg) { this.idProg = idProg; }

    public int getEventId() { return eventId; }
    public void setEventId(int eventId) { this.eventId = eventId; }

    public String getTitre() { return titre; }
    public void setTitre(String titre) { this.titre = titre; }

    public LocalDateTime getDebut() { return debut; }
    public void setDebut(LocalDateTime debut) { this.debut = debut; }

    public LocalDateTime getFin() { return fin; }
    public void setFin(LocalDateTime fin) { this.fin = fin; }

    @Override
    public String toString() {
        return "Programme{" +
                "idProg=" + idProg +
                ", eventId=" + eventId +
                ", titre='" + titre + '\'' +
                ", debut=" + debut +
                ", fin=" + fin +
                '}';
    }
}
