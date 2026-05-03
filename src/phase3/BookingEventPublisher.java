package phase3;

import java.util.ArrayList;
import java.util.List;

/**
 * ──────────────────────────────────────────────────────────────────
 * OBSERVER PATTERN  –  BookingEventPublisher  (the Subject)
 * ──────────────────────────────────────────────────────────────────
 *
 * Maintains the list of registered observers and fires notifications
 * when a booking is confirmed.
 *
 * Formal GoF roles:
 *   Subject / Observable → BookingEventPublisher  (this class)
 *   Observer interface   → BookingObserver
 *   Concrete Observers   → MainDashboard, NetworkBroadcastObserver
 *
 * The class is a singleton so that ConfirmationFrame and MainDashboard
 * share the same publisher instance.
 */
public class BookingEventPublisher {

    // ── Singleton ─────────────────────────────────────────────────
    private static BookingEventPublisher instance;

    public static synchronized BookingEventPublisher getInstance() {
        if (instance == null) instance = new BookingEventPublisher();
        return instance;
    }

    private BookingEventPublisher() {}

    // ── Observer list (the "subscriber registry") ─────────────────
    private final List<BookingObserver> observers = new ArrayList<>();

    /** Register an observer (subscribe). */
    public void addObserver(BookingObserver observer) {
        if (!observers.contains(observer)) {
            observers.add(observer);
        }
    }

    /** Unregister an observer (unsubscribe). */
    public void removeObserver(BookingObserver observer) {
        observers.remove(observer);
    }

    /**
     * Notify all registered observers of a confirmed booking.
     * Called by ConfirmationFrame after BookingService succeeds.
     */
    public void notifyBookingConfirmed(BookingService.BookingResult result, Room room) {
        System.out.println("[Publisher] Notifying " + observers.size()
                           + " observer(s) of booking #" + result.bookingId());
        for (BookingObserver obs : observers) {
            obs.onBookingConfirmed(result, room);
        }
    }
}
