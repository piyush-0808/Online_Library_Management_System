package library.management.system.ui;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SwingWorker;
import javax.swing.table.DefaultTableModel;

import library.management.system.exception.DatabaseException;
import library.management.system.member.Member;
import library.management.system.model.MemberStatus;
import library.management.system.model.Role;
import library.management.system.model.User;
import library.management.system.service.MemberService;
import library.management.system.util.MessageUtil;

public final class MemberManagementFrame extends BaseFrame {
    private static final long serialVersionUID = 1L;

    private final transient MemberService memberService;
    private final transient User loggedInLibrarian;

    private JTable memberTable;
    private DefaultTableModel tableModel;

    private JTextField searchField;
    private JComboBox<String> statusCombo;

    public MemberManagementFrame(User loggedInLibrarian) {

        super(
                "Library Management System - Member Management",
                1250,
                760
        );

        if (loggedInLibrarian == null
                || loggedInLibrarian.getUserId() <= 0
                || loggedInLibrarian.getRole() != Role.LIBRARIAN) {
            throw new IllegalArgumentException(
                    "A logged-in librarian is required.");
        }
        this.loggedInLibrarian = loggedInLibrarian;
        memberService = new MemberService();

        initializeUI();
        loadMembers();
    }

    private void initializeUI() {

        add(
                createHeader(
                        "Member Management",
                        "Manage registrations, approvals and member status"
                ),
                BorderLayout.NORTH
        );

        JPanel mainPanel =
                new JPanel(
                        new BorderLayout(15, 15)
                );

        mainPanel.setOpaque(false);

        mainPanel.add(
                createControlPanel(),
                BorderLayout.NORTH
        );

        mainPanel.add(
                createTablePanel(),
                BorderLayout.CENTER
        );

        mainPanel.add(
                createActionPanel(),
                BorderLayout.SOUTH
        );

        contentPanel.add(
                mainPanel,
                BorderLayout.CENTER
        );
    }

    private JPanel createControlPanel() {

        JPanel card = createCard();

        JPanel panel =
                new JPanel(
                        new BorderLayout(10, 10)
                );

        panel.setOpaque(false);

        JPanel searchPanel =
                new JPanel(
                        new BorderLayout(8, 0)
                );

        searchPanel.setOpaque(false);

        searchField =
                new JTextField();

        UITheme.styleTextField(
                searchField
        );

        searchField.setToolTipText(
                "Search by member ID, name, email or phone"
        );

        JButton searchButton =
                createButton(
                        "Search"
                );

        searchButton.addActionListener(
                e -> searchMembers()
        );

        searchField.addActionListener(
                e -> searchMembers()
        );

        searchPanel.add(
                searchField,
                BorderLayout.CENTER
        );

        searchPanel.add(
                searchButton,
                BorderLayout.EAST
        );

        panel.add(
                searchPanel,
                BorderLayout.CENTER
        );

        JPanel filterPanel =
                new JPanel(
                        new FlowLayout(
                                FlowLayout.RIGHT
                        )
                );

        filterPanel.setOpaque(false);

        JLabel statusLabel =
                new JLabel("Status:");

        statusLabel.setForeground(
                UITheme.TEXT
        );

        statusCombo =
                new JComboBox<>(
                        new String[]{
                                "ALL",
                                "PENDING",
                                "ACTIVE",
                                "BLOCKED"
                        }
                );

        JButton filterButton =
                createButton(
                        "Filter"
                );

        JButton refreshButton =
                createButton(
                        "Refresh"
                );

        filterButton.addActionListener(
                e -> filterMembers()
        );

        refreshButton.addActionListener(
                e -> loadMembers()
        );

        filterPanel.add(statusLabel);
        filterPanel.add(statusCombo);
        filterPanel.add(filterButton);
        filterPanel.add(refreshButton);

        panel.add(
                filterPanel,
                BorderLayout.EAST
        );

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
                                "Member ID",
                                "Name",
                                "Email",
                                "Phone",
                                "Type",
                                "Department",
                                "Status",
                                "Joined Date"
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

        memberTable =
                new JTable(tableModel);

        UITheme.styleTable(
                memberTable
        );

        JScrollPane scrollPane =
                new JScrollPane(
                        memberTable
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

        JPanel actions =
                new JPanel(
                        new GridLayout(
                                1,
                                5,
                                10,
                                0
                        )
                );

        actions.setOpaque(false);

        JButton approveButton =
                createButton(
                        "Approve"
                );

        JButton rejectButton =
                createButton(
                        "Reject",
                        UITheme.DANGER
                );

        JButton activateButton =
                createButton(
                        "Activate"
                );

        JButton blockButton =
                createButton(
                        "Block",
                        UITheme.WARNING
                );

        JButton detailsButton =
                createButton(
                        "View Details"
                );

        approveButton.addActionListener(
                e -> approveSelectedMember()
        );

        rejectButton.addActionListener(
                e -> rejectSelectedMember()
        );

        activateButton.addActionListener(
                e -> activateSelectedMember()
        );

        blockButton.addActionListener(
                e -> blockSelectedMember()
        );

        detailsButton.addActionListener(
                e -> showSelectedDetails()
        );

        actions.add(approveButton);
        actions.add(rejectButton);
        actions.add(activateButton);
        actions.add(blockButton);
        actions.add(detailsButton);

        card.add(
                actions,
                BorderLayout.CENTER
        );

        return card;
    }

    private void loadMembers() {

        runMemberTask(
                () -> memberService.findAll()
        );
    }

    private void searchMembers() {

        String query =
                searchField
                        .getText()
                        .trim()
                        .toLowerCase();

        if (query.isEmpty()) {

            loadMembers();
            return;
        }

        runMemberTask(
                () -> {

                    List<Member> all =
                            memberService.findAll();

                    return all.stream()
                            .filter(
                                    member ->
                                            contains(
                                                    member.getMemberId(),
                                                    query
                                            )
                                            || contains(
                                                    member.getFullName(),
                                                    query
                                            )
                                            || contains(
                                                    member.getEmail(),
                                                    query
                                            )
                                            || contains(
                                                    member.getPhone(),
                                                    query
                                            )
                            )
                            .toList();
                }
        );
    }

    private void filterMembers() {

        String selected =
                (String) statusCombo
                        .getSelectedItem();

        if ("ALL".equals(selected)) {

            loadMembers();
            return;
        }

        try {

            MemberStatus status =
                    MemberStatus.valueOf(
                            selected
                    );

            runMemberTask(
                    () ->
                            memberService
                                    .findByStatus(status)
            );

        } catch (IllegalArgumentException e) {

            MessageUtil.showError(
                    this,
                    "Invalid member status."
            );
        }
    }

    private boolean contains(
            String value,
            String query) {

        return value != null
                && value
                        .toLowerCase()
                        .contains(query);
    }

    private void runMemberTask(
            MemberOperation operation) {

        SwingWorker<List<Member>, Void> worker =
                new SwingWorker<>() {

                    @Override
                    protected List<Member> doInBackground()
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

                            Throwable cause =
                                    e.getCause() != null
                                            ? e.getCause()
                                            : e;

                            String message =
                                    cause.getMessage();

                            if (message == null
                                    || message.isBlank()) {

                                message =
                                        "Could not load members.";
                            }

                            MessageUtil.showError(
                                    MemberManagementFrame.this,
                                    message
                            );

                            System.err.println(
                                    "Member loading error: "
                                            + message
                            );
                        }
                    }
                };

        worker.execute();
    }

    private void refreshTable(
            List<Member> members) {

        tableModel.setRowCount(0);

        for (Member member : members) {

            tableModel.addRow(
                    new Object[]{
                            member.getMemberId(),
                            member.getFullName(),
                            member.getEmail(),
                            member.getPhone(),
                            member.getClass()
                                    .getSimpleName()
                                    .replace(
                                            "Member",
                                            ""
                                    ),
                            member.getDepartment(),
                            member.getStatus(),
                            member.getJoinedDate()
                    }
            );
        }
    }

private Member getSelectedMember() {

    int selectedRow =
            memberTable.getSelectedRow();

    if (selectedRow < 0) {

        MessageUtil.showWarning(
                this,
                "Please select a member first."
        );

        return null;
    }

    int modelRow =
            memberTable.convertRowIndexToModel(
                    selectedRow
            );

    String memberId =
            String.valueOf(
                    tableModel.getValueAt(
                            modelRow,
                            0
                    )
            );

    try {

        return memberService
                .findById(memberId)
                .orElse(null);

    } catch (DatabaseException e) {

        MessageUtil.showError(
                this,
                e.getMessage()
        );

        return null;
    }
}

    private void approveSelectedMember() {

        Member member =
                getSelectedMember();

        if (member == null) {
            return;
        }

        if (member.getStatus()
                != MemberStatus.PENDING) {

            MessageUtil.showWarning(
                    this,
                    "Only pending members can be approved."
            );

            return;
        }

        int choice =
                JOptionPane.showConfirmDialog(
                        this,
                        "Approve member "
                                + member.getMemberId()
                                + "?",
                        "Confirm Approval",
                        JOptionPane.YES_NO_OPTION,
                        JOptionPane.QUESTION_MESSAGE
                );

        if (choice != JOptionPane.YES_OPTION) {
            return;
        }

        runMemberMutation(
                () -> memberService.approveMember(
                        member.getMemberId(),
                        loggedInLibrarian.getUserId()),
                "Member approved successfully."
        );
    }

    private void rejectSelectedMember() {

        Member member =
                getSelectedMember();

        if (member == null) {
            return;
        }

        if (member.getStatus()
                != MemberStatus.PENDING) {

            MessageUtil.showWarning(
                    this,
                    "Only pending members can be rejected."
            );

            return;
        }

        int choice =
                JOptionPane.showConfirmDialog(
                        this,
                        "Reject registration for "
                                + member.getMemberId()
                                + "?",
                        "Confirm Rejection",
                        JOptionPane.YES_NO_OPTION,
                        JOptionPane.WARNING_MESSAGE
                );

        if (choice != JOptionPane.YES_OPTION) {
            return;
        }

        runMemberMutation(
                () -> memberService.rejectMember(member.getMemberId()),
                "Registration rejected."
        );
    }

    private void activateSelectedMember() {

        Member member =
                getSelectedMember();

        if (member == null) {
            return;
        }

        if (member.getStatus()
                == MemberStatus.ACTIVE) {

            MessageUtil.showWarning(
                    this,
                    "This member is already active."
            );

            return;
        }

        int choice =
                JOptionPane.showConfirmDialog(
                        this,
                        "Activate member "
                                + member.getMemberId()
                                + "?",
                        "Confirm Activation",
                        JOptionPane.YES_NO_OPTION,
                        JOptionPane.QUESTION_MESSAGE
                );

        if (choice != JOptionPane.YES_OPTION) {
            return;
        }

        runStatusChange(
                member,
                MemberStatus.ACTIVE
        );
    }

    private void blockSelectedMember() {

        Member member =
                getSelectedMember();

        if (member == null) {
            return;
        }

        if (member.getStatus() == MemberStatus.PENDING) {
            MessageUtil.showWarning(
                    this,
                    "Approve or reject a pending registration instead of blocking it.");
            return;
        }

        if (member.getStatus()
                == MemberStatus.BLOCKED) {

            MessageUtil.showWarning(
                    this,
                    "This member is already blocked."
            );

            return;
        }

        int choice =
                JOptionPane.showConfirmDialog(
                        this,
                        "Block member "
                                + member.getMemberId()
                                + "?",
                        "Confirm Block",
                        JOptionPane.YES_NO_OPTION,
                        JOptionPane.WARNING_MESSAGE
                );

        if (choice != JOptionPane.YES_OPTION) {
            return;
        }

        runStatusChange(
                member,
                MemberStatus.BLOCKED
        );
    }

    private void runStatusChange(
            Member member,
            MemberStatus status) {

        if (member.getApprovedBy() == null
                && status == MemberStatus.ACTIVE) {
            runMemberMutation(
                    () -> memberService.approveMember(
                            member.getMemberId(),
                            loggedInLibrarian.getUserId()),
                    "Member approved successfully."
            );
            return;
        }

        runMemberMutation(
                () -> {
                    if (!memberService.updateStatus(
                            member.getMemberId(), status)) {
                        throw new IllegalStateException(
                                "Member status was not changed.");
                    }
                },
                "Member status updated to " + status + "."
        );
    }

    private void runMemberMutation(
            MemberMutation mutation,
            String successMessage) {
        new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() throws Exception {
                mutation.execute();
                return null;
            }

            @Override
            protected void done() {
                try {
                    get();
                    MessageUtil.showSuccess(
                            MemberManagementFrame.this, successMessage);
                    loadMembers();
                } catch (Exception ex) {
                    Throwable cause = ex.getCause() == null
                            ? ex : ex.getCause();
                    MessageUtil.showError(
                            MemberManagementFrame.this,
                            cause.getMessage() == null
                                    ? "Member operation failed."
                                    : cause.getMessage());
                }
            }
        }.execute();
    }

    private void showSelectedDetails() {

        Member member =
                getSelectedMember();

        if (member == null) {
            return;
        }

        String details =
                "<html>"
                        + "<b>Member ID:</b> "
                        + safe(member.getMemberId())
                        + "<br><br>"
                        + "<b>Name:</b> "
                        + safe(member.getFullName())
                        + "<br>"
                        + "<b>Email:</b> "
                        + safe(member.getEmail())
                        + "<br>"
                        + "<b>Phone:</b> "
                        + safe(member.getPhone())
                        + "<br>"
                        + "<b>Department:</b> "
                        + safe(member.getDepartment())
                        + "<br>"
                        + "<b>Status:</b> "
                        + safe(
                                member.getStatus() == null
                                        ? null
                                        : member.getStatus().name()
                        )
                        + "<br>"
                        + "<b>Joined:</b> "
                        + safe(
                                member.getJoinedDate() == null
                                        ? null
                                        : member.getJoinedDate().toString()
                        )
                        + "</html>";

        MessageUtil.showInfo(
                this,
                details
        );
    }

    private String safe(
            String value) {

        return value == null
                ? "-"
                : value;
    }

    private interface MemberOperation {

        List<Member> execute()
                throws Exception;
    }

    @FunctionalInterface
    private interface MemberMutation {

        void execute() throws Exception;
    }
}
