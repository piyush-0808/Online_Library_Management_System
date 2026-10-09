package library.management.system.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.RenderingHints;

import javax.swing.BorderFactory;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JProgressBar;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

public final class SplashScreen extends JFrame {
    private static final long serialVersionUID = 1L;

    private final JProgressBar progressBar;
    private final JLabel statusLabel;

    private transient Thread loadingThread;

    public SplashScreen() {

        setTitle("Library Management System");

        setSize(620, 380);

        setUndecorated(true);

        setLocationRelativeTo(null);

        setDefaultCloseOperation(
                JFrame.EXIT_ON_CLOSE
        );

        JPanel mainPanel =
                new GradientPanel();

        mainPanel.setLayout(
                new BorderLayout()
        );

        /*
         * Center content.
         */
        JPanel centerPanel =
                new JPanel(
                        new GridBagLayout()
                );

        centerPanel.setOpaque(false);

        GridBagConstraints gbc =
                new GridBagConstraints();

        gbc.gridx = 0;
        gbc.fill =
                GridBagConstraints.HORIZONTAL;

        gbc.insets =
                new Insets(
                        8,
                        40,
                        8,
                        40
                );

        /*
         * Logo.
         */
        JLabel logoLabel =
                new JLabel(
                        "📚",
                        JLabel.CENTER
                );

        logoLabel.setFont(
                new Font(
                        "Segoe UI Emoji",
                        Font.PLAIN,
                        54
                )
        );

        gbc.gridy = 0;

        centerPanel.add(
                logoLabel,
                gbc
        );

        /*
         * Application title.
         */
        JLabel titleLabel =
                new JLabel(
                        "Library Management System",
                        JLabel.CENTER
                );

        titleLabel.setFont(
                new Font(
                        "Segoe UI",
                        Font.BOLD,
                        28
                )
        );

        titleLabel.setForeground(
                Color.WHITE
        );

        gbc.gridy = 1;

        centerPanel.add(
                titleLabel,
                gbc
        );

        /*
         * Subtitle.
         */
        JLabel subtitleLabel =
                new JLabel(
                        "Smart Library • Java Desktop Application",
                        JLabel.CENTER
                );

        subtitleLabel.setFont(
                new Font(
                        "Segoe UI",
                        Font.PLAIN,
                        14
                )
        );

        subtitleLabel.setForeground(
                new Color(
                        226,
                        232,
                        240
                )
        );

        gbc.gridy = 2;

        centerPanel.add(
                subtitleLabel,
                gbc
        );

        /*
         * Progress bar.
         */
        progressBar =
                new JProgressBar(
                        0,
                        100
                );

        progressBar.setValue(0);

        progressBar.setPreferredSize(
                new Dimension(
                        420,
                        10
                )
        );

        progressBar.setBorderPainted(false);

        progressBar.setForeground(
                Color.WHITE
        );

        progressBar.setBackground(
                new Color(
                        255,
                        255,
                        255,
                        60
                )
        );

        gbc.gridy = 3;

        gbc.insets =
                new Insets(
                        28,
                        40,
                        5,
                        40
                );

        centerPanel.add(
                progressBar,
                gbc
        );

        /*
         * Loading status.
         */
        statusLabel =
                new JLabel(
                        "Starting application...",
                        JLabel.CENTER
                );

        statusLabel.setFont(
                new Font(
                        "Segoe UI",
                        Font.PLAIN,
                        12
                )
        );

        statusLabel.setForeground(
                new Color(
                        226,
                        232,
                        240
                )
        );

        gbc.gridy = 4;

        gbc.insets =
                new Insets(
                        5,
                        40,
                        8,
                        40
                );

        centerPanel.add(
                statusLabel,
                gbc
        );

        mainPanel.add(
                centerPanel,
                BorderLayout.CENTER
        );

        /*
         * Footer.
         */
        JLabel footerLabel =
                new JLabel(
                        "Loading...",
                        JLabel.CENTER
                );

        footerLabel.setFont(
                new Font(
                        "Segoe UI",
                        Font.PLAIN,
                        11
                )
        );

        footerLabel.setForeground(
                new Color(
                        203,
                        213,
                        225
                )
        );

        footerLabel.setBorder(
                BorderFactory.createEmptyBorder(
                        10,
                        10,
                        16,
                        10
                )
        );

        mainPanel.add(
                footerLabel,
                BorderLayout.SOUTH
        );

        setContentPane(mainPanel);
    }

    /**
     * Starts splash loading on a separate thread.
     */
    public void startLoading(
            Runnable onComplete) {

        loadingThread =
                new Thread(
                        () -> {

                            String[] messages = {
                                "Initializing application...",
                                "Loading configuration...",
                                "Connecting to database...",
                                "Preparing services...",
                                "Loading user interface...",
                                "Starting library system..."
                            };

                            for (int i = 0;
                                 i < messages.length;
                                 i++) {

                                final int progress =
                                        ((i + 1) * 100)
                                        / messages.length;

                                final String message =
                                        messages[i];

                                SwingUtilities.invokeLater(
                                        () -> {

                                            progressBar
                                                    .setValue(
                                                            progress
                                                    );

                                            statusLabel
                                                    .setText(
                                                            message
                                                    );
                                        }
                                );

                                try {
                                    Thread.sleep(350);
                                } catch (InterruptedException e) {

                                    Thread.currentThread()
                                            .interrupt();

                                    return;
                                }
                            }

                            SwingUtilities.invokeLater(
                                    () -> {

                                        dispose();

                                        if (onComplete != null) {
                                            onComplete.run();
                                        }
                                    }
                            );
                        },
                        "SplashLoader"
                );

        loadingThread.start();
    }

    /**
     * Starts the splash screen.
     */
    public void showSplash(
            Runnable onComplete) {

        setVisible(true);

        startLoading(onComplete);
    }

    /**
     * Gradient background.
     */
    private static class GradientPanel
            extends JPanel {
        private static final long serialVersionUID = 1L;

        @Override
        protected void paintComponent(
                Graphics graphics) {

            Graphics2D g =
                    (Graphics2D) graphics.create();

            g.setRenderingHint(
                    RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON
            );

            GradientPaint gradient =
                    new GradientPaint(
                            0,
                            0,
                            new Color(
                                    30,
                                    41,
                                    59
                            ),
                            getWidth(),
                            getHeight(),
                            new Color(
                                    37,
                                    99,
                                    235
                            )
                    );

            g.setPaint(gradient);

            g.fillRect(
                    0,
                    0,
                    getWidth(),
                    getHeight()
            );

            g.dispose();
        }
    }

    /**
     * Allows standalone testing of the splash screen.
     */
    public static void main(
            String[] args) {

        SwingUtilities.invokeLater(
                () -> {

                    SplashScreen splash =
                            new SplashScreen();

                    splash.showSplash(
                            () -> System.out.println(
                                    "Splash loading complete."
                            )
                    );
                }
        );
    }
}