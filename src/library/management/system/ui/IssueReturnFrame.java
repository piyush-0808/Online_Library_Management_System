package library.management.system.ui;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SwingWorker;
import javax.swing.table.DefaultTableModel;

import library.management.system.model.Issue;
import library.management.system.model.Role;
import library.management.system.model.User;
import library.management.system.service.IssueService;
import library.management.system.util.MessageUtil;

public final class IssueReturnFrame extends BaseFrame {
    private static final long serialVersionUID = 1L;

    private final transient IssueService issueService;
    private final transient User loggedInLibrarian;

    private JTextField memberIdField;
    private JTextField bookIdField;
    private JTextField loanDaysField;
    private JTextField issueIdField;

    private JTable issueTable;
    private DefaultTableModel tableModel;

    public IssueReturnFrame(User loggedInLibrarian) {

        super(
                "Library Management System - Issue / Return",
                1200,
                760
        );

        if (loggedInLibrarian == null
                || loggedInLibrarian.getUserId() <= 0
                || loggedInLibrarian.getRole() != Role.LIBRARIAN) {
            throw new IllegalArgumentException(
                    "A logged-in librarian is required.");
        }
        this.loggedInLibrarian = loggedInLibrarian;
        issueService = new IssueService();

        initializeUI();
        loadActiveIssues();
    }

    private void initializeUI() {

        add(
                createHeader(
                        "Issue / Return Books",
                        "Issue books to members and process returns"
                ),
                BorderLayout.NORTH
        );

        JPanel mainPanel =
                new JPanel(
                        new BorderLayout(15, 15)
                );

        mainPanel.setOpaque(false);

        mainPanel.add(
                createIssuePanel(),
                BorderLayout.NORTH
        );

        mainPanel.add(
                createIssueTable(),
                BorderLayout.CENTER
        );

        mainPanel.add(
                createReturnPanel(),
                BorderLayout.SOUTH
        );

        contentPanel.add(
                mainPanel,
                BorderLayout.CENTER
        );
    }

    private JPanel createIssuePanel() {

        JPanel card = createCard();

        JPanel form =
                new JPanel(
                        new GridBagLayout()
                );

        form.setOpaque(false);

        GridBagConstraints gbc =
                new GridBagConstraints();

        gbc.insets =
                new Insets(8, 8, 8, 8);

        gbc.fill =
                GridBagConstraints.HORIZONTAL;

        gbc.weightx = 1;

        memberIdField =
                new JTextField();

        bookIdField =
                new JTextField();

        loanDaysField = new JTextField();
        loanDaysField.setToolTipText("Enter how many days the student may keep the book");

        UITheme.styleTextField(memberIdField);
        UITheme.styleTextField(bookIdField);
        UITheme.styleTextField(loanDaysField);

        addField(
                form,
                gbc,
                0,
                "Member ID",
                memberIdField
        );

        addField(
                form,
                gbc,
                1,
                "Book ID",
                bookIdField
        );

        addField(
                form,
                gbc,
                2,
                "Loan Days",
                loanDaysField
        );

        JButton issueButton =
                createButton(
                        "Issue Book"
                );

        JButton clearButton =
                createButton(
                        "Clear"
                );

        issueButton.addActionListener(
                e -> issueBook()
        );

        clearButton.addActionListener(
                e -> clearIssueFields()
        );

        JPanel buttons =
                new JPanel(
                        new FlowLayout(
                                FlowLayout.RIGHT
                        )
                );

        buttons.setOpaque(false);

        buttons.add(issueButton);
        buttons.add(clearButton);

        gbc.gridx = 0;
        gbc.gridy = 3;
        gbc.gridwidth = 2;

        form.add(
                buttons,
                gbc
        );

        card.add(
                form,
                BorderLayout.CENTER
        );

        return card;
    }

    private JPanel createReturnPanel() {

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

        JLabel label =
                new JLabel("Issue ID:");

        label.setForeground(
                UITheme.TEXT
        );

        issueIdField =
                new JTextField(15);

        UITheme.styleTextField(
                issueIdField
        );

        JButton returnButton =
                createButton(
                        "Return Book"
                );

        JButton refreshButton =
                createButton(
                        "Refresh"
                );

        returnButton.addActionListener(
                e -> returnBook()
        );

        refreshButton.addActionListener(
                e -> loadActiveIssues()
        );

        panel.add(label);
        panel.add(issueIdField);
        panel.add(returnButton);
        panel.add(refreshButton);

        card.add(
                panel,
                BorderLayout.CENTER
        );

        return card;
    }

    private JPanel createIssueTable() {

        JPanel card = createCard();

        tableModel =
                new DefaultTableModel(
                        new Object[]{
                                "Issue ID",
                                "Member ID",
                                "Book ID",
                                "Issue Date",
                                "Due Date",
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

        issueTable =
                new JTable(tableModel);

        UITheme.styleTable(
                issueTable
        );

        JScrollPane scrollPane =
                new JScrollPane(
                        issueTable
                );

        UITheme.styleScrollPane(
                scrollPane
        );

        issueTable
                .getSelectionModel()
                .addListSelectionListener(
                        e -> {

                            if (!e.getValueIsAdjusting()) {

                                int row =
                                        issueTable
                                                .getSelectedRow();

                                if (row >= 0) {

                                    int modelRow =
                                            issueTable
                                                    .convertRowIndexToModel(
                                                            row
                                                    );

                                    issueIdField.setText(
                                            String.valueOf(
                                                    tableModel
                                                            .getValueAt(
                                                                    modelRow,
                                                                    0
                                                            )
                                            )
                                    );
                                }
                            }
                        }
                );

        card.add(
                scrollPane,
                BorderLayout.CENTER
        );

        return card;
    }

    private void addField(
            JPanel panel,
            GridBagConstraints gbc,
            int row,
            String labelText,
            JTextField field) {

        gbc.gridx = 0;
        gbc.gridy = row;
        gbc.gridwidth = 1;
        gbc.weightx = 0.2;

        JLabel label =
                new JLabel(labelText);

        label.setForeground(
                UITheme.TEXT
        );

        panel.add(
                label,
                gbc
        );

        gbc.gridx = 1;
        gbc.weightx = 1;

        panel.add(
                field,
                gbc
        );
    }

    private void issueBook() {

        String memberId =
                memberIdField
                        .getText()
                        .trim();

        String bookIdText =
                bookIdField
                        .getText()
                        .trim();

        if (memberId.isEmpty()
                || bookIdText.isEmpty()) {

            MessageUtil.showWarning(
                    this,
                    "Please enter Member ID and Book ID."
            );

            return;
        }

        long bookId;
        int loanDays;

        try {

            bookId =
                    Long.parseLong(
                            bookIdText
                    );

        } catch (NumberFormatException e) {

            MessageUtil.showError(
                    this,
                    "Book ID must be a valid number."
            );

            return;
        }

        try {
            loanDays = Integer.parseInt(loanDaysField.getText().trim());
            if (loanDays <= 0) {
                throw new NumberFormatException();
            }
        } catch (NumberFormatException e) {
            MessageUtil.showError(
                    this,
                    "Loan Days must be a positive whole number."
            );
            return;
        }

        final long finalBookId = bookId;
        final int finalLoanDays = loanDays;

        SwingWorker<Issue, Void> worker =
                new SwingWorker<>() {

                    @Override
                    protected Issue doInBackground()
                            throws Exception {

                        return issueService.issueBook(
                                memberId,
                                finalBookId,
                                loggedInLibrarian.getUserId(),
                                finalLoanDays
                        );
                    }

                    @Override
                    protected void done() {

                        try {

                            Issue issue = get();

                            MessageUtil.showSuccess(
                                    IssueReturnFrame.this,
                                    "Book issued successfully.\n"
                                            + "Issue ID: "
                                            + issue.getIssueId()
                                            + "\nDue Date: "
                                            + issue.getDueDate()
                            );

                            clearIssueFields();
                            loadActiveIssues();

                        } catch (Exception e) {

                            Throwable cause =
                                    e.getCause() != null
                                            ? e.getCause()
                                            : e;

                            String message =
                                    cause.getMessage();

                            if (message == null
                                    || message.isBlank()) {

                                message =
                                        "Book could not be issued.";
                            }

                            MessageUtil.showError(
                                    IssueReturnFrame.this,
                                    message
                            );

                            System.err.println(
                                    "Issue error: "
                                            + message
                            );
                        }
                    }
                };

        worker.execute();
    }

    private void returnBook() {

        String issueIdText =
                issueIdField
                        .getText()
                        .trim();

        if (issueIdText.isEmpty()) {

            MessageUtil.showWarning(
                    this,
                    "Please enter or select an Issue ID."
            );

            return;
        }

        long issueId;

        try {

            issueId =
                    Long.parseLong(
                            issueIdText
                    );

        } catch (NumberFormatException e) {

            MessageUtil.showError(
                    this,
                    "Issue ID must be a valid number."
            );

            return;
        }

        final long finalIssueId = issueId;

        SwingWorker<IssueService.ReturnResult, Void> worker =
                new SwingWorker<>() {

                    @Override
                    protected IssueService.ReturnResult doInBackground()
                            throws Exception {

                        return issueService.returnBook(
                                finalIssueId,
                                loggedInLibrarian.getUserId()
                        );
                    }

                    @Override
                    protected void done() {

                        try {

                            IssueService.ReturnResult result =
                                    get();

                            String message =
                                    "Book returned successfully."
                                            + "\nIssue ID: "
                                            + result.getIssueId()
                                            + "\nStatus: RETURNED"
                                            + "\nDays Late: "
                                            + result.getDaysLate();

                            if (result.hasFine()) {

                                message +=
                                        "\nFine: ₹ "
                                                + String.format(
                                                        "%.2f",
                                                        result.getFineAmount()
                                                );
                            }

                            if (result.getReservedForMemberId() != null) {
                                message += "\nNext copy held for member: "
                                        + result.getReservedForMemberId();
                            }

                            MessageUtil.showSuccess(
                                    IssueReturnFrame.this,
                                    message
                            );

                            issueIdField.setText("");

                            loadActiveIssues();

                        } catch (Exception e) {

                            Throwable cause =
                                    e.getCause() != null
                                            ? e.getCause()
                                            : e;

                            String message =
                                    cause.getMessage();

                            if (message == null
                                    || message.isBlank()) {

                                message =
                                        "Book could not be returned.";
                            }

                            MessageUtil.showError(
                                    IssueReturnFrame.this,
                                    message
                            );

                            System.err.println(
                                    "Return error: "
                                            + message
                            );
                        }
                    }
                };

        worker.execute();
    }

    private void loadActiveIssues() {

        SwingWorker<List<Issue>, Void> worker =
                new SwingWorker<>() {

                    @Override
                    protected List<Issue> doInBackground()
                            throws Exception {

                        return issueService
                                .getActiveIssues();
                    }

                    @Override
                    protected void done() {

                        try {

                            refreshTable(
                                    get()
                            );

                        } catch (Exception e) {

                            Throwable cause =
                                    e.getCause() != null
                                            ? e.getCause()
                                            : e;

                            String message =
                                    cause.getMessage();

                            if (message == null
                                    || message.isBlank()) {

                                message =
                                        "Could not load active issues.";
                            }

                            MessageUtil.showError(
                                    IssueReturnFrame.this,
                                    message
                            );

                            System.err.println(
                                    "Issue loading error: "
                                            + message
                            );
                        }
                    }
                };

        worker.execute();
    }

    private void refreshTable(
            List<Issue> issues) {

        tableModel.setRowCount(0);

        for (Issue issue : issues) {

            tableModel.addRow(
                    new Object[]{
                            issue.getIssueId(),
                            issue.getMemberId(),
                            issue.getBookId(),
                            issue.getIssueDate(),
                            issue.getDueDate(),
                            issue.getStatus()
                    }
            );
        }
    }

    private void clearIssueFields() {

        memberIdField.setText("");
        bookIdField.setText("");
        loanDaysField.setText("");

        memberIdField.requestFocus();
    }
}
