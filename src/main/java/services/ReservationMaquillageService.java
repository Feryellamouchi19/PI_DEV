package services;

import utils.MyDataBase;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class ReservationMaquillageService {

    private final Connection cnx;

    public ReservationMaquillageService() {
        cnx = MyDataBase.getInstance().getCnx();
        ensureTableExists();
    }

    private void ensureTableExists() {
        String sql = "CREATE TABLE IF NOT EXISTS reservation_maquillage (" +
                "id INT AUTO_INCREMENT PRIMARY KEY, " +
                "event_id INT NOT NULL, " +
                "email VARCHAR(255) NOT NULL, " +
                "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
                "FOREIGN KEY (event_id) REFERENCES evenement(id_event) ON DELETE CASCADE)";
        try (Statement st = cnx.createStatement()) {
            st.executeUpdate(sql);
        } catch (SQLException ex) {
            System.err.println("❌ ReservationMaquillageService.ensureTableExists: " + ex.getMessage());
        }
    }

    public void add(int eventId, String email) throws SQLException {
        String sql = "INSERT INTO reservation_maquillage (event_id, email) VALUES (?, ?)";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, eventId);
            ps.setString(2, email == null ? "" : email.trim());
            ps.executeUpdate();
        }
    }

    public boolean exists(int eventId, String email) {
        if (email == null || email.trim().isBlank()) return false;
        String sql = "SELECT 1 FROM reservation_maquillage WHERE event_id = ? AND LOWER(TRIM(email)) = LOWER(TRIM(?)) LIMIT 1";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, eventId);
            ps.setString(2, email.trim());
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException ex) {
            return false;
        }
    }
}
