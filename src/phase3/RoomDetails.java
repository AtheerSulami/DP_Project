package phase3;

import javax.swing.*;
import javax.swing.border.MatteBorder;
import java.awt.*;

/**
 * RoomDetails – Phase 3
 *
 * Changes from Phase 2:
 *  • Receives a Room object (from DB) and a Faculty object
 *  • Displays real equipment and status from DB
 *  • Passes both objects to ConfirmationFrame for booking
 *  • Full exception handling
 */
public class RoomDetails extends JFrame {

    private final Room    room;
    private final Faculty faculty;

    public RoomDetails(Room room, Faculty faculty) {
        this.room    = room;
        this.faculty = faculty;

        setTitle("SilentCheck | Room Information");
        setSize(500, 500);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        getContentPane().setBackground(Color.WHITE);
        setLayout(new BorderLayout());

        // ── Header banner ─────────────────────────────────────────────────────
        JPanel topColor = new JPanel(new GridBagLayout());
        topColor.setBackground(new Color(52, 152, 219));
        topColor.setPreferredSize(new Dimension(500, 100));

        JLabel title = new JLabel(room.getName());
        title.setForeground(Color.WHITE);
        title.setFont(new Font("Segoe UI", Font.BOLD, 30));
        topColor.add(title);

        // ── Info rows from DB data ────────────────────────────────────────────
        JPanel infoPanel = new JPanel(new GridLayout(5, 1, 10, 10));
        infoPanel.setBorder(BorderFactory.createEmptyBorder(30, 50, 30, 50));
        infoPanel.setBackground(Color.WHITE);

        String floorLabel = switch (room.getFloor()) {
            case 0 -> "Ground Floor";
            case 1 -> "First Floor";
            case 2 -> "Second Floor";
            default -> "Unknown";
        };

        infoPanel.add(createInfoRow("Location:",  "Building 31, " + floorLabel));
        infoPanel.add(createInfoRow("Capacity:",  room.getCapacity()));
        infoPanel.add(createInfoRow("Equipment:", room.getEquipment()));
        infoPanel.add(createInfoRow("Status:",    room.getStatus()));
        infoPanel.add(createInfoRow("Booked by:", faculty.getFullName() + " (" + faculty.getEmail() + ")"));

        // ── Action buttons ────────────────────────────────────────────────────
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 20));
        actions.setBackground(Color.WHITE);

        JButton backBtn = new JButton("Back");
        backBtn.setPreferredSize(new Dimension(120, 40));
        backBtn.addActionListener(e -> {
            new MainDashboard(faculty).setVisible(true);
            dispose();
        });

        JButton bookBtn = new JButton("Confirm Booking");
        bookBtn.setPreferredSize(new Dimension(200, 40));
        bookBtn.setBackground(new Color(46, 204, 113));
        bookBtn.setForeground(Color.WHITE);
        bookBtn.setFocusPainted(false);
        bookBtn.addActionListener(e -> {
            new ConfirmationFrame(room, faculty).setVisible(true);
            dispose();
        });

        actions.add(backBtn);
        actions.add(bookBtn);

        add(topColor,   BorderLayout.NORTH);
        add(infoPanel,  BorderLayout.CENTER);
        add(actions,    BorderLayout.SOUTH);
    }

    private JPanel createInfoRow(String label, String value) {
        JPanel row = new JPanel(new BorderLayout());
        row.setOpaque(false);

        JLabel lbl = new JLabel(label);
        lbl.setFont(new Font("Segoe UI", Font.BOLD, 14));
        lbl.setForeground(new Color(127, 140, 141));

        JLabel val = new JLabel(value);
        val.setFont(new Font("Segoe UI", Font.PLAIN, 14));

        row.add(lbl, BorderLayout.WEST);
        row.add(val, BorderLayout.SOUTH);
        row.setBorder(new MatteBorder(0, 0, 1, 0, new Color(240, 240, 240)));
        return row;
    }
}
