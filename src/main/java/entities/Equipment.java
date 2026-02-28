package entities;

public class Equipment {

    private int id;
    private int eventId;
    private String libelle;

    public Equipment() {}

    public Equipment(int eventId, String libelle) {
        this.eventId = eventId;
        this.libelle = libelle;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getEventId() { return eventId; }
    public void setEventId(int eventId) { this.eventId = eventId; }

    public String getLibelle() { return libelle; }
    public void setLibelle(String libelle) { this.libelle = libelle; }
}
