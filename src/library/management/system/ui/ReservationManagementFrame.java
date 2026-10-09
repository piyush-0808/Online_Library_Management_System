package library.management.system.ui;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.util.List;
import java.util.Optional;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SwingWorker;
import javax.swing.table.DefaultTableModel;

import library.management.system.model.Reservation;
import library.management.system.model.Role;
import library.management.system.model.User;
import library.management.system.member.Member;
import library.management.system.exception.DatabaseException;
import library.management.system.service.MemberService;
import library.management.system.service.ReservationService;
import library.management.system.util.MessageUtil;

public final class ReservationManagementFrame extends BaseFrame {
    private static final long serialVersionUID = 1L;

    private final transient ReservationService reservationService;
    private final String scopedMemberId;

    private JTable reservationTable;
    private DefaultTableModel tableModel;

    private JTextField memberIdField;
    private JTextField bookIdField;

    public ReservationManagementFrame(User authenticatedUser) {

        super(
                "Library Management System - Reservations",
                1200,
                760
        );

        if (authenticatedUser == null
                || authenticatedUser.getUserId() <= 0
                || (authenticatedUser.getRole() != Role.LIBRARIAN
                    && authenticatedUser.getRole() != Role.MEMBER)) {
            throw new IllegalArgumentException(
                    "A logged-in library account is required.");
        }

        if (authenticatedUser.getRole() == Role.LIBRARIAN) {
            this.scopedMemberId = null;
        } else {
            try {
                Member member = new MemberService()
                        .findByUserId(authenticatedUser.getUserId())
                        .orElseThrow(() -> new IllegalArgumentException(
                                "Member profile was not found for this account."));
                this.scopedMemberId = member.getMemberId();
            } catch (DatabaseException ex) {
                throw new IllegalStateException(
                        "Could not verify the member profile.", ex);
            }
        }

        reservationService = new ReservationService();

        initializeUI();
        loadReservations();
    }

    private void initializeUI() {

        add(
                createHeader(
                        scopedMemberId == null
                                ? "Reservation Management" : "My Reservations",
                        scopedMemberId == null
                                ? "Manage book reservation queues"
                                : "Reserve unavailable books and manage your requests"
                ),
                BorderLayout.NORTH
        );

        JPanel mainPanel =
                new JPanel(
                        new BorderLayout(15, 15)
                );

        mainPanel.setOpaque(false);

        mainPanel.add(
                createSearchPanel(),
                BorderLayout.NORTH
        );

        mainPanel.add(
                createTablePanel(),
                BorderLayout.CENTER
        );

        mainPanel.add(createActionPanel(), BorderLayout.SOUTH);

        contentPanel.add(
                mainPanel,
                BorderLayout.CENTER
        );
    }

    private JPanel createSearchPanel() {

        JPanel card = createCard();

        JPanel panel =
                new JPanel(
                        new FlowLayout(
                                FlowLayout.LEFT,
                                10,
                                5
                        )
                );

        panel.setOpaque(false);

        JLabel memberLabel =
                new JLabel("Member ID:");

        memberLabel.setForeground(
                UITheme.TEXT
        );

        memberIdField =
                new JTextField(15);
        if (scopedMemberId != null) {
            memberIdField.setText(scopedMemberId);
            memberIdField.setEditable(false);
        }

        UITheme.styleTextField(
                memberIdField
        );

        JLabel bookLabel =
                new JLabel("Book ID:");

        bookLabel.setForeground(
                UITheme.TEXT
        );

        bookIdField =
                new JTextField(10);

        UITheme.styleTextField(
                bookIdField
        );

        JButton memberSearchButton =
                createButton(
                        "Search Member"
                );

        JButton bookSearchButton =
                createButton(
                        scopedMemberId == null ? "Search Book" : "Reserve Book"
                );

        JButton refreshButton =
                createButton(
                        "Refresh"
                );

        memberSearchButton.addActionListener(
                e -> searchByMember()
        );

        bookSearchButton.addActionListener(
                e -> {
                    if (scopedMemberId == null) {
                        searchByBook();
                    } else {
                        createReservation();
                    }
                }
        );

        refreshButton.addActionListener(
                e -> loadReservations()
        );

        panel.add(memberLabel);
        panel.add(memberIdField);
        if (scopedMemberId == null) {
            panel.add(memberSearchButton);
        }

        panel.add(bookLabel);
        panel.add(bookIdField);
        panel.add(bookSearchButton);

        panel.add(refreshButton);

        card.add(
                panel,
                BorderLayout.CENTER
        );

        return card;
    }

    private JPanel createTablePanel() {

        JPanel card = createCard();

        tableModel =
                new DefaultTableModel(
                        new Object[]{
                                "Reservation ID",
                                "Member ID",
                                "Book ID",
                                "Reservation Date",
                                "Status"
                        },
                        0
                ) {

                    @Override
                    public boolean isCellEditable(
                            int row,
                            int column) {

                        return false;
                    }
                };

        reservationTable =
                new JTable(tableModel);

        UITheme.styleTable(
                reservationTable
        );

        JScrollPane scrollPane =
                new JScrollPane(
                        reservationTable
                );

        UITheme.styleScrollPane(
                scrollPane
        );

        card.add(
                scrollPane,
                BorderLayout.CENTER
        );

        return card;
    }

    private JPanel createActionPanel() {

        JPanel card = createCard();

        JPanel panel =
                new JPanel(
                        new FlowLayout(
                                FlowLayout.RIGHT
                        )
                );

        panel.setOpaque(false);

        JButton cancelButton =
                createButton(
                        "Cancel Reservation",
                        UITheme.DANGER
                );

        JButton fulfillButton = scopedMemberId == null
                ? createButton("Fulfill Next") : null;

        cancelButton.addActionListener(
                e -> cancelSelected()
        );

        if (fulfillButton != null) {
            fulfillButton.addActionListener(e -> fulfillSelectedBook());
        }

        if (fulfillButton != null) {
            panel.add(fulfillButton);
        }
        panel.add(cancelButton);

        card.add(
                panel,
                BorderLayout.CENTER
        );

        return card;
    }

    private void loadReservations() {

        runTask(
                () -> scopedMemberId == null
                        ? reservationService.findAll()
                        : reservationService.findByMemberId(scopedMemberId)
        );
    }

    private void createReservation() {
        String bookIdText = bookIdField.getText().trim();
        final long bookId;
        try {
            bookId = Long.parseLong(bookIdText);
        } catch (NumberFormatException ex) {
            MessageUtil.showWarning(this, "Enter a valid Book ID.");
            return;
        }

        new SwingWorker<Reservation, Void>() {
            @Override
            protected Reservation doInBackground() throws Exception {
                return reservationService.reserveBook(scopedMemberId, bookId);
            }

            @Override
            protected void done() {
                try {
                    get();
                    MessageUtil.showSuccess(
                            ReservationManagementFrame.this,
                            "Reservation created.");
                    loadReservations();
                } catch (Exception ex) {
                    showError(ex);
                }
            }
        }.execute();
    }

    private void searchByMember() {

        String memberId =
                memberIdField
                        .getText()
                        .trim();

        if (memberId.isEmpty()) {

            MessageUtil.showWarning(
                    this,
                    "Enter a Member ID."
            );

            return;
        }

        runTask(
                () ->
                        reservationService
                                .findByMemberId(
                                        memberId
                                )
        );
    }

    private void searchByBook() {

        String bookIdText =
                bookIdField
                        .getText()
                        .trim();

        if (bookIdText.isEmpty()) {

            MessageUtil.showWarning(
                    this,
                    "Enter a Book ID."
            );

            return;
        }

        try {

            long bookId =
                    Long.parseLong(
                            bookIdText
                    );

            runTask(() -> reservationService.getWaitingQueue(bookId));

        } catch (NumberFormatException e) {

            MessageUtil.showError(
                    this,
                    "Book ID must be a valid number."
            );
        }
    }

    private void runTask(
            ReservationOperation operation) {

        SwingWorker<List<Reservation>, Void> worker =
                new SwingWorker<>() {

                    @Override
                    protected List<Reservation> doInBackground()
                            throws Exception {

                        return operation.execute();
                    }

                    @Override
                    protected void done() {

                        try {

                            refreshTable(
                                    get()
                            );

                        } catch (Exception e) {

                            showError(e);
                        }
                    }
                };

        worker.execute();
    }

    private void refreshTable(
            List<Reservation> reservations) {

        tableModel.setRowCount(0);

        for (Reservation reservation :
                reservations) {

            tableModel.addRow(
                    new Object[]{
                            reservation.getReservationId(),
                            reservation.getMemberId(),
                            reservation.getBookId(),
                            reservation.getReservationDate(),
                            reservation.getStatus()
                    }
            );
        }
    }

    private void cancelSelected() {

        int selectedRow =
                reservationTable
                        .getSelectedRow();

        if (selectedRow < 0) {

            MessageUtil.showWarning(
                    this,
                    "Please select a reservation."
            );

            return;
        }

        int modelRow =
                reservationTable
                        .convertRowIndexToModel(
                                selectedRow
                        );

        long reservationId =
                ((Number)
                        tableModel.getValueAt(
                                modelRow,
                                0
                        )).longValue();

        String status =
                String.valueOf(
                        tableModel.getValueAt(
                                modelRow,
                                4
                        )
                );

        if (!"WAITING".equalsIgnoreCase(status)) {

            MessageUtil.showWarning(
                    this,
                    "Only waiting reservations can be cancelled."
            );

            return;
        }

        int choice =
                MessageUtil.showConfirm(
                        this,
                        "Cancel Reservation ID "
                                + reservationId
                                + "?"
                );

        if (choice != javax.swing.JOptionPane.YES_OPTION) {
            return;
        }

        SwingWorker<Boolean, Void> worker =
                new SwingWorker<>() {

                    @Override
                    protected Boolean doInBackground()
                            throws Exception {

                        return scopedMemberId == null
                                ? reservationService.cancelReservationByLibrarian(
                                        reservationId)
                                : reservationService.cancelReservation(
                                        reservationId, scopedMemberId);
                    }

                    @Override
                    protected void done() {

                        try {

                            if (get()) {

                                MessageUtil.showSuccess(
                                        ReservationManagementFrame.this,
                                        "Reservation cancelled."
                                );

                                loadReservations();

                            } else {

                                MessageUtil.showWarning(
                                        ReservationManagementFrame.this,
                                        "Reservation could not be cancelled."
                                );
                            }

                        } catch (Exception e) {

                            showError(e);
                        }
                    }
                };

        worker.execute();
    }

    private void fulfillSelectedBook() {

        String bookIdText =
                bookIdField
                        .getText()
                        .trim();

        if (bookIdText.isEmpty()) {

            int selectedRow =
                    reservationTable
                            .getSelectedRow();

            if (selectedRow < 0) {

                MessageUtil.showWarning(
                        this,
                        "Select a reservation or enter a Book ID."
                );

                return;
            }

            int modelRow =
                    reservationTable
                            .convertRowIndexToModel(
                                    selectedRow
                            );

            bookIdText =
                    String.valueOf(
                            tableModel.getValueAt(
                                    modelRow,
                                    2
                            )
                    );
        }

        try {

            long bookId =
                    Long.parseLong(
                            bookIdText
                    );

            final long finalBookId = bookId;

            SwingWorker<
                    Optional<Reservation>,
                    Void
                    > worker =
                    new SwingWorker<>() {

                        @Override
                        protected Optional<Reservation>
                        doInBackground()
                                throws Exception {

                            return reservationService
                                    .fulfillNextReservation(
                                            finalBookId
                                    );
                        }

                        @Override
                        protected void done() {

                            try {

                                Optional<Reservation> result =
                                        get();

                                if (result.isEmpty()) {

                                    MessageUtil.showInfo(
                                            ReservationManagementFrame.this,
                                            "No waiting reservation found."
                                    );

                                } else {

                                    Reservation reservation =
                                            result.get();

                                    MessageUtil.showSuccess(
                                            ReservationManagementFrame.this,
                                            "Copy held for member "
                                                    + reservation.getMemberId()
                                    );
                                }

                                loadReservations();

                            } catch (Exception e) {

                                showError(e);
                            }
                        }
                    };

            worker.execute();

        } catch (NumberFormatException e) {

            MessageUtil.showError(
                    this,
                    "Book ID must be a valid number."
            );
        }
    }

    private void showError(
            Exception exception) {

        Throwable cause =
                exception.getCause() != null
                        ? exception.getCause()
                        : exception;

        String message =
                cause.getMessage();

        if (message == null
                || message.isBlank()) {

            message =
                    "Reservation operation failed.";
        }

        MessageUtil.showError(
                this,
                message
        );

        System.err.println(
                "Reservation operation error: "
                        + message
        );
    }

    private interface ReservationOperation {

        List<Reservation> execute()
                throws Exception;
    }
}
