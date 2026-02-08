package services;

import entities.Programme;
import utils.MyDataBase;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ServiceProgramme {

    private final Connection cnx;

    public ServiceProgramme() throws SQLException {
        cnx = MyDataBase.getInstance().getCnx();
    }

    // ===================== CREATE =====================
    public void add(Programme p) throws SQLException {
        String sql = "INSERT INTO programme (event_id, titre, debut, fin) VALUES (?, ?, ?, ?)";
        try (PreparedStatement ps = cnx.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, p.getEventId());
            ps.setString(2, p.getTitre());
            ps.setTimestamp(3, Timestamp.valueOf(p.getDebut()));
            ps.setTimestamp(4, Timestamp.valueOf(p.getFin()));
            ps.executeUpdate();

            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) p.setIdProg(rs.getInt(1));
            }
        }
    }

    // ===================== READ ONE =====================
    public Programme getOneById(int idProg) throws SQLException {
        String sql = "SELECT * FROM programme WHERE id_prog=?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, idProg);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return null;
                return map(rs);
            }
        }
    }

    // ===================== READ ALL =====================
    public List<Programme> getAll() throws SQLException {
        String sql = "SELECT * FROM programme ORDER BY id_prog DESC";
        List<Programme> list = new ArrayList<>();

        try (PreparedStatement ps = cnx.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(map(rs));
            }
        }
        return list;
    }

    // ===================== READ BY EVENT =====================
    public List<Programme> getByEventId(int eventId) throws SQLException {
        String sql = "SELECT * FROM programme WHERE event_id=? ORDER BY debut ASC";
        List<Programme> list = new ArrayList<>();

        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, eventId);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(map(rs));
                }
            }
        }
        return list;
    }

    // ===================== UPDATE =====================
    public void update(Programme p) throws SQLException {
        String sql = "UPDATE programme SET event_id=?, titre=?, debut=?, fin=? WHERE id_prog=?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, p.getEventId());
            ps.setString(2, p.getTitre());
            ps.setTimestamp(3, Timestamp.valueOf(p.getDebut()));
            ps.setTimestamp(4, Timestamp.valueOf(p.getFin()));
            ps.setInt(5, p.getIdProg());
            ps.executeUpdate();
        }
    }

    // ===================== DELETE =====================
    public void delete(int idProg) throws SQLException {
        String sql = "DELETE FROM programme WHERE id_prog=?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, idProg);
            ps.executeUpdate();
        }
    }

    // ===================== MAPPER =====================
    private Programme map(ResultSet rs) throws SQLException {
        Programme p = new Programme();
        p.setIdProg(rs.getInt("id_prog"));
        p.setEventId(rs.getInt("event_id"));
        p.setTitre(rs.getString("titre"));

        Timestamp d = rs.getTimestamp("debut");
        Timestamp f = rs.getTimestamp("fin");

        p.setDebut(d != null ? d.toLocalDateTime() : null);
        p.setFin(f != null ? f.toLocalDateTime() : null);

        return p;
    }
}
