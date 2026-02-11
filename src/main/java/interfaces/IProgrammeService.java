package interfaces;

import entities.Programme;

import java.sql.SQLException;
import java.util.List;

public interface IProgrammeService extends IService<Programme> {
    List<Programme> getByEventId(int eventId) throws SQLException;
}
