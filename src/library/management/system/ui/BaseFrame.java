package library.management.system.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.awt.FlowLayout;
import java.awt.GraphicsEnvironment;
import java.awt.Rectangle;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingConstants;
import javax.swing.border.EmptyBorder;

public abstract class BaseFrame extends JFrame {
    private static final long serialVersionUID = 1L;

    protected static final Color PRIMARY =
            new Color(45, 55, 72);

    protected static final Color SECONDARY =
            new Color(74, 85, 104);

    protected static final Color ACCENT =
            new Color(59, 130, 246);

    protected static final Color SUCCESS =
            new Color(34, 197, 94);

    protected static final Color DANGER =
            new Color(239, 68, 68);

    protected static final Color WARNING =
            new Color(245, 158, 11);

    protected static final Color BACKGROUND =
            new Color(245, 247, 250);

    protected static final Color CARD =
            Color.WHITE;

    protected static final Color TEXT =
            new Color(31, 41, 55);

    protected static final Color MUTED_TEXT =
            new Color(107, 114, 128);

    protected JPanel contentPanel;
    private BaseFrame navigationParent;

    /* Swing requires the frame instance while configuring JFrame's close handling. */
    @SuppressWarnings("this-escape")
    protected BaseFrame(
            String title,
            int width,
            int height) {

        super(title);

        setDefaultCloseOperation(
                JFrame.DISPOSE_ON_CLOSE
        );

        Rectangle usableScreen = GraphicsEnvironment
                .getLocalGraphicsEnvironment().getMaximumWindowBounds();
        int frameWidth = Math.min(width, usableScreen.width);
        int frameHeight = Math.min(height, usableScreen.height);
        setSize(frameWidth, frameHeight);

        setMinimumSize(
                new Dimension(Math.min(1000, frameWidth),
                        Math.min(650, frameHeight))
        );

        setLocationRelativeTo(null);

        setLayout(
                new BorderLayout()
        );

        initializeFrame();
    }

    /**
     * Common frame initialization.
     */
    private void initializeFrame() {

        getContentPane().setBackground(
                BACKGROUND
        );

        contentPanel = new JPanel(
                new BorderLayout()
        );

        contentPanel.setOpaque(false);

        contentPanel.setBorder(
                new EmptyBorder(
                        20,
                        24,
                        20,
                        24
                )
        );

        JScrollPane contentScrollPane = new JScrollPane(contentPanel,
                ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        contentScrollPane.setBorder(null);
        contentScrollPane.setOpaque(false);
        contentScrollPane.getViewport().setOpaque(false);
        add(contentScrollPane, BorderLayout.CENTER);
    }

    /**
     * Creates the common gradient header.
     */
    protected JPanel createHeader(
            String title,
            String subtitle) {

        JPanel header =
                new GradientHeader();

        header.setLayout(
                new BorderLayout(
                        20,
                        0
                )
        );

        header.setBorder(
                new EmptyBorder(
                        18,
                        24,
                        18,
                        24
                )
        );

        JPanel logoPanel =
                createLogoPanel();

        header.add(
                logoPanel,
                BorderLayout.WEST
        );

        JPanel textPanel =
                new JPanel();

        textPanel.setOpaque(false);

        textPanel.setLayout(
                new javax.swing.BoxLayout(
                        textPanel,
                        javax.swing.BoxLayout.Y_AXIS
                )
        );

        JLabel titleLabel =
                new JLabel(title);

        titleLabel.setForeground(
                Color.WHITE
        );

        titleLabel.setFont(
                new Font(
                        "Segoe UI",
                        Font.BOLD,
                        24
                )
        );

        JLabel subtitleLabel =
                new JLabel(subtitle);

        subtitleLabel.setForeground(
                new Color(
                        226,
                        232,
                        240
                )
        );

        subtitleLabel.setFont(
                new Font(
                        "Segoe UI",
                        Font.PLAIN,
                        13
                )
        );

        textPanel.add(titleLabel);
        textPanel.add(subtitleLabel);

        header.add(
                textPanel,
                BorderLayout.CENTER
        );

        return header;
    }

    /**
     * Simple library logo drawn using Java2D.
     */
    protected JPanel createLogoPanel() {

        JPanel logo =
                new JPanel() {

                    @Override
                    protected void paintComponent(
                            Graphics graphics) {

                        super.paintComponent(
                                graphics
                        );

                        Graphics2D g =
                                (Graphics2D) graphics;

                        g.setRenderingHint(
                                RenderingHints.KEY_ANTIALIASING,
                                RenderingHints.VALUE_ANTIALIAS_ON
                        );

                        g.setColor(
                                new Color(
                                        255,
                                        255,
                                        255,
                                        35
                                )
                        );

                        g.fillRoundRect(
                                0,
                                0,
                                getWidth(),
                                getHeight(),
                                18,
                                18
                        );

                        g.setColor(
                                Color.WHITE
                        );

                        int centerX =
                                getWidth() / 2;

                        int centerY =
                                getHeight() / 2;

                        /*
                         * Simple open-book symbol.
                         */
                        g.fillRoundRect(
                                centerX - 24,
                                centerY - 15,
                                20,
                                30,
                                5,
                                5
                        );

                        g.fillRoundRect(
                                centerX + 4,
                                centerY - 15,
                                20,
                                30,
                                5,
                                5
                        );

                        g.drawLine(
                                centerX,
                                centerY - 14,
                                centerX,
                                centerY + 14
                        );
                    }
                };

        logo.setOpaque(false);

        logo.setPreferredSize(
                new Dimension(
                        60,
                        60
                )
        );

        return logo;
    }

    /**
     * Creates a styled button.
     */
    protected JButton createButton(
            String text) {

        return createButton(
                text,
                ACCENT
        );
    }

    /**
     * Creates a styled button with custom color.
     */
    protected JButton createButton(
            String text,
            Color background) {

        JButton button =
                new JButton(text);

        button.setFont(
                new Font(
                        "Segoe UI",
                        Font.BOLD,
                        13
                )
        );

        button.setForeground(
                TEXT
        );

        // Keep the original light button appearance while forcing Windows'
        // look and feel to honor the foreground/background colors.
        button.setBackground(Color.WHITE);

        // Some Windows look and feels ignore button colors unless the button
        // is explicitly opaque, leaving white text on a white button.
        button.setOpaque(true);
        button.setContentAreaFilled(true);
        button.setBorderPainted(true);

        button.setFocusPainted(false);

        button.setBorder(
                BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(background, 1),
                        BorderFactory.createEmptyBorder(9, 17, 9, 17)
                )
        );

        button.setCursor(
                new java.awt.Cursor(
                        java.awt.Cursor.HAND_CURSOR
                )
        );

        button.setMargin(
                new Insets(
                        8,
                        16,
                        8,
                        16
                )
        );

        return button;
    }

    /**
     * Adds a consistent way back to the dashboard and restores it when this
     * module window is closed with the window's X button.
     */
    protected void setNavigationParent(BaseFrame parent) {
        if (parent == null || navigationParent != null) {
            return;
        }
        navigationParent = parent;

        JButton backButton = createButton("← Back to Dashboard", SECONDARY);
        backButton.addActionListener(event -> returnToParent());

        JPanel navigationBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        navigationBar.setOpaque(false);
        navigationBar.add(backButton);
        contentPanel.add(navigationBar, BorderLayout.SOUTH);

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent event) {
                showNavigationParent();
            }
        });
    }

    private void returnToParent() {
        if (navigationParent != null) {
            showNavigationParent();
            dispose();
        }
    }

    private void showNavigationParent() {
        navigationParent.refreshAfterChild();
        navigationParent.setVisible(true);
    }

    /** Called when a child module closes so dashboards can reload live data. */
    protected void refreshAfterChild() {
        // Other frames have no dashboard summary to refresh.
    }

    /**
     * Creates a card-style panel.
     */
    protected JPanel createCard() {

        JPanel card =
                new JPanel(
                        new BorderLayout()
                );

        card.setBackground(
                CARD
        );

        card.setBorder(
                BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(
                                new Color(
                                        226,
                                        232,
                                        240
                                )
                        ),
                        new EmptyBorder(
                                18,
                                18,
                                18,
                                18
                        )
                )
        );

        return card;
    }

    /**
     * Creates a standard label.
     */
    protected JLabel createLabel(
            String text) {

        JLabel label =
                new JLabel(text);

        label.setFont(
                new Font(
                        "Segoe UI",
                        Font.PLAIN,
                        13
                )
        );

        label.setForeground(
                TEXT
        );

        return label;
    }

    /**
     * Creates a section heading.
     */
    protected JLabel createSectionTitle(
            String text) {

        JLabel label =
                new JLabel(text);

        label.setFont(
                new Font(
                        "Segoe UI",
                        Font.BOLD,
                        18
                )
        );

        label.setForeground(
                TEXT
        );

        label.setBorder(
                new EmptyBorder(
                        0,
                        0,
                        10,
                        0
                )
        );

        return label;
    }

    /**
     * Centers the frame on screen.
     */
    protected void centerFrame() {
        setLocationRelativeTo(null);
    }

    /**
     * Shows the frame.
     */
    protected void showFrame() {
        setLocationRelativeTo(null);
        setVisible(true);
    }

    /**
     * Header with gradient background.
     */
    private static class GradientHeader
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
                            PRIMARY,
                            getWidth(),
                            0,
                            ACCENT
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
}
