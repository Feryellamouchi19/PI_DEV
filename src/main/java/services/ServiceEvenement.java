package services;

import entities.Evenement;
import utils.MyDataBase;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ServiceEvenement implements IService<Evenement> {

    private final Connection cnx;

    public ServiceEvenement() throws SQLException {
        cnx = MyDataBase.getInstance().getCnx();
    }

    @Override
    public void add(Evenement e) throws SQLException {
        String sql = "INSERT INTO evenement (titre, description, type, date_debut, date_fin, lieu) VALUES (?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = cnx.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, e.getTitre());
            ps.setString(2, e.getDescription());
            ps.setString(3, e.getType());
            ps.setTimestamp(4, Timestamp.valueOf(e.getDateDebut()));

            if (e.getDateFin() == null) ps.setNull(5, Types.TIMESTAMP);
            else ps.setTimestamp(5, Timestamp.valueOf(e.getDateFin()));

            ps.setString(6, e.getLieu());

            ps.executeUpdate();

            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) e.setIdEvent(rs.getInt(1));
            }
        }
    }

    @Override
    public void update(Evenement e) throws SQLException {
        String sql = "UPDATE evenement SET titre=?, description=?, type=?, date_debut=?, date_fin=?, lieu=? WHERE id_event=?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setString(1, e.getTitre());
            ps.setString(2, e.getDescription());
            ps.setString(3, e.getType());
            ps.setTimestamp(4, Timestamp.valueOf(e.getDateDebut()));

            if (e.getDateFin() == null) ps.setNull(5, Types.TIMESTAMP);
            else ps.setTimestamp(5, Timestamp.valueOf(e.getDateFin()));

            ps.setString(6, e.getLieu());
            ps.setInt(7, e.getIdEvent());

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

            while (rs.next()) {
                list.add(map(rs));
            }
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

    private Evenement map(ResultSet rs) throws SQLException {
        Evenement e = new Evenement();
        e.setIdEvent(rs.getInt("id_event"));
        e.setTitre(rs.getString("titre"));
        e.setDescription(rs.getString("description"));
        e.setType(rs.getString("type"));

        Timestamp td = rs.getTimestamp("date_debut");
        Timestamp tf = rs.getTimestamp("date_fin");

        e.setDateDebut(td != null ? td.toLocalDateTime() : null);
        e.setDateFin(tf != null ? tf.toLocalDateTime() : null);

        e.setLieu(rs.getString("lieu"));
        return e;
    }
}
