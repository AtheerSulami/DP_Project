package phase3;

import java.io.*;
import java.net.*;
import java.util.function.Consumer;

/**
 * BookingClient – connects to BookingServer and listens for room-status
 * updates. When a broadcast arrives, it fires a callback on the calling
 * thread (the GUI uses this to refresh the room grid automatically).
 *
 * Covers: Network Programming requirement (Socket, BufferedReader,
 *         PrintWriter, client-side TCP).
 */
public class BookingClient {

    private Socket        socket;
    private PrintWriter   out;
    private BufferedReader in;
    private volatile boolean connected = false;

    /**
     * Connects to the local BookingServer.
     *
     * @param onMessage  callback invoked with each server message
     * @throws IOException if connection fails
     */
    public void connect(Consumer<String> onMessage) throws IOException {
        socket = new Socket("localhost", BookingServer.PORT);
        out    = new PrintWriter(socket.getOutputStream(), true);
        in     = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        connected = true;
        System.out.println("[Client] Connected to BookingServer.");

        // Background listener thread
        Thread listener = new Thread(() -> {
            try {
                String msg;
                while (connected && (msg = in.readLine()) != null) {
                    System.out.println("[Client] Received: " + msg);
                    onMessage.accept(msg);   // fire the GUI callback
                }
            } catch (IOException e) {
                if (connected) System.err.println("[Client] Connection lost: " + e.getMessage());
            }
        }, "client-listener");
        listener.setDaemon(true);
        listener.start();
    }

    /** Sends a message to the server (e.g., a booking notification). */
    public void send(String message) {
        if (connected && out != null) {
            out.println(message);
            out.flush();
        }
    }

    public void disconnect() {
        connected = false;
        try { if (socket != null) socket.close(); }
        catch (IOException ignored) {}
    }

    public boolean isConnected() { return connected; }
}
