package library.management.system.ui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;

import library.management.system.model.User;
import library.management.system.service.ReportService;
import library.management.system.service.OverdueMonitor;

public final class LibrarianDashboardFrame extends BaseFrame {
    private static final long serialVersionUID = 1L;

    private final transient User loggedInUser;
    private final transient ReportService reportService;
    private final transient OverdueMonitor overdueMonitor;

    private JLabel clockLabel;

    private JLabel totalBooksLabel;
    private JLabel availableBooksLabel;
    private JLabel activeIssuesLabel;
    private JLabel overdueLabel;
    private JLabel membersLabel;
    private JLabel unpaidFineLabel;

    private volatile boolean clockRunning;
    private transient Thread clockThread;

    public LibrarianDashboardFrame(User user) {

        super(
                "Library Management System - Dashboard",
                1200,
                760
        );

        this.loggedInUser = user;
        this.reportService = new ReportService();
        this.overdueMonitor = new OverdueMonitor(this::loadDashboard);

        initializeUI();
        startClock();
        loadDashboard();
    }

    private void initializeUI() {

        JPanel header =
                createHeader(
                        "Librarian Dashboard",
                        "Welcome, "
                                + loggedInUser.getFullName()
                );

        clockLabel =
                new JLabel(
                        "",
                        SwingConstants.RIGHT
                );

        clockLabel.setForeground(
                java.awt.Color.WHITE
        );

        clockLabel.setFont(
                UITheme.NORMAL_FONT
        );

        header.add(
                clockLabel,
                BorderLayout.EAST
        );

        add(
                header,
                BorderLayout.NORTH
        );

        JPanel mainPanel =
                new JPanel(
                        new BorderLayout(0, 20)
                );

        mainPanel.setOpaque(false);

        /*
         * Statistics cards.
         */
        JPanel statsPanel =
                new JPanel(
                        new GridLayout(
                                2,
                                3,
                                15,
                                15
                        )
                );

        statsPanel.setOpaque(false);

        totalBooksLabel =
                createValueLabel();

        availableBooksLabel =
                createValueLabel();

        activeIssuesLabel =
                createValueLabel();

        overdueLabel =
                createValueLabel();

        membersLabel =
                createValueLabel();

        unpaidFineLabel =
                createValueLabel();

        statsPanel.add(
                createStatCard(
                        "Total Books",
                        totalBooksLabel,
                        UITheme.ACCENT
                )
        );

        statsPanel.add(
                createStatCard(
                        "Available Copies",
                        availableBooksLabel,
                        UITheme.SUCCESS
                )
        );

        statsPanel.add(
                createStatCard(
                        "Active Issues",
                        activeIssuesLabel,
                        UITheme.ACCENT
                )
        );

        statsPanel.add(
                createStatCard(
                        "Overdue",
                        overdueLabel,
                        UITheme.DANGER
                )
        );

        statsPanel.add(
                createStatCard(
                        "Members",
                        membersLabel,
                        UITheme.WARNING
                )
        );

        statsPanel.add(
                createStatCard(
                        "Unpaid Fines",
                        unpaidFineLabel,
                        UITheme.DANGER
                )
        );

        mainPanel.add(
                statsPanel,
                BorderLayout.NORTH
        );

        /*
         * Quick actions.
         */
        JPanel actionsCard =
                createCard();

        actionsCard.add(
                createSectionTitle(
                        "Quick Actions"
                ),
                BorderLayout.NORTH
        );

        JPanel actionsPanel =
                new JPanel(
                        new GridLayout(
                                2,
                                4,
                                12,
                                12
                        )
                );

        actionsPanel.setOpaque(false);

        JButton booksButton =
                createButton(
                        "Manage Books"
                );

        JButton membersButton =
                createButton(
                        "Manage Members"
                );

        JButton issueButton =
                createButton(
                        "Issue Book"
                );

        JButton returnButton =
                createButton(
                        "Return Book"
                );

        JButton reservationsButton =
                createButton(
                        "Reservations"
                );

        JButton finesButton =
                createButton(
                        "Fines"
                );

        JButton reportsButton =
                createButton(
                        "Reports"
                );

        JButton logoutButton =
                createButton(
                        "Logout",
                        UITheme.DANGER
                );

        for (JButton button : new JButton[]{
                booksButton, membersButton, issueButton, returnButton,
                reservationsButton, finesButton, reportsButton, logoutButton
        }) {
            button.setForeground(UITheme.TEXT);
            button.setBackground(java.awt.Color.WHITE);
            button.setOpaque(true);
            button.setContentAreaFilled(true);
        }

        actionsPanel.add(booksButton);
        actionsPanel.add(membersButton);
        actionsPanel.add(issueButton);
        actionsPanel.add(returnButton);
        actionsPanel.add(reservationsButton);
        actionsPanel.add(finesButton);
        actionsPanel.add(reportsButton);
        actionsPanel.add(logoutButton);

        actionsCard.add(
                actionsPanel,
                BorderLayout.CENTER
        );

        mainPanel.add(
                actionsCard,
                BorderLayout.CENTER
        );

        contentPanel.add(
                mainPanel,
                BorderLayout.CENTER
        );

        booksButton.addActionListener(
                e -> openModule(new BookManagementFrame(loggedInUser))
        );

        membersButton.addActionListener(
                e -> openModule(new MemberManagementFrame(loggedInUser))
        );

        issueButton.addActionListener(
                e -> openModule(new IssueReturnFrame(loggedInUser))
        );

        returnButton.addActionListener(
                e -> openModule(new IssueReturnFrame(loggedInUser))
        );

        reservationsButton.addActionListener(
                e -> openModule(new ReservationManagementFrame(loggedInUser))
        );

        finesButton.addActionListener(
                e -> openModule(new FineManagementFrame(loggedInUser))
        );

        reportsButton.addActionListener(
                e -> openModule(new ReportFrame())
        );

        logoutButton.addActionListener(
                e -> logout()
        );
    }

    private JPanel createStatCard(
            String title,
            JLabel valueLabel,
            java.awt.Color accent) {

        JPanel card =
                createCard();

        JPanel content =
                new JPanel(
                        new BorderLayout()
                );

        content.setOpaque(false);

        JLabel titleLabel =
                new JLabel(title);

        titleLabel.setFont(
                UITheme.LABEL_FONT
        );

        titleLabel.setForeground(
                UITheme.MUTED_TEXT
        );

        content.add(
                titleLabel,
                BorderLayout.NORTH
        );

        content.add(
                valueLabel,
                BorderLayout.CENTER
        );

        JPanel accentBar =
                new JPanel();

        accentBar.setBackground(accent);

        accentBar.setPreferredSize(
                new Dimension(
                        5,
                        1
                )
        );

        card.add(
                accentBar,
                BorderLayout.WEST
        );

        card.add(
                content,
                BorderLayout.CENTER
        );

        return card;
    }

    private JLabel createValueLabel() {

        JLabel label =
                new JLabel(
                        "—"
                );

        label.setFont(
                new Font(
                        "Segoe UI",
                        Font.BOLD,
                        28
                )
        );

        label.setForeground(
                UITheme.TEXT
        );

        return label;
    }

    private void loadDashboard() {

        new Thread(
                () -> {

                    try {

                        ReportService.DashboardReport report =
                                reportService
                                        .getDashboardReport();

                        SwingUtilities.invokeLater(
                                () -> updateDashboard(
                                        report
                                )
                        );

                    } catch (Exception e) {

                        System.err.println(
                                "Dashboard loading error: "
                                        + e.getMessage()
                        );
                    }

                },
                "DashboardLoader"
        ).start();
    }

    private void updateDashboard(
            ReportService.DashboardReport report) {

        totalBooksLabel.setText(
                String.valueOf(
                        report.getTotalBooks()
                )
        );

        availableBooksLabel.setText(
                String.valueOf(
                        report.getAvailableCopies()
                )
        );

        activeIssuesLabel.setText(
                String.valueOf(
                        report.getActiveIssues()
                )
        );

        overdueLabel.setText(
                String.valueOf(
                        report.getOverdueIssues()
                )
        );

        membersLabel.setText(
                String.valueOf(
                        report.getTotalMembers()
                )
        );

        unpaidFineLabel.setText(
                String.format(
                        "₹ %.2f",
                        report.getUnpaidFines()
                )
        );
    }

    /**
     * Required daemon live-clock thread.
     */
    private void startClock() {

        clockRunning = true;

        clockThread =
                new Thread(
                        () -> {

                            DateTimeFormatter formatter =
                                    DateTimeFormatter.ofPattern(
                                            "dd MMM yyyy  |  hh:mm:ss a"
                                    );

                            while (clockRunning) {

                                String currentTime =
                                        LocalDateTime
                                                .now()
                                                .format(formatter);

                                SwingUtilities.invokeLater(
                                        () -> clockLabel
                                                .setText(
                                                        currentTime
                                                )
                                );

                                try {

                                    Thread.sleep(1000);

                                } catch (InterruptedException e) {

                                    Thread.currentThread()
                                            .interrupt();

                                    break;
                                }
                            }
                        },
                        "DashboardClock"
                );

        clockThread.setDaemon(true);
        clockThread.start();
    }

    private void logout() {

        clockRunning = false;

        overdueMonitor.close();

        if (clockThread != null) {
            clockThread.interrupt();
        }

        new LoginFrame().showFrame();

        dispose();
    }

    private void openModule(BaseFrame frame) {
        frame.setNavigationParent(this);
        frame.showFrame();
    }

    @Override
    protected void refreshAfterChild() {
        loadDashboard();
    }

    @Override
    public void dispose() {

        clockRunning = false;

        if (clockThread != null) {
            clockThread.interrupt();
        }

        super.dispose();
    }
}
