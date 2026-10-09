package library.management.system.ui;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.util.List;
import java.util.Optional;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SwingWorker;
import javax.swing.table.DefaultTableModel;

import library.management.system.model.Fine;
import library.management.system.model.Role;
import library.management.system.model.User;
import library.management.system.service.FineService;
import library.management.system.util.MessageUtil;

public class FineManagementFrame extends AbstractAsyncTableFrame<List<Fine>> {
    private static final long serialVersionUID = 1L;

    private final transient FineService fineService;
    private final String scopedMemberId;
    private final transient User loggedInLibrarian;

    private JTable fineTable;
    private DefaultTableModel tableModel;

    private JTextField memberIdField;
    private JTextField issueIdField;
    private JComboBox<String> statusCombo;
    private boolean loadUnpaidOnly;
    private String requestedMemberId;

    /* Delegating constructors finish setup before the EDT can dispatch registered callbacks. */
    @SuppressWarnings("this-escape")
    public FineManagementFrame(User loggedInLibrarian) {
        this(null, loggedInLibrarian);
    }

    @SuppressWarnings("this-escape")
    public FineManagementFrame(String memberId) {
        this(memberId, null);
    }

    @SuppressWarnings("this-escape")
    private FineManagementFrame(
            String scopedMemberId,
            User loggedInLibrarian) {

        super(
                "Library Management System - Fine Management",
                1200,
                760
        );

        this.scopedMemberId = scopedMemberId;
        this.loggedInLibrarian = loggedInLibrarian;
        if (scopedMemberId == null
                && (loggedInLibrarian == null
                    || loggedInLibrarian.getUserId() <= 0
                    || loggedInLibrarian.getRole() != Role.LIBRARIAN)) {
            throw new IllegalArgumentException(
                    "A logged-in librarian is required.");
        }
        fineService = new FineService();

        initializeUI();
        loadAllFines();
    }

    private void initializeUI() {

        add(
                createHeader(
                        scopedMemberId == null ? "Fine Management" : "My Fines",
                        scopedMemberId == null
                                ? "View and manage member fines"
                                : "View fines for your account"
                ),
                BorderLayout.NORTH
        );

        JPanel mainPanel =
                new JPanel(
                        new BorderLayout(15, 15)
                );

        mainPanel.setOpaque(false);

        if (scopedMemberId == null) {
            mainPanel.add(createSearchPanel(), BorderLayout.NORTH);
        }

        mainPanel.add(
                createTablePanel(),
                BorderLayout.CENTER
        );

        if (scopedMemberId == null) {
            mainPanel.add(createActionPanel(), BorderLayout.SOUTH);
        }

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

        UITheme.styleTextField(
                memberIdField
        );

        JLabel issueLabel =
                new JLabel("Issue ID:");

        issueLabel.setForeground(
                UITheme.TEXT
        );

        issueIdField =
                new JTextField(10);

        UITheme.styleTextField(
                issueIdField
        );

        JLabel statusLabel =
                new JLabel("Status:");

        statusLabel.setForeground(
                UITheme.TEXT
        );

        statusCombo =
                new JComboBox<>(
                        new String[]{
                                "ALL",
                                "UNPAID",
                                "PAID"
                        }
                );

        JButton searchButton =
                createButton(
                        "Search"
                );

        JButton refreshButton =
                createButton(
                        "Refresh"
                );

        searchButton.addActionListener(
                e -> searchFines()
        );

        refreshButton.addActionListener(
                e -> loadAllFines()
        );

        panel.add(memberLabel);
        panel.add(memberIdField);

        panel.add(issueLabel);
        panel.add(issueIdField);

        panel.add(statusLabel);
        panel.add(statusCombo);

        panel.add(searchButton);
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
                                "Fine ID",
                                "Issue ID",
                                "Member ID",
                                "Days Late",
                                "Amount",
                                "Status",
                                "Paid At"
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

        fineTable =
                new JTable(tableModel);

        UITheme.styleTable(
                fineTable
        );

        JScrollPane scrollPane =
                new JScrollPane(
                        fineTable
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

        JButton payButton =
                createButton(
                        "Pay Selected Fine"
                );

        JButton unpaidButton =
                createButton(
                        "Show Unpaid"
                );

        payButton.addActionListener(
                e -> paySelectedFine()
        );

        unpaidButton.addActionListener(
                e -> loadUnpaidFines()
        );

        panel.add(unpaidButton);
        panel.add(payButton);

        card.add(
                panel,
                BorderLayout.CENTER
        );

        return card;
    }

    private void loadAllFines() {
        loadUnpaidOnly = false;
        requestedMemberId = scopedMemberId;
        loadTableData();
    }

    private void loadUnpaidFines() {

        String memberId =
                memberIdField
                        .getText()
                        .trim();

        if (memberId.isEmpty()) {
            loadUnpaidOnly = false;
            requestedMemberId = null;
            loadTableData();
            return;
        }
        memberIdField.setText(memberId);
        loadUnpaidOnly = true;
        requestedMemberId = memberId;
        loadTableData();
    }

    private void searchFines() {

        String memberId =
                memberIdField
                        .getText()
                        .trim();

        String issueIdText =
                issueIdField
                        .getText()
                        .trim();

        String status =
                String.valueOf(
                        statusCombo.getSelectedItem()
                );

        if (!issueIdText.isEmpty()) {

            try {

                long issueId =
                        Long.parseLong(
                                issueIdText
                        );

                runSingleFineTask(issueId);

                return;

            } catch (NumberFormatException e) {

                MessageUtil.showError(
                        this,
                        "Issue ID must be a valid number."
                );

                return;
            }
        }

        if (!memberId.isEmpty()) {

            if ("UNPAID".equals(status)) {

                runTask(
                        () ->
                                fineService
                                        .findUnpaidByMemberId(
                                                memberId
                                        )
                );

            } else if ("PAID".equals(status)) {

                runTask(
                        () ->
                                fineService
                                        .findByMemberId(
                                                memberId
                                        )
                );

            } else {

                runTask(
                        () ->
                                fineService
                                        .findByMemberId(
                                                memberId
                                        )
                );
            }

            return;
        }

        loadAllFines();
    }

    private void runSingleFineTask(
            long issueId) {

        SwingWorker<List<Fine>, Void> worker =
                new SwingWorker<>() {

                    @Override
                    protected List<Fine> doInBackground()
                            throws Exception {

                        Optional<Fine> fineOptional =
                                fineService
                                        .findByIssueId(
                                                issueId
                                        );

                        if (fineOptional.isEmpty()) {
                            return List.of();
                        }

                        return List.of(
                                fineOptional.get()
                        );
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

    private void runTask(
            FineOperation operation) {

        SwingWorker<List<Fine>, Void> worker =
                new SwingWorker<>() {

                    @Override
                    protected List<Fine> doInBackground()
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
            List<Fine> fines) {

        tableModel.setRowCount(0);

        for (Fine fine : fines) {

            tableModel.addRow(
                    new Object[]{
                            fine.getFineId(),
                            fine.getIssueId(),
                            fine.getMemberId(),
                            fine.getDaysLate(),
                            String.format(
                                    "₹ %.2f",
                                    fine.getAmount()
                            ),
                            fine.getStatus(),
                            fine.getPaidAt()
                    }
            );
        }
    }

    @Override
    protected List<Fine> fetchTableData() throws Exception {
        if (scopedMemberId != null) {
            return fineService.findByMemberId(scopedMemberId);
        }
        if (loadUnpaidOnly && requestedMemberId != null
                && !requestedMemberId.isBlank()) {
            return fineService.findUnpaidByMemberId(requestedMemberId);
        }
        return fineService.findAll();
    }

    @Override
    protected void renderTableData(List<Fine> fines) {
        refreshTable(fines);
    }

    private void paySelectedFine() {

        if (scopedMemberId != null) {
            MessageUtil.showWarning(this, "Members can view fines but cannot mark them paid.");
            return;
        }

        int selectedRow =
                fineTable.getSelectedRow();

        if (selectedRow < 0) {

            MessageUtil.showWarning(
                    this,
                    "Please select a fine first."
            );

            return;
        }

        int modelRow =
                fineTable.convertRowIndexToModel(
                        selectedRow
                );

        long fineId =
                ((Number)
                        tableModel.getValueAt(
                                modelRow,
                                0
                        )).longValue();

        String status =
                String.valueOf(
                        tableModel.getValueAt(
                                modelRow,
                                5
                        )
                );

        if ("PAID".equalsIgnoreCase(status)) {

            MessageUtil.showInfo(
                    this,
                    "This fine has already been paid."
            );

            return;
        }

        int choice =
        javax.swing.JOptionPane.showConfirmDialog(
                this,
                "Mark Fine ID "
                        + fineId
                        + " as paid?",
                "Confirm Payment",
                javax.swing.JOptionPane.YES_NO_OPTION,
                javax.swing.JOptionPane.QUESTION_MESSAGE
        );

        if (choice != javax.swing.JOptionPane.YES_OPTION) {
            return;
        }

        SwingWorker<Boolean, Void> worker =
                new SwingWorker<>() {

                    @Override
                    protected Boolean doInBackground()
                            throws Exception {

                        return fineService.payFine(
                                fineId,
                                loggedInLibrarian.getUserId()
                        );
                    }

                    @Override
                    protected void done() {

                        try {

                            if (get()) {

                                MessageUtil.showSuccess(
                                        FineManagementFrame.this,
                                        "Fine paid successfully."
                                );

                                loadAllFines();

                            } else {

                                MessageUtil.showWarning(
                                        FineManagementFrame.this,
                                        "Fine could not be paid."
                                );
                            }

                        } catch (Exception e) {

                            showError(e);
                        }
                    }
                };

        worker.execute();
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
                    "An unexpected error occurred.";
        }

        MessageUtil.showError(
                this,
                message
        );

        System.err.println(
                "Fine operation error: "
                        + message
        );
    }

    private interface FineOperation {

        List<Fine> execute()
                throws Exception;
    }
}
