package phase3;

import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * ──────────────────────────────────────────────────────────────────
 * FACTORY PATTERN  –  RoomFactory
 * ──────────────────────────────────────────────────────────────────
 *
 * WHAT:
 *   Centralises all Room object creation.  Every caller that used to
 *   write "new Room(rs.getInt("id"), rs.getString("name"), …)" is
 *   replaced by a single call to RoomFactory.fromResultSet(rs) or
 *   RoomFactory.create(…).
 *
 * WHY (formal definition match):
 *   The Factory Method pattern "defines an interface for creating an
 *   object but lets the calling code avoid specifying the exact class
 *   to instantiate" (GoF).  Here, RoomFactory is the Creator; its
 *   factory methods (fromResultSet / create) are the Factory Methods;
 *   Room is the Product.  Callers depend only on the factory, not on
 *   the Room constructor signature.
 *
 * BEFORE → AFTER:
 *   BEFORE (in MainDashboard, DatabaseManager, BookingService):
 *       new Room(rs.getInt("id"), rs.getString("name"),
 *                rs.getInt("floor"), rs.getString("capacity"),
 *                rs.getString("equipment"), rs.getString("status"))
 *   AFTER:
 *       RoomFactory.fromResultSet(rs)
 *
 * BENEFIT:
 *   If the Room constructor signature ever changes (e.g. adding a
 *   "building" field), only this one file needs updating.
 */
public class RoomFactory {

    private RoomFactory() {} // static-utility class – no instances

    /**
     * Creates a Room from the *current* row of an open ResultSet.
     * Does NOT advance the ResultSet cursor.
     */
    public static Room fromResultSet(ResultSet rs) throws SQLException {
        return new Room(
            rs.getInt("id"),
            rs.getString("name"),
            rs.getInt("floor"),
            rs.getString("capacity"),
            rs.getString("equipment"),
            rs.getString("status")
        );
    }

    /**
     * Creates a Room from explicit field values.
     * Used in unit tests or wherever a ResultSet is not available.
     */
    public static Room create(int id, String name, int floor,
                              String capacity, String equipment,
                              String status) {
        return new Room(id, name, floor, capacity, equipment, status);
    }
}
