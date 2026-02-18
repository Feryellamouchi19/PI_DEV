package entities;

import java.time.LocalDateTime;

public class Evenement {
    private int idEvent;
    private String titre;
    private String description;
    private String type; // SOIREE/RANDONNEE/CAMPING/SEJOUR
    private LocalDateTime dateDebut;
    private LocalDateTime dateFin; // nullable
    private String lieu;

    public Evenement() {}

    public Evenement(String titre, String description, String type,
                     LocalDateTime dateDebut, LocalDateTime dateFin, String lieu) {
        this.titre = titre;
        this.description = description;
        this.type = type;
        this.dateDebut = dateDebut;
        this.dateFin = dateFin;
        this.lieu = lieu;
    }

    public int getIdEvent() { return idEvent; }
    public void setIdEvent(int idEvent) { this.idEvent = idEvent; }

    public String getTitre() { return titre; }
    public void setTitre(String titre) { this.titre = titre; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public LocalDateTime getDateDebut() { return dateDebut; }
    public void setDateDebut(LocalDateTime dateDebut) { this.dateDebut = dateDebut; }

    public LocalDateTime getDateFin() { return dateFin; }
    public void setDateFin(LocalDateTime dateFin) { this.dateFin = dateFin; }

    public String getLieu() { return lieu; }
    public void setLieu(String lieu) { this.lieu = lieu; }

    @Override
    public String toString() {
        return "Evenement{" +
                "idEvent=" + idEvent +
                ", titre='" + titre + '\'' +
                ", type='" + type + '\'' +
                ", dateDebut=" + dateDebut +
                ", dateFin=" + dateFin +
                ", lieu='" + lieu + '\'' +
                '}';
    }
    private String image;

    public String getImage() { return image; }
    public void setImage(String image) { this.image = image; }
}
