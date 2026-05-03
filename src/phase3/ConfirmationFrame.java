package phase3;

import javax.swing.*;
import java.awt.*;

/**
 * ConfirmationFrame – Booking Confirmation screen.
 *
 * ── Pattern changes (Phase 3 refactor) ───────────────────────────
 *
 * FACADE PATTERN:
 *   BEFORE: This class called three separate subsystems:
 *     (1) BookingService.submitBooking(faculty, room)  [concurrent layer]
 *     (2) PermitWriter.readPermit(path)               [IO layer]
 *     (3) BookingServer.broadcast("ROOM_BOOKED:...")  [network layer]
 *   AFTER: All three are replaced by ONE call:
 *     SilentCheckFacade.getInstance().processBooking(faculty, room)
 *     SilentCheckFacade.getInstance().readPermit(path)
 *   ConfirmationFrame no longer imports BookingService, PermitWriter,
 *   or BookingServer at all.
 *
 * OBSERVER PATTERN:
 *   BEFORE: ConfirmationFrame called BookingServer.broadcast() directly.
 *   AFTER:  SilentCheckFacade.processBooking() calls
 *           BookingEventPublisher.notifyBookingConfirmed() internally,
 *           which triggers NetworkBroadcastObserver (broadcast) AND
 *           MainDashboard.onBookingConfirmed() (UI refresh).
 *   ConfirmationFrame is completely decoupled from both side-effects.
 * ─────────────────────────────────────────────────────────────────
 */
public class ConfirmationFrame extends JFrame {

    private final Room    room;
    private final Faculty faculty;

    public ConfirmationFrame(Room room, Faculty faculty) {
        this.room    = room;
        this.faculty = faculty;

        setTitle("SilentCheck | Booking Confirmation");
        setSize(480, 400);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBackground(Color.WHITE);
        panel.setBorder(BorderFactory.createEmptyBorder(30, 40, 30, 40));

        JLabel statusLbl = new JLabel(
            "<html><center>\u23F3 Processing your booking...<br><small>Please wait</small></center></html>",
            SwingConstants.CENTER);
        statusLbl.setFont(new Font("Segoe UI", Font.PLAIN, 18));
        statusLbl.setForeground(new Color(52, 152, 219));

        JLabel permitLbl = new JLabel("", SwingConstants.CENTER);
        permitLbl.setFont(new Font("Segoe UI", Font.ITALIC, 11));
        permitLbl.setForeground(Color.GRAY);

        JProgressBar progress = new JProgressBar();
        progress.setIndeterminate(true);

        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 0));
        btnPanel.setBackground(Color.WHITE);
        btnPanel.setVisible(false);

        JButton viewPermitBtn = new JButton("\uD83D\uDCC4 View Permit");
        viewPermitBtn.setBackground(new Color(52, 152, 219));
        viewPermitBtn.setForeground(Color.WHITE);
        viewPermitBtn.setFocusPainted(false);

        JButton dashBtn = new JButton("Return to Dashboard");
        dashBtn.setBackground(new Color(44, 62, 80));
        dashBtn.setForeground(Color.WHITE);
        dashBtn.setFocusPainted(false);
        dashBtn.addActionListener(e -> {
            new MainDashboard(faculty).setVisible(true);
            dispose();
        });

        btnPanel.add(viewPermitBtn);
        btnPanel.add(dashBtn);

        panel.add(statusLbl, BorderLayout.CENTER);
        panel.add(progress,  BorderLayout.NORTH);

        JPanel south = new JPanel(new BorderLayout(5, 5));
        south.setBackground(Color.WHITE);
        south.add(permitLbl, BorderLayout.NORTH);
        south.add(btnPanel,  BorderLayout.SOUTH);
        panel.add(south, BorderLayout.SOUTH);

        add(panel);

        // ── Submit booking via Facade ─────────────────────────────────────────
        SwingWorker<BookingService.BookingResult, Void> worker = new SwingWorker<>() {
            @Override
            protected BookingService.BookingResult doInBackground() throws Exception {
                // FACADE PATTERN: was BookingService.submitBooking() + future.get()
                //                 + BookingServer.broadcast() called separately.
                // Now a single facade call handles all of that, including
                // firing the Observer notifications.
                return SilentCheckFacade.getInstance().processBooking(faculty, room);
            }

            @Override
            protected void done() {
                progress.setIndeterminate(false);
                progress.setValue(100);

                try {
                    BookingService.BookingResult result = get();

                    if (result.success()) {
                        statusLbl.setText(
                            "<html><center><b style='color:#27ae60'>\u2705 Booking Confirmed!</b>" +
                            "<br>Booking #" + result.bookingId() +
                            "<br><br>" + room.getName() + " is now reserved for<br>" +
                            faculty.getFullName() + "</center></html>");

                        permitLbl.setText("Permit saved: " + result.permitPath());

                        // FACADE PATTERN: was PermitWriter.readPermit(path)
                        viewPermitBtn.addActionListener(e -> showPermit(result.permitPath()));
                        btnPanel.setVisible(true);

                        // NOTE: BookingServer.broadcast() is NOT called here anymore.
                        // It is handled by NetworkBroadcastObserver, which is registered
                        // inside SilentCheckFacade and triggered automatically by
                        // BookingEventPublisher.notifyBookingConfirmed().

                    } else {
                        statusLbl.setText(
                            "<html><center><b style='color:#e74c3c'>\u274C Booking Failed</b>" +
                            "<br><small>" + result.errorMessage() + "</small></center></html>");
                        btnPanel.setVisible(true);
                        viewPermitBtn.setVisible(false);
                    }

                } catch (Exception ex) {
                    statusLbl.setText(
                        "<html><center><b style='color:#e74c3c'>\u274C Unexpected Error</b>" +
                        "<br><small>" + ex.getMessage() + "</small></center></html>");
                    btnPanel.setVisible(true);
                    viewPermitBtn.setVisible(false);
                }

                panel.revalidate();
                panel.repaint();
            }
        };
        worker.execute();
    }

    private void showPermit(String permitPath) {
        try {
            // FACADE PATTERN: was PermitWriter.readPermit(permitPath)
            String content = SilentCheckFacade.getInstance().readPermit(permitPath);
            JTextArea area = new JTextArea(content);
            area.setFont(new Font("Monospaced", Font.PLAIN, 13));
            area.setEditable(false);
            JScrollPane sp = new JScrollPane(area);
            sp.setPreferredSize(new Dimension(560, 420));
            JOptionPane.showMessageDialog(this, sp,
                "Booking Permit", JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this,
                "Could not read permit file:\n" + e.getMessage(),
                "IO Error", JOptionPane.ERROR_MESSAGE);
        }
    }
}
