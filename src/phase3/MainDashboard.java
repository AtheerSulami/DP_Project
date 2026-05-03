package phase3;

import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.sql.SQLException;

/**
 * MainDashboard – SilentCheck main screen.
 *
 * ── Pattern changes (Phase 3 refactor) ───────────────────────────
 *
 * FACADE PATTERN:
 *   BEFORE: Called DatabaseManager.getInstance().getRoomsByFloor(floor) with
 *           manual ResultSet iteration and inline Room construction.
 *           Called DatabaseManager.getInstance().getBookingsByFaculty(...) with
 *           manual ResultSet iteration.
 *   AFTER:  Calls SilentCheckFacade.getInstance().getRoomsByFloor(floor)
 *           → returns List<Room> directly (no SQL boilerplate in UI).
 *           Calls SilentCheckFacade.getInstance().getBookingHistory(id)
 *           → returns List<Object[]> directly.
 *
 * OBSERVER PATTERN (Concrete Observer):
 *   BEFORE: Dashboard refreshed only via a socket message routed through
 *           BookingClient → handleServerMessage → updateFloorView.
 *   AFTER:  MainDashboard also implements BookingObserver and registers
 *           itself on BookingEventPublisher.  When any booking is confirmed,
 *           onBookingConfirmed() triggers an immediate local UI refresh –
 *           no network round-trip needed for the same JVM instance.
 * ─────────────────────────────────────────────────────────────────
 */
public class MainDashboard extends JFrame implements BookingObserver {

    private final Faculty      faculty;
    private       JPanel       roomGrid;
    private       JLabel       titleLbl;
    private       BookingClient client;
    private       int          currentFloor = 0;

    public MainDashboard(Faculty faculty) {
        this.faculty = faculty;

        // OBSERVER PATTERN: register this dashboard as a booking observer
        BookingEventPublisher.getInstance().addObserver(this);

        setTitle("SilentCheck | Faculty of Computing & IT");
        setSize(1200, 800);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        getContentPane().setBackground(new Color(248, 249, 250));

        // ── Sidebar ───────────────────────────────────────────────────────────
        JPanel sidebar = new JPanel();
        sidebar.setLayout(new BoxLayout(sidebar, BoxLayout.Y_AXIS));
        sidebar.setPreferredSize(new Dimension(250, 800));
        sidebar.setBackground(new Color(33, 37, 41));
        sidebar.setBorder(BorderFactory.createEmptyBorder(30, 20, 30, 20));

        JLabel logo = new JLabel("SilentCheck");
        logo.setForeground(Color.WHITE);
        logo.setFont(new Font("Segoe UI", Font.BOLD, 26));
        logo.setAlignmentX(Component.CENTER_ALIGNMENT);
        sidebar.add(logo);

        JLabel userLbl = new JLabel("\uD83D\uDC64 " + faculty.getFullName());
        userLbl.setForeground(new Color(150, 200, 255));
        userLbl.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        userLbl.setAlignmentX(Component.CENTER_ALIGNMENT);
        sidebar.add(Box.createVerticalStrut(10));
        sidebar.add(userLbl);
        sidebar.add(Box.createVerticalStrut(40));

        String[] floors = {"Ground Floor", "First Floor", "Second Floor"};
        for (int i = 0; i < floors.length; i++) {
            final int floorIndex = i;
            JButton fBtn = new JButton(floors[i]);
            fBtn.setMaximumSize(new Dimension(210, 45));
            fBtn.setForeground(Color.WHITE);
            fBtn.setBackground(new Color(52, 58, 64));
            fBtn.setFocusPainted(false);
            fBtn.addActionListener(e -> updateFloorView(floorIndex));
            sidebar.add(fBtn);
            sidebar.add(Box.createVerticalStrut(12));
        }

        sidebar.add(Box.createVerticalGlue());

        JButton historyBtn = new JButton("\uD83D\uDCCB My Bookings");
        historyBtn.setMaximumSize(new Dimension(210, 40));
        historyBtn.setForeground(Color.WHITE);
        historyBtn.setBackground(new Color(52, 58, 64));
        historyBtn.setFocusPainted(false);
        historyBtn.addActionListener(e -> showBookingHistory());
        sidebar.add(historyBtn);
        sidebar.add(Box.createVerticalStrut(12));

        JButton logoutBtn = new JButton("Logout");
        logoutBtn.setMaximumSize(new Dimension(210, 40));
        logoutBtn.setForeground(Color.WHITE);
        logoutBtn.setBackground(new Color(180, 60, 60));
        logoutBtn.setFocusPainted(false);
        logoutBtn.addActionListener(e -> {
            // Unregister observer before navigating away
            BookingEventPublisher.getInstance().removeObserver(this);
            if (client != null) client.disconnect();
            BookingService.shutdown();
            new LoginFrame().setVisible(true);
            dispose();
        });
        sidebar.add(logoutBtn);

        // ── Main area ─────────────────────────────────────────────────────────
        JPanel mainArea = new JPanel(new BorderLayout());
        mainArea.setOpaque(false);

        titleLbl = new JLabel("Ground Floor - Computing Labs");
        titleLbl.setFont(new Font("Segoe UI", Font.BOLD, 24));
        titleLbl.setBorder(BorderFactory.createEmptyBorder(25, 30, 10, 30));
        mainArea.add(titleLbl, BorderLayout.NORTH);

        roomGrid = new JPanel(new GridLayout(0, 4, 25, 25));
        roomGrid.setBackground(new Color(248, 249, 250));
        roomGrid.setBorder(BorderFactory.createEmptyBorder(20, 30, 30, 30));

        JPanel scrollWrapper = new JPanel(new BorderLayout());
        scrollWrapper.setOpaque(false);
        scrollWrapper.add(roomGrid, BorderLayout.NORTH);

        JScrollPane scrollPane = new JScrollPane(scrollWrapper);
        scrollPane.setBorder(null);
        scrollPane.getViewport().setBackground(new Color(248, 249, 250));
        mainArea.add(scrollPane, BorderLayout.CENTER);

        add(sidebar, BorderLayout.WEST);
        add(mainArea, BorderLayout.CENTER);

        updateFloorView(0);
        connectToServer();

        addWindowListener(new java.awt.event.WindowAdapter() {
            @Override public void windowClosing(java.awt.event.WindowEvent e) {
                BookingEventPublisher.getInstance().removeObserver(MainDashboard.this);
                try {
                    if (client != null) client.disconnect();
                    DatabaseManager.getInstance().close();
                    BookingServer.stop();
                } catch (SQLException ex) {
                    Logger.getLogger(MainDashboard.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        });
    }

    // ── OBSERVER PATTERN: concrete observer method ────────────────────────────
    /**
     * Called by BookingEventPublisher when any booking is confirmed.
     * Refreshes the room grid immediately (same JVM, no network needed).
     */
    @Override
    public void onBookingConfirmed(BookingService.BookingResult result, Room room) {
        System.out.println("[Dashboard Observer] Booking #" + result.bookingId()
                           + " confirmed – refreshing floor view.");
        SwingUtilities.invokeLater(() -> updateFloorView(currentFloor));
    }

    // ── Connect to BookingServer for remote-client live updates ───────────────
    private void connectToServer() {
        client = new BookingClient();
        try {
            client.connect(msg -> SwingUtilities.invokeLater(() -> handleServerMessage(msg)));
        } catch (Exception e) {
            System.err.println("[Dashboard] Could not connect to server: " + e.getMessage());
        }
    }

    private void handleServerMessage(String msg) {
        if (msg.startsWith("ROOM_BOOKED:")) {
            String[] parts = msg.split(":", 3);
            if (parts.length == 3) {
                System.out.println("[Dashboard] Socket update: " + parts[1] + " -> " + parts[2]);
                updateFloorView(currentFloor);
            }
        }
    }

    // ── Load rooms via Facade ─────────────────────────────────────────────────
    /**
     * FACADE PATTERN:
     *   BEFORE: Opened a ResultSet and built Room objects inline.
     *   AFTER:  Calls SilentCheckFacade.getRoomsByFloor() → clean List<Room>.
     */
    private void updateFloorView(int floor) {
        currentFloor = floor;
        roomGrid.removeAll();

        String[] floorTitles = {
            "Ground Floor - Labs & Classrooms",
            "First Floor - Classrooms & Labs",
            "Second Floor - Offices & Admin Only"
        };
        titleLbl.setText(floorTitles[floor]);

        SwingWorker<List<Room>, Void> loader = new SwingWorker<>() {
            @Override
            protected List<Room> doInBackground() throws Exception {
                // FACADE: single call replaces ResultSet loop + Room construction
                return SilentCheckFacade.getInstance().getRoomsByFloor(floor);
            }

            @Override
            protected void done() {
                try {
                    List<Room> rooms = get();
                    roomGrid.removeAll();
                    for (Room r : rooms) roomGrid.add(createRoomCard(r));
                    roomGrid.revalidate();
                    roomGrid.repaint();
                } catch (Exception e) {
                    JOptionPane.showMessageDialog(MainDashboard.this,
                        "Failed to load rooms:\n" + e.getMessage(),
                        "Database Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        };
        loader.execute();
    }

    private JPanel createRoomCard(Room room) {
        boolean isAvail      = room.isAvailable();
        boolean isRestricted = room.isRestricted();

        JPanel card = new JPanel(new BorderLayout());
        card.setBackground(Color.WHITE);
        card.setBorder(new LineBorder(new Color(230, 230, 230), 1, true));
        card.setPreferredSize(new Dimension(200, 200));

        JPanel info = new JPanel(new GridLayout(3, 1));
        info.setOpaque(false);
        info.setBorder(BorderFactory.createEmptyBorder(20, 20, 15, 20));

        JLabel n = new JLabel(room.getName());
        n.setFont(new Font("Segoe UI", Font.BOLD, 17));
        JLabel c = new JLabel(room.getCapacity());
        c.setForeground(Color.GRAY);
        JLabel s = new JLabel(room.getStatus());
        s.setForeground(isRestricted ? Color.ORANGE :
                        isAvail      ? new Color(40, 167, 69) : Color.RED);

        info.add(n); info.add(c); info.add(s);

        JButton btn = new JButton(isRestricted ? "Restricted" : isAvail ? "Book Room" : "Occupied");
        btn.setEnabled(isAvail);
        btn.setBackground(isRestricted ? new Color(225,225,225) :
                          isAvail      ? new Color(0,123,255)   : new Color(200,200,200));
        btn.setForeground(isAvail && !isRestricted ? Color.WHITE : Color.GRAY);
        btn.setFocusPainted(false);
        btn.setFont(new Font("Segoe UI", Font.BOLD, 13));
        btn.addActionListener(e -> {
            new RoomDetails(room, faculty).setVisible(true);
            dispose();
        });

        card.add(info, BorderLayout.CENTER);
        card.add(btn,  BorderLayout.SOUTH);
        return card;
    }

    // ── Booking history via Facade ────────────────────────────────────────────
    /**
     * FACADE PATTERN:
     *   BEFORE: Opened a raw ResultSet from DatabaseManager inline.
     *   AFTER:  Calls SilentCheckFacade.getBookingHistory() → List<Object[]>.
     */
    private void showBookingHistory() {
        JDialog dialog = new JDialog(this, "My Booking History", true);
        dialog.setSize(600, 400);
        dialog.setLocationRelativeTo(this);

        String[] cols = {"#", "Room", "Booked At", "Permit File"};
        javax.swing.table.DefaultTableModel model =
                new javax.swing.table.DefaultTableModel(cols, 0);

        JTable table = new JTable(model);
        table.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        table.setRowHeight(28);

        try {
            // FACADE: replaces DatabaseManager.getInstance().getBookingsByFaculty(...)
            List<Object[]> rows = SilentCheckFacade.getInstance()
                                                   .getBookingHistory(faculty.getId());
            for (Object[] row : rows) model.addRow(row);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this,
                "Could not load history:\n" + e.getMessage(),
                "Error", JOptionPane.ERROR_MESSAGE);
        }

        dialog.add(new JScrollPane(table));
        dialog.setVisible(true);
    }
}
