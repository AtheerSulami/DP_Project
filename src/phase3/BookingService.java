package phase3;

import java.sql.SQLException;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * BookingService – Concurrent Programming layer for SilentCheck.
 *
 * Uses a single-thread ExecutorService to serialise bookings so that
 * two faculty members cannot book the same room simultaneously.
 * Also uses AtomicBoolean to guard the "booking in progress" state
 * shown on the UI while the background task runs.
 *
 * Covers: Concurrent Programming requirement (ExecutorService, Future,
 *         AtomicBoolean, thread-safe design).
 */
public class BookingService {

    // Single-thread executor → only one booking processed at a time,
    // preventing double-booking race conditions.
    private static final ExecutorService executor =
            Executors.newSingleThreadExecutor(r -> {
                Thread t = new Thread(r, "booking-worker");
                t.setDaemon(true);   // exits when JVM shuts down
                return t;
            });

    private static final AtomicBoolean bookingInProgress = new AtomicBoolean(false);

    private BookingService() {}

    /**
     * Result object returned by the booking Future.
     */
    public record BookingResult(boolean success, int bookingId,
                                String permitPath, String errorMessage) {}

    /**
     * Submits a booking request asynchronously.
     *
     * The caller receives a Future<BookingResult> immediately and can poll /
     * block until the booking worker completes.
     *
     * Steps performed on the worker thread:
     *   1. Re-check room availability (prevents TOCTOU race)
     *   2. Insert booking record into DB
     *   3. Mark room as Occupied in DB
     *   4. Generate permit .txt file via PermitWriter
     *   5. Update booking record with permit path
     *
     * @param faculty   Logged-in faculty member
     * @param room      Room to be booked
     * @return          Future that resolves to a BookingResult
     */
    public static Future<BookingResult> submitBooking(Faculty faculty, Room room) {
        return executor.submit(() -> {
            // Guard: refuse if another booking is already in flight
            if (!bookingInProgress.compareAndSet(false, true)) {
                return new BookingResult(false, -1, null,
                        "Another booking is being processed. Please try again.");
            }

            try {
                DatabaseManager db = DatabaseManager.getInstance();

                // 1. Re-check availability inside the worker thread (thread-safe check)
                Room fresh = db.getRoomByName(room.getName());
                if (fresh == null || !fresh.isAvailable()) {
                    return new BookingResult(false, -1, null,
                            "Room '" + room.getName() + "' is no longer available.");
                }

                // 2. Insert booking (permit_file set after file is written)
                int bookingId = db.createBooking(faculty.getId(), fresh.getId(), "pending");

                // 3. Mark room as Occupied
                db.updateRoomStatus(fresh.getId(), "Occupied");

                // 4. Write permit file
                String permitPath = PermitWriter.generatePermit(bookingId, faculty, fresh);

                // 5. Update booking record with the real permit path
                String updateSql =
                        "UPDATE bookings SET permit_file=? WHERE id=?";
                try (var ps = db.getConnection().prepareStatement(updateSql)) {
                    ps.setString(1, permitPath);
                    ps.setInt   (2, bookingId);
                    ps.executeUpdate();
                }

                System.out.printf("[Booking] #%d confirmed for %s in %s%n",
                        bookingId, faculty.getEmail(), fresh.getName());

                return new BookingResult(true, bookingId, permitPath, null);

            } catch (Exception e) {
                System.err.println("[Booking] Error: " + e.getMessage());
                return new BookingResult(false, -1, null,
                        "Booking failed: " + e.getMessage());
            } finally {
                bookingInProgress.set(false);
            }
        });
    }

    /** Cleanly shuts down the executor (call on application exit). */
    public static void shutdown() {
        executor.shutdownNow();
    }
}
