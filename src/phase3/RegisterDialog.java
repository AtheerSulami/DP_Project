package phase3;

import javax.swing.*;
import java.awt.*;

/**
 * RegisterDialog – new faculty account registration.
 *
 * FACADE PATTERN:
 *   BEFORE: Called DatabaseManager.getInstance().registerFaculty(...)
 *   AFTER:  Calls SilentCheckFacade.getInstance().register(...)
 */
public class RegisterDialog extends JDialog {

    public RegisterDialog(JFrame parent) {
        super(parent, "Register New Account", true);
        setSize(380, 380);
        setLocationRelativeTo(parent);

        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBackground(Color.WHITE);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 25, 8, 25);
        gbc.fill   = GridBagConstraints.HORIZONTAL;

        JLabel title = new JLabel("Create Faculty Account", SwingConstants.CENTER);
        title.setFont(new Font("Segoe UI", Font.BOLD, 18));
        gbc.gridy = 0; panel.add(title, gbc);

        JTextField nameField = new JTextField();
        nameField.setBorder(BorderFactory.createTitledBorder("Full Name"));
        gbc.gridy = 1; panel.add(nameField, gbc);

        JTextField emailField = new JTextField();
        emailField.setBorder(BorderFactory.createTitledBorder("KAU Email"));
        gbc.gridy = 2; panel.add(emailField, gbc);

        JPasswordField passField = new JPasswordField();
        passField.setBorder(BorderFactory.createTitledBorder("Password"));
        gbc.gridy = 3; panel.add(passField, gbc);

        JLabel msgLbl = new JLabel("", SwingConstants.CENTER);
        msgLbl.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        gbc.gridy = 4; panel.add(msgLbl, gbc);

        JButton regBtn = new JButton("Register");
        regBtn.setBackground(new Color(46, 204, 113));
        regBtn.setForeground(Color.WHITE);
        regBtn.setFocusPainted(false);
        gbc.gridy = 5; panel.add(regBtn, gbc);

        add(panel);

        regBtn.addActionListener(e -> {
            String name  = nameField.getText().trim();
            String email = emailField.getText().trim();
            String pass  = new String(passField.getPassword());

            if (name.isEmpty() || email.isEmpty() || pass.isEmpty()) {
                msgLbl.setForeground(Color.RED);
                msgLbl.setText("All fields are required.");
                return;
            }

            try {
                // FACADE PATTERN: was DatabaseManager.getInstance().registerFaculty(...)
                SilentCheckFacade.getInstance().register(email, pass, name);
                msgLbl.setForeground(new Color(46, 204, 113));
                msgLbl.setText("Account created! You may now log in.");
                regBtn.setEnabled(false);
            } catch (Exception ex) {
                msgLbl.setForeground(Color.RED);
                msgLbl.setText("Error: " + ex.getMessage());
            }
        });
    }
}
