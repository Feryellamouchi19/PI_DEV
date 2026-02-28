-- Table équipements pour les événements
CREATE TABLE IF NOT EXISTS equipment (
    id INT AUTO_INCREMENT PRIMARY KEY,
    event_id INT NOT NULL,
    libelle VARCHAR(255) NOT NULL,
    FOREIGN KEY (event_id) REFERENCES evenement(id_event) ON DELETE CASCADE
);
