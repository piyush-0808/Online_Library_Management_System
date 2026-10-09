package library.management.system;

import javax.swing.SwingUtilities;
import javax.swing.UIManager;

import library.management.system.ui.LoginFrame;
import library.management.system.ui.SplashScreen;

public class Main {

    public static void main(String[] args) {

        try {
            UIManager.setLookAndFeel(
                    UIManager.getSystemLookAndFeelClassName()
            );
        } catch (Exception e) {
            System.err.println(
                    "Could not set system look and feel: "
                            + e.getMessage()
            );
        }

        SwingUtilities.invokeLater(() -> {

            SplashScreen splash =
                    new SplashScreen();

            splash.showSplash(() -> {

                LoginFrame loginFrame =
                        new LoginFrame();

                loginFrame.setVisible(true);
            });
        });
    }
}