package phase3;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Future;

/**
 * ──────────────────────────────────────────────────────────────────
 * FACADE PATTERN  –  SilentCheckFacade
 * ──────────────────────────────────────────────────────────────────
 *
 * WHAT:
 *   Provides a single, simplified interface to three complex subsystems:
 *     1. DatabaseManager  (SQL, connections, ResultSets)
 *     2. BookingService   (Executor, Future, AtomicBoolean)
 *     3. PermitWriter     (File IO, paths)
 *
 *   UI classes (LoginFrame, MainDashboard, ConfirmationFrame) used to
 *   import and call each subsystem directly.  Now they call only this
 *   Facade, keeping the UI completely decoupled from the subsystem
 *   implementation details.
 *
 * WHY (formal definition match):
 *   GoF: "Provide a unified interface to a set of interfaces in a
 *   subsystem. Facade defines a higher-level interface that makes the
 *   subsystem easier to use."
 *
 *   Subsystems (complex)   → DatabaseManager, BookingService, PermitWriter
 *   Facade (simplified)    → SilentCheckFacade  (this class)
 *   Clients (UI screens)   → LoginFrame, MainDashboard, ConfirmationFrame
 *
 * BEFORE → AFTER:
 *   BEFORE (LoginFrame):
 *       DatabaseManager db = DatabaseManager.getInstance();
 *       Faculty f = db.authenticate(email, password);
 *
 *   AFTER (LoginFrame):
 *       Faculty f = SilentCheckFacade.getInstance().login(email, password);
 *
 *   BEFORE (ConfirmationFrame):
 *       Future<BookingService.BookingResult> future =
 *           BookingService.submitBooking(faculty, room);
 *       BookingResult result = future.get();
 *       BookingServer.broadcast("ROOM_BOOKED:…");
 *
 *   AFTER (ConfirmationFrame):
 *       BookingService.BookingResult result =
 *           SilentCheckFacade.getInstance().processBooking(faculty, room);
 *       // broadcast is handled internally by the Observer chain
 *
 * BENEFIT:
 *   The UI layer never imports DatabaseManager, BookingService, or
 *   PermitWriter again.  Only the Facade knows these subsystems exist.
 */
public class SilentCheckFacade {

    // ── Singleton ─────────────────────────────────────────────────
    private static SilentCheckFacade instance;

    public static synchronized SilentCheckFacade getInstance() {
        if (instance == null) instance = new SilentCheckFacade();
        return instance;
    }

    private SilentCheckFacade() {
        // Register the network-broadcast observer once at construction time
        BookingEventPublisher.getInstance()
                             .addObserver(new NetworkBroadcastObserver());
    }

    // ═══════════════════════════════════════════════════════════════
    //  AUTH SUBSYSTEM  (wraps DatabaseManager)
    // ═══════════════════════════════════════════════════════════════

    /**
     * Authenticates a faculty member.
     * Returns the Faculty record, or null if credentials are wrong.
     *
     * BEFORE: LoginFrame called DatabaseManager.getInstance().authenticate(…)
     * AFTER:  LoginFrame calls this method.
     */
    public Faculty login(String email, String password) throws SQLException {
        return DatabaseManager.getInstance().authenticate(email, password);
    }

    /**
     * Registers a new faculty member account.
     *
     * BEFORE: RegisterDialog called DatabaseManager.getInstance().registerFaculty(…)
     * AFTER:  RegisterDialog calls this method.
     */
    public void register(String email, String password, String fullName)
            throws SQLException {
        DatabaseManager.getInstance().registerFaculty(email, password, fullName);
    }

    // ═══════════════════════════════════════════════════════════════
    //  ROOM SUBSYSTEM  (wraps DatabaseManager + RoomFactory)
    // ═══════════════════════════════════════════════════════════════

    /**
     * Returns all rooms on a given floor.
     *
     * BEFORE: MainDashboard opened a ResultSet and manually built Room objects.
     * AFTER:  MainDashboard calls getRoomsByFloor() and receives a ready List<Room>.
     */
    public List<Room> getRoomsByFloor(int floor) throws SQLException {
        List<Room> rooms = new ArrayList<>();
        try (ResultSet rs = DatabaseManager.getInstance().getRoomsByFloor(floor)) {
            while (rs.next()) {
                rooms.add(RoomFactory.fromResultSet(rs));   // Factory Pattern in use
            }
        }
        return rooms;
    }

    /**
     * Returns all past bookings for a faculty member as display rows.
     * Each Object[] is: { bookingId, roomName, bookedAt, permitFile }
     *
     * BEFORE: MainDashboard iterated over a raw ResultSet inline.
     * AFTER:  MainDashboard receives a typed list of arrays.
     */
    public List<Object[]> getBookingHistory(int facultyId) throws SQLException {
        List<Object[]> rows = new ArrayList<>();
        try (ResultSet rs = DatabaseManager.getInstance()
                                           .getBookingsByFaculty(facultyId)) {
            while (rs.next()) {
                rows.add(new Object[]{
                    rs.getInt("id"),
                    rs.getString("room"),
                    rs.getTimestamp("booked_at"),
                    rs.getString("permit_file")
                });
            }
        }
        return rows;
    }

    // ═══════════════════════════════════════════════════════════════
    //  BOOKING SUBSYSTEM  (wraps BookingService + Observer publish)
    // ═══════════════════════════════════════════════════════════════

    /**
     * Processes a booking end-to-end:
     *   1. Calls BookingService (concurrent worker thread)
     *   2. On success, notifies all registered observers via BookingEventPublisher
     *      (which triggers the NetworkBroadcastObserver to send the socket message)
     *
     * BEFORE: ConfirmationFrame called BookingService AND BookingServer directly.
     * AFTER:  ConfirmationFrame calls only this one method.
     *
     * @return the BookingResult (success/failure + bookingId + permitPath)
     */
    public BookingService.BookingResult processBooking(Faculty faculty, Room room)
            throws Exception {

        Future<BookingService.BookingResult> future =
                BookingService.submitBooking(faculty, room);

        BookingService.BookingResult result = future.get(); // blocks SwingWorker thread

        if (result.success()) {
            // Notify all observers (NetworkBroadcastObserver → socket broadcast,
            // and any other observers, e.g. MainDashboard's UI refresh observer)
            BookingEventPublisher.getInstance()
                                 .notifyBookingConfirmed(result, room);
        }

        return result;
    }

    // ═══════════════════════════════════════════════════════════════
    //  PERMIT SUBSYSTEM  (wraps PermitWriter)
    // ═══════════════════════════════════════════════════════════════

    /**
     * Reads and returns the content of a permit file.
     *
     * BEFORE: ConfirmationFrame called PermitWriter.readPermit(path) directly.
     * AFTER:  ConfirmationFrame calls this method.
     */
    public String readPermit(String permitPath) throws Exception {
        return PermitWriter.readPermit(permitPath);
    }

    // ═══════════════════════════════════════════════════════════════
    //  LIFECYCLE  (wraps server + service + DB shutdown)
    // ═══════════════════════════════════════════════════════════════

    /**
     * Starts the BookingServer (call after successful login).
     *
     * BEFORE: LoginFrame called BookingServer.start() directly.
     * AFTER:  LoginFrame calls this method.
     */
    public void startServer() {
        try {
            BookingServer.start();
        } catch (Exception ex) {
            System.err.println("[Facade] Could not start server: " + ex.getMessage());
        }
    }

    /**
     * Cleanly shuts down all subsystems.
     *
     * BEFORE: Main / logout button called BookingServer.stop(),
     *         BookingService.shutdown(), DatabaseManager.close() separately.
     * AFTER:  A single call to shutdown().
     */
    public void shutdown() {
        try {
            BookingServer.stop();
            BookingService.shutdown();
            DatabaseManager.getInstance().close();
        } catch (Exception e) {
            System.err.println("[Facade] Shutdown error: " + e.getMessage());
        }
    }
}
