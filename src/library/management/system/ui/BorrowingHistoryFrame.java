package library.management.system.ui;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.table.DefaultTableModel;

import library.management.system.model.Issue;
import library.management.system.service.IssueService;
import library.management.system.util.MessageUtil;

public class BorrowingHistoryFrame extends AbstractAsyncTableFrame<List<Issue>> {
    private static final long serialVersionUID = 1L;

    private final transient IssueService issueService;
    private final String memberId;
    private final boolean memberScopeOnly;

    private JTable issueTable;
    private DefaultTableModel tableModel;
    private JTextField memberIdField;
    private String requestedMemberId;

    public BorrowingHistoryFrame(String memberId) {
        this(memberId, false);
    }

    /* UI construction registers callbacks, which Swing invokes only after construction. */
    @SuppressWarnings("this-escape")
    public BorrowingHistoryFrame(String memberId, boolean memberScopeOnly) {

        super(
                "Library Management System - Borrowing History",
                1100,
                700
        );

        this.memberId = memberId;
        this.memberScopeOnly = memberScopeOnly;
        this.issueService = new IssueService();

        initializeUI();
        loadHistory();
    }

    private void initializeUI() {

        add(
                createHeader(
                        "Borrowing History",
                        "View book issue and return history"
                ),
                BorderLayout.NORTH
        );

        JPanel mainPanel =
                new JPanel(
                        new BorderLayout(15, 15)
                );

        mainPanel.setOpaque(false);

        if (!memberScopeOnly) {
            mainPanel.add(createSearchPanel(), BorderLayout.NORTH);
        }

        mainPanel.add(
                createTablePanel(),
                BorderLayout.CENTER
        );

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

        JLabel label =
                new JLabel("Member ID:");

        label.setForeground(
                UITheme.TEXT
        );

        memberIdField =
                new JTextField(
                        memberId,
                        18
                );

        UITheme.styleTextField(
                memberIdField
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
                e -> loadHistory()
        );

        refreshButton.addActionListener(
                e -> {
                    memberIdField.setText(memberId);
                    loadHistory();
                }
        );

        panel.add(label);
        panel.add(memberIdField);
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
                                "Issue ID",
                                "Book ID",
                                "Issue Date",
                                "Due Date",
                                "Return Date",
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

        card.add(
                scrollPane,
                BorderLayout.CENTER
        );

        return card;
    }

    private void loadHistory() {

        String selectedMemberId = memberScopeOnly
                ? memberId
                : memberIdField.getText().trim();

        if (selectedMemberId == null || selectedMemberId.isBlank()) {

            MessageUtil.showWarning(
                    this,
                    "Member ID is required."
            );

            return;
        }

        requestedMemberId = selectedMemberId;
        loadTableData();
    }

    @Override
    protected List<Issue> fetchTableData() throws Exception {
        return issueService.getIssuesByMember(requestedMemberId);
    }

    @Override
    protected void renderTableData(List<Issue> issues) {
        refreshTable(issues);
    }

    private void refreshTable(
            List<Issue> issues) {

        tableModel.setRowCount(0);

        for (Issue issue : issues) {

            tableModel.addRow(
                    new Object[]{
                            issue.getIssueId(),
                            issue.getBookId(),
                            issue.getIssueDate(),
                            issue.getDueDate(),
                            issue.getReturnDate(),
                            issue.getStatus()
                    }
            );
        }
    }
}
