package phase3;

import java.io.*;
import java.net.*;
import java.util.concurrent.*;

/**
 * BookingServer – Network Programming layer for SilentCheck.
 *
 * A lightweight TCP server that:
 *   • Listens on port 9090 for booking notifications
 *   • Handles multiple simultaneous client connections via a thread pool
 *   • Broadcasts room-status updates to all connected clients
 *
 * This simulates a real-time room-availability broadcast in a multi-user
 * faculty environment (e.g., multiple computers in a department).
 *
 * Covers: Network Programming requirement (ServerSocket, Socket,
 *         multi-client handling, ObjectOutputStream/BufferedReader).
 *
 * ── How to use ──────────────────────────────────────────────────────────
 *  Start server:  BookingServer.start();
 *  Notify rooms:  BookingServer.broadcast("ROOM_BOOKED:Lab G-01:Occupied");
 *  Stop server:   BookingServer.stop();
 * ────────────────────────────────────────────────────────────────────────
 */
public class BookingServer {

    public static final int PORT = 9090;

    // Thread-safe set of all active client writers
    private static final CopyOnWriteArrayList<PrintWriter> clients =
            new CopyOnWriteArrayList<>();

    private static ServerSocket serverSocket;
    private static volatile boolean running = false;
    private static final ExecutorService pool =
            Executors.newCachedThreadPool(r -> {
                Thread t = new Thread(r, "server-client-handler");
                t.setDaemon(true);
                return t;
            });

    private BookingServer() {}

    // ── Server lifecycle ──────────────────────────────────────────────────────

    /**
     * Starts the server on a daemon thread so it doesn't block the GUI.
     */
    public static void start() {
        if (running) return;
        Thread serverThread = new Thread(() -> {
            try {
                serverSocket = new ServerSocket(PORT);
                running = true;
                System.out.println("[Server] BookingServer started on port " + PORT);

                while (running) {
                    try {
                        Socket clientSocket = serverSocket.accept();
                        pool.submit(() -> handleClient(clientSocket));
                    } catch (SocketException e) {
                        if (running) System.err.println("[Server] Accept error: " + e.getMessage());
                    }
                }
            } catch (IOException e) {
                System.err.println("[Server] Cannot start on port " + PORT + ": " + e.getMessage());
            }
        }, "booking-server");
        serverThread.setDaemon(true);
        serverThread.start();
    }

    public static void stop() {
        running = false;
        try { if (serverSocket != null) serverSocket.close(); }
        catch (IOException ignored) {}
        pool.shutdownNow();
        System.out.println("[Server] BookingServer stopped.");
    }

    /**
     * Sends a message to every connected client.
     * Message format examples:
     *   "ROOM_BOOKED:Lab G-01:Occupied"
     *   "PING"
     */
    public static void broadcast(String message) {
        System.out.println("[Server] Broadcasting → " + message);
        for (PrintWriter pw : clients) {
            pw.println(message);
            pw.flush();
        }
    }

    // ── Client handler ────────────────────────────────────────────────────────

    private static void handleClient(Socket socket) {
        String clientInfo = socket.getInetAddress() + ":" + socket.getPort();
        System.out.println("[Server] Client connected: " + clientInfo);

        try (
            PrintWriter  out = new PrintWriter(socket.getOutputStream(), true);
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))
        ) {
            clients.add(out);
            out.println("WELCOME:SilentCheck Server Ready");

            String line;
            while ((line = in.readLine()) != null) {
                System.out.println("[Server] From " + clientInfo + ": " + line);
                // Echo back an acknowledgement
                out.println("ACK:" + line);
                out.flush();
            }
        } catch (IOException e) {
            System.err.println("[Server] Client disconnected: " + clientInfo);
        } finally {
            // Remove the writer when client leaves — iterate to find and remove
            clients.removeIf(pw -> {
                try { pw.checkError(); return pw.checkError(); }
                catch (Exception ex) { return true; }
            });
        }
    }
}
