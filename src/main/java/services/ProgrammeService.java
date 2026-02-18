package services;

import entities.Programme;
import interfaces.IProgrammeService;
import utils.MyDataBase;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ProgrammeService implements IProgrammeService {

    private final Connection cnx;

    public ProgrammeService() throws SQLException {
        cnx = MyDataBase.getInstance().getCnx();
    }

    @Override
    public void add(Programme p) throws SQLException {

        String sql = "INSERT INTO programme (event_id, titre, date_debut, date_fin) VALUES (?, ?, ?, ?)";

        try (PreparedStatement ps = cnx.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setInt(1, p.getEventId());
            ps.setString(2, p.getTitre());

            // date_debut (NOT NULL)
            ps.setTimestamp(3, Timestamp.valueOf(p.getDebut()));

            // date_fin (peut être NULL)
            if (p.getFin() != null) {
                ps.setTimestamp(4, Timestamp.valueOf(p.getFin()));
            } else {
                ps.setNull(4, Types.TIMESTAMP);
            }

            ps.executeUpdate();

            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    p.setIdProg(rs.getInt(1));
                }
            }
        }
    }

    @Override
    public void update(Programme p) throws SQLException {

        String sql = "UPDATE programme SET event_id=?, titre=?, date_debut=?, date_fin=? WHERE id_prog=?";

        try (PreparedStatement ps = cnx.prepareStatement(sql)) {

            ps.setInt(1, p.getEventId());
            ps.setString(2, p.getTitre());
            ps.setTimestamp(3, Timestamp.valueOf(p.getDebut()));

            if (p.getFin() != null) {
                ps.setTimestamp(4, Timestamp.valueOf(p.getFin()));
            } else {
                ps.setNull(4, Types.TIMESTAMP);
            }

            ps.setInt(5, p.getIdProg());
            ps.executeUpdate();
        }
    }

    @Override
    public void delete(int id) throws SQLException {

        String sql = "DELETE FROM programme WHERE id_prog=?";

        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }

    @Override
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

    @Override
    public Programme getOneById(int id) throws SQLException {

        String sql = "SELECT * FROM programme WHERE id_prog=?";

        try (PreparedStatement ps = cnx.prepareStatement(sql)) {

            ps.setInt(1, id);

            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return null;
                return map(rs);
            }
        }
    }

    @Override
    public List<Programme> getByEventId(int eventId) throws SQLException {

        String sql = "SELECT * FROM programme WHERE event_id = ? ORDER BY date_debut ASC";
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

    private Programme map(ResultSet rs) throws SQLException {

        Programme p = new Programme();

        p.setIdProg(rs.getInt("id_prog"));
        p.setEventId(rs.getInt("event_id"));
        p.setTitre(rs.getString("titre"));

        Timestamp td = rs.getTimestamp("date_debut");
        Timestamp tf = rs.getTimestamp("date_fin");

        p.setDebut(td != null ? td.toLocalDateTime() : null);
        p.setFin(tf != null ? tf.toLocalDateTime() : null);

        return p;
    }
}