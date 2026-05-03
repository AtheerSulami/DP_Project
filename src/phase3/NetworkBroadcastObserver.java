package phase3;

/**
 * ──────────────────────────────────────────────────────────────────
 * OBSERVER PATTERN  –  NetworkBroadcastObserver  (Concrete Observer)
 * ──────────────────────────────────────────────────────────────────
 *
 * Handles the network-broadcast side-effect of a booking.
 * Previously this code lived inline inside ConfirmationFrame:
 *
 *   BEFORE (ConfirmationFrame.java, line ~100):
 *       BookingServer.broadcast("ROOM_BOOKED:" + room.getName() + ":Occupied");
 *
 *   AFTER:
 *       This observer is registered on BookingEventPublisher.
 *       ConfirmationFrame no longer mentions BookingServer at all.
 *
 * Formal role:  Concrete Observer implementing BookingObserver.
 */
public class NetworkBroadcastObserver implements BookingObserver {

    @Override
    public void onBookingConfirmed(BookingService.BookingResult result, Room room) {
        // Broadcast the status change to all connected BookingClient sockets
        BookingServer.broadcast("ROOM_BOOKED:" + room.getName() + ":Occupied");
        System.out.println("[NetworkObserver] Broadcast sent for room: " + room.getName());
    }
}
