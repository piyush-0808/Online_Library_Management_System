package library.management.system.util;

import javax.swing.JOptionPane;
import java.awt.Component;

public final class MessageUtil {

    private MessageUtil() {
        // Utility class
    }

    public static void showInfo(Component parent, String message) {
        JOptionPane.showMessageDialog(
                parent,
                message,
                "Information",
                JOptionPane.INFORMATION_MESSAGE
        );
    }

    public static void showSuccess(Component parent, String message) {
        JOptionPane.showMessageDialog(
                parent,
                message,
                "Success",
                JOptionPane.INFORMATION_MESSAGE
        );
    }

    public static void showWarning(Component parent, String message) {
        JOptionPane.showMessageDialog(
                parent,
                message,
                "Warning",
                JOptionPane.WARNING_MESSAGE
        );
    }

    public static void showError(Component parent, String message) {
        JOptionPane.showMessageDialog(
                parent,
                message,
                "Error",
                JOptionPane.ERROR_MESSAGE
        );
    }

    public static boolean confirm(Component parent, String message) {
        int result = JOptionPane.showConfirmDialog(
                parent,
                message,
                "Confirmation",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE
        );

        return result == JOptionPane.YES_OPTION;
    }

    public static String ask(Component parent, String message) {
        return JOptionPane.showInputDialog(
                parent,
                message,
                "Input",
                JOptionPane.QUESTION_MESSAGE
        );
    }

    public static int showConfirm(
        java.awt.Component parent,
        String message) {

    return javax.swing.JOptionPane.showConfirmDialog(
            parent,
            message,
            "Confirmation",
            javax.swing.JOptionPane.YES_NO_OPTION,
            javax.swing.JOptionPane.QUESTION_MESSAGE
    );
}
}