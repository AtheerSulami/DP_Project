package phase3;

import javax.swing.*;

/**
 * Main – SilentCheck entry point.
 *
 * FACADE PATTERN:
 *   BEFORE: Shutdown hook called BookingServer.stop(), BookingService.shutdown(),
 *           DatabaseManager.getInstance().close() separately.
 *   AFTER:  Calls SilentCheckFacade.getInstance().shutdown() – one call,
 *           all subsystems cleaned up.
 */
public class Main {
    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
        } catch (Exception e) {
            e.printStackTrace();
        }

        // FACADE: single shutdown call replaces three separate subsystem calls
        Runtime.getRuntime().addShutdownHook(new Thread(() ->
            SilentCheckFacade.getInstance().shutdown()
        ));

        SwingUtilities.invokeLater(() -> new LoginFrame().setVisible(true));
    }
}
