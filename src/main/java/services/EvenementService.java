package services;

import entities.Evenement;
import interfaces.IEvenementService;
import utils.MyDataBase;

import java.sql.*;
import java.util.*;

public class EvenementService implements IEvenementService {

    private final Connection cnx;

    public EvenementService() throws SQLException {
        cnx = MyDataBase.getInstance().getCnx();
        if (cnx == null) {
            throw new SQLException("Connexion DB null (cnx=null). Vérifie MySQL et MyDataBase.");
        }
    }

    @Override
    public void add(Evenement e) throws SQLException {
        String sql = "INSERT INTO evenement (titre, description, type, date_debut, date_fin, lieu, image) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?)";

        try (PreparedStatement ps = cnx.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, e.getTitre());
            ps.setString(2, e.getDescription());
            ps.setString(3, e.getType());

            if (e.getDateDebut() == null) ps.setNull(4, Types.TIMESTAMP);
            else ps.setTimestamp(4, Timestamp.valueOf(e.getDateDebut()));

            if (e.getDateFin() == null) ps.setNull(5, Types.TIMESTAMP);
            else ps.setTimestamp(5, Timestamp.valueOf(e.getDateFin()));

            ps.setString(6, e.getLieu());

            // image (nullable)
            if (e.getImage() == null || e.getImage().isBlank()) ps.setNull(7, Types.VARCHAR);
            else ps.setString(7, e.getImage());

            ps.executeUpdate();

            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) e.setIdEvent(rs.getInt(1));
            }
        }
    }

    @Override
    public void update(Evenement e) throws SQLException {
        String sql = "UPDATE evenement SET titre=?, description=?, type=?, date_debut=?, date_fin=?, lieu=?, image=? " +
                "WHERE id_event=?";

        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setString(1, e.getTitre());
            ps.setString(2, e.getDescription());
            ps.setString(3, e.getType());

            if (e.getDateDebut() == null) ps.setNull(4, Types.TIMESTAMP);
            else ps.setTimestamp(4, Timestamp.valueOf(e.getDateDebut()));

            if (e.getDateFin() == null) ps.setNull(5, Types.TIMESTAMP);
            else ps.setTimestamp(5, Timestamp.valueOf(e.getDateFin()));

            ps.setString(6, e.getLieu());

            // image (nullable)
            if (e.getImage() == null || e.getImage().isBlank()) ps.setNull(7, Types.VARCHAR);
            else ps.setString(7, e.getImage());

            ps.setInt(8, e.getIdEvent());

            ps.executeUpdate();
        }
    }

    @Override
    public void delete(int id) throws SQLException {
        String sql = "DELETE FROM evenement WHERE id_event=?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }

    @Override
    public List<Evenement> getAll() throws SQLException {
        String sql = "SELECT * FROM evenement ORDER BY id_event DESC";
        List<Evenement> list = new ArrayList<>();
        try (PreparedStatement ps = cnx.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) list.add(map(rs));
        }
        return list;
    }

    @Override
    public Evenement getOneById(int id) throws SQLException {
        String sql = "SELECT * FROM evenement WHERE id_event=?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return null;
                return map(rs);
            }
        }
    }

    // ✅ Search SQL
    public List<Evenement> search(String keyword) throws SQLException {
        String k = (keyword == null) ? "" : keyword.toLowerCase().trim();

        String sql = """
            SELECT * FROM evenement
            WHERE LOWER(titre) LIKE ?
               OR LOWER(description) LIKE ?
               OR LOWER(lieu) LIKE ?
            ORDER BY id_event DESC
        """;

        List<Evenement> list = new ArrayList<>();
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            String like = "%" + k + "%";
            ps.setString(1, like);
            ps.setString(2, like);
            ps.setString(3, like);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(map(rs));
            }
        }
        return list;
    }

    // ===== Streams =====
    @Override
    public List<Evenement> rechercher(List<Evenement> events, String keyword) {
        String k = safe(keyword);
        return events.stream()
                .filter(e -> safe(e.getTitre()).contains(k)
                        || safe(e.getDescription()).contains(k)
                        || safe(e.getLieu()).contains(k))
                .toList();
    }

    @Override
    public List<Evenement> filtrerParType(List<Evenement> events, String type) {
        String t = safeUpper(type);
        return events.stream()
                .filter(e -> safeUpper(e.getType()).equals(t))
                .toList();
    }

    @Override
    public List<Evenement> filtrerParLieu(List<Evenement> events, String keyword) {
        String k = safe(keyword);
        return events.stream()
                .filter(e -> safe(e.getLieu()).contains(k))
                .toList();
    }

    @Override
    public List<Evenement> trierParDateAsc(List<Evenement> events) {
        return events.stream()
                .sorted(Comparator.comparing(Evenement::getDateDebut, Comparator.nullsLast(Comparator.naturalOrder())))
                .toList();
    }

    @Override
    public List<Evenement> trierParDateDesc(List<Evenement> events) {
        return events.stream()
                .sorted(Comparator.comparing(Evenement::getDateDebut, Comparator.nullsLast(Comparator.naturalOrder())).reversed())
                .toList();
    }

    // ===== helpers =====
    private Evenement map(ResultSet rs) throws SQLException {
        Evenement e = new Evenement();
        e.setIdEvent(rs.getInt("id_event"));
        e.setTitre(rs.getString("titre"));
        e.setDescription(rs.getString("description"));
        e.setType(rs.getString("type"));
        e.setLieu(rs.getString("lieu"));
        e.setImage(rs.getString("image"));

        Timestamp td = rs.getTimestamp("date_debut");
        Timestamp tf = rs.getTimestamp("date_fin");

        e.setDateDebut(td != null ? td.toLocalDateTime() : null);
        e.setDateFin(tf != null ? tf.toLocalDateTime() : null);

        return e;
    }

    private String safe(String s) {
        return s == null ? "" : s.toLowerCase(Locale.ROOT).trim();
    }

    private String safeUpper(String s) {
        return s == null ? "" : s.toUpperCase(Locale.ROOT).trim();
    }
}