package library.management.system.ui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;

import library.management.system.exception.DatabaseException;
import library.management.system.exception.InvalidInputException;
import library.management.system.model.Fine;
import library.management.system.model.Issue;
import library.management.system.model.Reservation;
import library.management.system.model.User;
import library.management.system.member.Member;
import library.management.system.service.AuthenticationService;
import library.management.system.service.FineService;
import library.management.system.service.IssueService;
import library.management.system.service.ReservationService;
import library.management.system.util.MessageUtil;

public final class MemberDashboardFrame extends BaseFrame {
    private static final long serialVersionUID = 1L;

    private final transient User loggedInUser;

    private final transient AuthenticationService authenticationService;
    private final transient IssueService issueService;
    private final transient FineService fineService;
    private final transient ReservationService reservationService;

    private transient Member member;

    private JLabel memberIdLabel;
    private JLabel memberTypeLabel;
    private JLabel borrowedBooksLabel;
    private JLabel unpaidFineLabel;
    private JLabel reservationLabel;
    private JLabel clockLabel;

    private volatile boolean clockRunning;
    private transient Thread clockThread;

    public MemberDashboardFrame(User user) {

        super(
                "Library Management System - Member Dashboard",
                1200,
                760
        );

        this.loggedInUser = user;

        this.authenticationService =
                new AuthenticationService();

        this.issueService =
                new IssueService();

        this.fineService =
                new FineService();

        this.reservationService =
                new ReservationService();

        loadMember();
    }

    private void loadMember() {

        try {

            member =
                    authenticationService
                            .loadMemberProfile(
                                    loggedInUser
                            );

            initializeUI();

            startClock();

            loadDashboardData();

        } catch (Exception e) {

            MessageUtil.showError(
                    this,
                    "Could not load member profile.\n"
                            + e.getMessage()
            );

            System.err.println(
                    "Member profile error: "
                            + e.getMessage()
            );
        }
    }

    private void initializeUI() {

        JPanel header =
                createHeader(
                        "Member Dashboard",
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
         * Profile card.
         */
        JPanel profileCard =
                createCard();

        JPanel profilePanel =
                new JPanel(
                        new GridLayout(
                                1,
                                3,
                                20,
                                0
                        )
                );

        profilePanel.setOpaque(false);

        memberIdLabel =
                createProfileValue(
                        "Member ID",
                        member.getMemberId()
                );

        memberTypeLabel =
                createProfileValue(
                        "Member Type",
                        member.getClass()
                                .getSimpleName()
                );

        JLabel departmentLabel =
                createProfileValue(
                        "Department",
                        member.getDepartment()
                );

        profilePanel.add(
                memberIdLabel
        );

        profilePanel.add(
                memberTypeLabel
        );

        profilePanel.add(
                departmentLabel
        );

        profileCard.add(
                profilePanel,
                BorderLayout.CENTER
        );

        mainPanel.add(
                profileCard,
                BorderLayout.NORTH
        );

        /*
         * Statistics.
         */
        JPanel statsPanel =
                new JPanel(
                        new GridLayout(
                                1,
                                3,
                                15,
                                15
                        )
                );

        statsPanel.setOpaque(false);

        borrowedBooksLabel =
                createValueLabel();

        unpaidFineLabel =
                createValueLabel();

        reservationLabel =
                createValueLabel();

        statsPanel.add(
                createStatCard(
                        "Borrowed Books",
                        borrowedBooksLabel,
                        UITheme.ACCENT
                )
        );

        statsPanel.add(
                createStatCard(
                        "Unpaid Fine",
                        unpaidFineLabel,
                        UITheme.DANGER
                )
        );

        statsPanel.add(
                createStatCard(
                        "Waiting Reservations",
                        reservationLabel,
                        UITheme.WARNING
                )
        );

        mainPanel.add(
                statsPanel,
                BorderLayout.CENTER
        );

        /*
         * Quick actions.
         */
        JPanel actionCard =
                createCard();

        actionCard.add(
                createSectionTitle(
                        "Quick Actions"
                ),
                BorderLayout.NORTH
        );

        JPanel actions =
                new JPanel(
                        new GridLayout(
                                1,
                                5,
                                12,
                                12
                        )
                );

        actions.setOpaque(false);

        JButton searchButton =
                createButton(
                        "Search Books"
                );

        JButton historyButton =
                createButton(
                        "Borrowing History"
                );

        JButton finesButton =
                createButton(
                        "My Fines"
                );

        JButton reservationsButton =
                createButton(
                        "My Reservations"
                );

        JButton logoutButton =
                createButton(
                        "Logout",
                        UITheme.DANGER
                );

        actions.add(searchButton);
        actions.add(historyButton);
        actions.add(finesButton);
        actions.add(reservationsButton);
        actions.add(logoutButton);

        actionCard.add(
                actions,
                BorderLayout.CENTER
        );

        mainPanel.add(
                actionCard,
                BorderLayout.SOUTH
        );

        contentPanel.add(
                mainPanel,
                BorderLayout.CENTER
        );

        searchButton.addActionListener(
                e -> openModule(new BookSearchFrame())
        );

        historyButton.addActionListener(
                e -> openModule(
                        new BorrowingHistoryFrame(
                                member.getMemberId(), true))
        );

        finesButton.addActionListener(
                e -> openModule(
                        new FineManagementFrame(member.getMemberId()))
        );

        reservationsButton.addActionListener(
                e -> openModule(
                        new ReservationManagementFrame(loggedInUser))
        );

        logoutButton.addActionListener(
                e -> logout()
        );
    }

    private JLabel createProfileValue(
            String title,
            String value) {

        JLabel label =
                new JLabel(
                        "<html><b>"
                                + title
                                + "</b><br>"
                                + safe(value)
                                + "</html>"
                );

        label.setFont(
                UITheme.NORMAL_FONT
        );

        label.setForeground(
                UITheme.TEXT
        );

        return label;
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

        JPanel accentPanel =
                new JPanel();

        accentPanel.setBackground(accent);

        accentPanel.setPreferredSize(
                new Dimension(
                        5,
                        1
                )
        );

        card.add(
                accentPanel,
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
                new JLabel("—");

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

    private void loadDashboardData() {

        Thread dataThread =
                new Thread(
                        () -> {

                            try {

                                List<Issue> issues =
                                        issueService
                                                .getActiveIssues()
                                                .stream()
                                                .filter(
                                                        issue ->
                                                                member.getMemberId()
                                                                        .equals(
                                                                                issue.getMemberId()
                                                                        )
                                                )
                                                .toList();

                                List<Fine> fines =
                                        fineService
                                                .findUnpaidByMemberId(
                                                        member.getMemberId()
                                                );

                                List<Reservation> reservations =
                                        reservationService
                                                .findByMemberId(
                                                        member.getMemberId()
                                                )
                                                .stream()
                                                .filter(
                                                        reservation ->
                                                                reservation.isWaiting()
                                                                        || reservation.isReady()
                                                )
                                                .toList();

                                double unpaidAmount =
                                        fines.stream()
                                                .mapToDouble(
                                                        Fine::getAmount
                                                )
                                                .sum();

                                SwingUtilities.invokeLater(
                                        () -> {

                                            borrowedBooksLabel
                                                    .setText(
                                                            String.valueOf(
                                                                    issues.size()
                                                            )
                                                    );

                                            unpaidFineLabel
                                                    .setText(
                                                            String.format(
                                                                    "₹ %.2f",
                                                                    unpaidAmount
                                                            )
                                                    );

                                            reservationLabel
                                                    .setText(
                                                            String.valueOf(
                                                                    reservations.size()
                                                            )
                                                    );
                                        }
                                );

                            } catch (
                                    DatabaseException
                                    | InvalidInputException
                                    | RuntimeException e) {

                                System.err.println(
                                        "Dashboard data error: "
                                                + e.getMessage()
                                );
                            }
                        },
                        "MemberDashboardLoader"
                );

        dataThread.start();
    }

    /**
     * Required daemon clock thread.
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
                                        () ->
                                                clockLabel.setText(
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
                        "MemberDashboardClock"
                );

        clockThread.setDaemon(true);

        clockThread.start();
    }

    private void logout() {

        clockRunning = false;

        if (clockThread != null) {
            clockThread.interrupt();
        }

        new LoginFrame().showFrame();

        dispose();
    }

    @Override
    protected void refreshAfterChild() {
        loadDashboardData();
    }

    private void openModule(BaseFrame frame) {
        frame.setNavigationParent(this);
        frame.showFrame();
    }

    private String safe(
            String value) {

        return value == null
                ? "-"
                : value;
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
