package phase3;

/**
 * ──────────────────────────────────────────────────────────────────
 * OBSERVER PATTERN  –  BookingObserver  (the Observer interface)
 * ──────────────────────────────────────────────────────────────────
 *
 * WHAT:
 *   This interface is implemented by any class that wants to be
 *   notified when a room booking event occurs.
 *
 * WHY (formal definition match):
 *   The Observer pattern defines a one-to-many dependency so that
 *   when the Subject (BookingEventPublisher) changes state, all
 *   registered Observers are notified automatically (GoF).
 *
 *   Role in this pattern:
 *       Observer  → BookingObserver (this interface)
 *       Subject   → BookingEventPublisher
 *       Concrete observers → MainDashboard (refresh room grid),
 *                           BookingServer  (broadcast over network)
 *
 * BEFORE:
 *   ConfirmationFrame directly called:
 *       BookingServer.broadcast("ROOM_BOOKED:…");
 *       // and MainDashboard re-loaded itself only via a socket message
 *   This was tight coupling: ConfirmationFrame knew about BookingServer.
 *
 * AFTER:
 *   ConfirmationFrame fires an event on BookingEventPublisher.
 *   BookingEventPublisher notifies all registered BookingObservers.
 *   MainDashboard and the network broadcast are separate observers.
 */
public interface BookingObserver {

    /**
     * Called by the Subject when a room is successfully booked.
     *
     * @param result the completed booking result (room name, booking id, etc.)
     * @param room   the room that was booked
     */
    void onBookingConfirmed(BookingService.BookingResult result, Room room);
}
