package services;

import entities.Evenement;
import utils.MyDataBase;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class EvenementService {

    private final Connection cnx;

    public EvenementService() {
        cnx = MyDataBase.getInstance().getCnx();
    }

    public void add(Evenement e) {
        String sql = """
            INSERT INTO evenement (titre, description, type, date_debut, date_fin, lieu, image)
            VALUES (?, ?, ?, ?, ?, ?, ?)
        """;

        try (PreparedStatement ps = cnx.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setString(1, e.getTitre());
            ps.setString(2, e.getDescription());
            ps.setString(3, e.getType());

            if (e.getDateDebut() != null) ps.setTimestamp(4, Timestamp.valueOf(e.getDateDebut()));
            else ps.setNull(4, Types.TIMESTAMP);

            if (e.getDateFin() != null) ps.setTimestamp(5, Timestamp.valueOf(e.getDateFin()));
            else ps.setNull(5, Types.TIMESTAMP);

            ps.setString(6, e.getLieu());
            ps.setString(7, e.getImage());

            ps.executeUpdate();

            // ✅ récupérer l'ID généré
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    e.setIdEvent(rs.getInt(1));
                }
            }

        } catch (SQLException ex) {
            System.out.println("❌ EvenementService.add()");
            ex.printStackTrace();
        }
    }

    public List<Evenement> getAll() {
        List<Evenement> list = new ArrayList<>();

        String sql = """
            SELECT id_event, titre, description, type, date_debut, date_fin, lieu, image
            FROM evenement
            ORDER BY id_event DESC
        """;

        try (PreparedStatement ps = cnx.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                list.add(map(rs));
            }

        } catch (SQLException ex) {
            System.out.println("❌ EvenementService.getAll()");
            ex.printStackTrace();
        }

        return list;
    }

    public Evenement getOneById(int idEvent) {
        String sql = """
            SELECT id_event, titre, description, type, date_debut, date_fin, lieu, image
            FROM evenement
            WHERE id_event = ?
        """;

        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, idEvent);

            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return null;
                return map(rs);
            }

        } catch (SQLException ex) {
            System.out.println("❌ EvenementService.getOneById()");
            ex.printStackTrace();
            return null;
        }
    }

    public void update(Evenement e) {
        String sql = """
            UPDATE evenement
            SET titre=?, description=?, type=?, date_debut=?, date_fin=?, lieu=?, image=?
            WHERE id_event=?
        """;

        try (PreparedStatement ps = cnx.prepareStatement(sql)) {

            ps.setString(1, e.getTitre());
            ps.setString(2, e.getDescription());
            ps.setString(3, e.getType());

            if (e.getDateDebut() != null) ps.setTimestamp(4, Timestamp.valueOf(e.getDateDebut()));
            else ps.setNull(4, Types.TIMESTAMP);

            if (e.getDateFin() != null) ps.setTimestamp(5, Timestamp.valueOf(e.getDateFin()));
            else ps.setNull(5, Types.TIMESTAMP);

            ps.setString(6, e.getLieu());
            ps.setString(7, e.getImage());
            ps.setInt(8, e.getIdEvent());

            ps.executeUpdate();

        } catch (SQLException ex) {
            System.out.println("❌ EvenementService.update()");
            ex.printStackTrace();
        }
    }

    public void delete(int idEvent) {
        String sql = "DELETE FROM evenement WHERE id_event=?";

        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, idEvent);
            ps.executeUpdate();
        } catch (SQLException ex) {
            System.out.println("❌ EvenementService.delete()");
            ex.printStackTrace();
        }
    }

    private Evenement map(ResultSet rs) throws SQLException {
        Evenement e = new Evenement();
        e.setIdEvent(rs.getInt("id_event"));
        e.setTitre(rs.getString("titre"));
        e.setDescription(rs.getString("description"));
        e.setType(rs.getString("type"));
        e.setLieu(rs.getString("lieu"));
        e.setImage(rs.getString("image"));

        Timestamp d1 = rs.getTimestamp("date_debut");
        Timestamp d2 = rs.getTimestamp("date_fin");
        if (d1 != null) e.setDateDebut(d1.toLocalDateTime());
        if (d2 != null) e.setDateFin(d2.toLocalDateTime());

        return e;
    }
}