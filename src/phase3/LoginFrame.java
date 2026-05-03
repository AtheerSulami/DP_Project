package phase3;

import javax.swing.*;
import java.awt.*;

/**
 * LoginFrame – SilentCheck Faculty Login
 *
 * FACADE PATTERN applied:
 *   BEFORE: Called DatabaseManager.getInstance().authenticate(email, password)
 *           Called BookingServer.start() directly
 *   AFTER:  Calls SilentCheckFacade.getInstance().login(email, password)
 *           Calls SilentCheckFacade.getInstance().startServer()
 *
 *   LoginFrame no longer imports DatabaseManager or BookingServer.
 *   It only knows about the Facade's simple interface.
 */
public class LoginFrame extends JFrame {

    public LoginFrame() {
        setTitle("SilentCheck | Faculty Login");
        setSize(400, 550);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBackground(Color.WHITE);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 30, 10, 30);
        gbc.fill   = GridBagConstraints.HORIZONTAL;

        JLabel title = new JLabel("SilentCheck", SwingConstants.CENTER);
        title.setFont(new Font("Segoe UI", Font.BOLD, 28));
        title.setForeground(new Color(44, 62, 80));
        gbc.gridy = 0; panel.add(title, gbc);

        JLabel sub = new JLabel("KAU Faculty Room Booking System", SwingConstants.CENTER);
        sub.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        sub.setForeground(Color.GRAY);
        gbc.gridy = 1; panel.add(sub, gbc);

        JTextField user = new JTextField();
        user.setBorder(BorderFactory.createTitledBorder("KAU Email"));
        gbc.gridy = 2; panel.add(user, gbc);

        JPasswordField pass = new JPasswordField();
        pass.setBorder(BorderFactory.createTitledBorder("Password"));
        gbc.gridy = 3; panel.add(pass, gbc);

        JLabel errorLbl = new JLabel("", SwingConstants.CENTER);
        errorLbl.setForeground(Color.RED);
        errorLbl.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        gbc.gridy = 4; panel.add(errorLbl, gbc);

        JButton loginBtn = new JButton("Login");
        loginBtn.setBackground(new Color(52, 152, 219));
        loginBtn.setForeground(Color.WHITE);
        loginBtn.setFocusPainted(false);
        loginBtn.setFont(new Font("Segoe UI", Font.BOLD, 14));
        gbc.gridy = 5; panel.add(loginBtn, gbc);

        JButton regBtn = new JButton("New here? Register");
        regBtn.setBorderPainted(false);
        regBtn.setContentAreaFilled(false);
        regBtn.setForeground(new Color(52, 152, 219));
        regBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        gbc.gridy = 6; panel.add(regBtn, gbc);

        JLabel hint = new JLabel("Demo: demo@kau.edu.sa / 1234", SwingConstants.CENTER);
        hint.setFont(new Font("Segoe UI", Font.ITALIC, 11));
        hint.setForeground(Color.LIGHT_GRAY);
        gbc.gridy = 7; panel.add(hint, gbc);

        add(panel);

        // ── Login action via Facade ───────────────────────────────────────────
        loginBtn.addActionListener(e -> {
            String email    = user.getText().trim();
            String password = new String(pass.getPassword());

            if (email.isEmpty() || password.isEmpty()) {
                errorLbl.setText("Please enter both email and password.");
                return;
            }

            loginBtn.setEnabled(false);
            loginBtn.setText("Authenticating...");

            // Run DB auth off the EDT
            SwingWorker<Faculty, Void> worker = new SwingWorker<>() {
                @Override
                protected Faculty doInBackground() throws Exception {
                    // FACADE PATTERN: was DatabaseManager.getInstance().authenticate(...)
                    return SilentCheckFacade.getInstance().login(email, password);
                }

                @Override
                protected void done() {
                    try {
                        Faculty faculty = get();
                        if (faculty != null) {
                            // FACADE PATTERN: was BookingServer.start() directly
                            SilentCheckFacade.getInstance().startServer();
                            new MainDashboard(faculty).setVisible(true);
                            dispose();
                        } else {
                            errorLbl.setText("Invalid email or password. Try again.");
                            loginBtn.setEnabled(true);
                            loginBtn.setText("Login");
                        }
                    } catch (Exception ex) {
                        errorLbl.setText("Database error. Check connection.");
                        loginBtn.setEnabled(true);
                        loginBtn.setText("Login");
                        JOptionPane.showMessageDialog(LoginFrame.this,
                            "Could not connect to database:\n" + ex.getMessage(),
                            "Connection Error", JOptionPane.ERROR_MESSAGE);
                    }
                }
            };
            worker.execute();
        });

        regBtn.addActionListener(e -> new RegisterDialog(this).setVisible(true));
    }
}
