package phase3;

import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * ──────────────────────────────────────────────────────────────────
 * FACTORY PATTERN  –  FacultyFactory
 * ──────────────────────────────────────────────────────────────────
 *
 * Centralises Faculty object creation, mirroring RoomFactory.
 * DatabaseManager.authenticate() previously built Faculty inline;
 * now it delegates to FacultyFactory.fromResultSet(rs).
 *
 * Formal match:
 *   Creator  → FacultyFactory
 *   Product  → Faculty
 *   Factory Method → fromResultSet / create
 */
public class FacultyFactory {

    private FacultyFactory() {}

    /**
     * Creates a Faculty from the current ResultSet row.
     */
    public static Faculty fromResultSet(ResultSet rs) throws SQLException {
        return new Faculty(
            rs.getInt("id"),
            rs.getString("email"),
            rs.getString("full_name")
        );
    }

    /**
     * Creates a Faculty from explicit values.
     */
    public static Faculty create(int id, String email, String fullName) {
        return new Faculty(id, email, fullName);
    }
}
