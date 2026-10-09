package library.management.system.ui;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.print.PrinterException;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SwingWorker;
import javax.swing.table.DefaultTableModel;

import library.management.system.service.ReportService;
import library.management.system.util.MessageUtil;

/** Librarian summary and searchable/printable report tables. */
public final class ReportFrame extends BaseFrame {
    private static final long serialVersionUID = 1L;

    private final transient ReportService reportService = new ReportService();
    private JLabel booksLabel;
    private JLabel membersLabel;
    private JLabel issuesLabel;
    private JLabel finesLabel;
    private JTable reportTable;
    private DefaultTableModel reportTableModel;
    private JComboBox<ReportType> reportTypeCombo;
    private JComboBox<String> statusCombo;
    private JTextField searchField;

    public ReportFrame() {
        super("Library Management System - Reports", 1100, 700);
        initializeUI();
        loadReport();
    }

    private void initializeUI() {
        add(createHeader("Library Reports", "Library statistics and system overview"),
                BorderLayout.NORTH);
        JPanel main = new JPanel(new BorderLayout(15, 15));
        main.setOpaque(false);
        main.add(createSummaryPanel(), BorderLayout.NORTH);
        main.add(createDetailsPanel(), BorderLayout.CENTER);
        contentPanel.add(main, BorderLayout.CENTER);
    }

    private JPanel createSummaryPanel() {
        JPanel card = createCard();
        JPanel panel = new JPanel(new GridLayout(1, 4, 15, 0));
        panel.setOpaque(false);
        booksLabel = createValueLabel();
        membersLabel = createValueLabel();
        issuesLabel = createValueLabel();
        finesLabel = createValueLabel();
        panel.add(createStatCard("Books", booksLabel));
        panel.add(createStatCard("Members", membersLabel));
        panel.add(createStatCard("Active Issues", issuesLabel));
        panel.add(createStatCard("Unpaid Fines", finesLabel));
        card.add(panel, BorderLayout.CENTER);
        return card;
    }

    private JPanel createDetailsPanel() {
        JPanel card = createCard();
        JPanel controls = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        controls.setOpaque(false);
        reportTypeCombo = new JComboBox<>(ReportType.values());
        statusCombo = new JComboBox<>();
        updateStatusChoices();
        reportTypeCombo.addActionListener(event -> updateStatusChoices());
        searchField = new JTextField(18);
        UITheme.styleTextField(searchField);
        JButton searchButton = createButton("Search / Filter");
        JButton refreshButton = createButton("Refresh");
        JButton printButton = createButton("Print");
        searchButton.addActionListener(event -> loadDetailedReport());
        searchField.addActionListener(event -> loadDetailedReport());
        refreshButton.addActionListener(event -> loadReport());
        printButton.addActionListener(event -> printReport());
        controls.add(reportTypeCombo);
        controls.add(statusCombo);
        controls.add(searchField);
        controls.add(searchButton);
        controls.add(refreshButton);
        controls.add(printButton);

        reportTableModel = new DefaultTableModel(new Object[0], 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        reportTable = new JTable(reportTableModel);
        reportTable.setAutoCreateRowSorter(true);
        UITheme.styleTable(reportTable);
        card.add(controls, BorderLayout.NORTH);
        card.add(new JScrollPane(reportTable), BorderLayout.CENTER);
        return card;
    }

    private JLabel createValueLabel() {
        JLabel label = new JLabel("Loading...");
        label.setForeground(UITheme.TEXT);
        label.setFont(UITheme.NORMAL_FONT);
        return label;
    }

    private JPanel createStatCard(String title, JLabel value) {
        JPanel card = createCard();
        JLabel heading = new JLabel(title);
        heading.setForeground(UITheme.MUTED_TEXT);
        heading.setFont(UITheme.LABEL_FONT);
        card.add(heading, BorderLayout.NORTH);
        card.add(value, BorderLayout.CENTER);
        return card;
    }

    private void loadReport() {
        SwingWorker<ReportService.DashboardReport, Void> worker = new SwingWorker<>() {
            @Override
            protected ReportService.DashboardReport doInBackground() throws Exception {
                return reportService.getDashboardReport();
            }

            @Override
            protected void done() {
                try {
                    displaySummary(get());
                } catch (Exception ex) {
                    showReportError(ex);
                }
            }
        };
        worker.execute();
        loadDetailedReport();
    }

    private void displaySummary(ReportService.DashboardReport report) {
        booksLabel.setText("<html><b>Books</b><br>" + report.getTotalBooks() + "</html>");
        membersLabel.setText("<html><b>Members</b><br>" + report.getTotalMembers() + "</html>");
        issuesLabel.setText("<html><b>Active Issues</b><br>" + report.getActiveIssues() + "</html>");
        finesLabel.setText("<html><b>Unpaid Fines</b><br>"
                + String.format("₹ %.2f", report.getUnpaidFines()) + "</html>");
    }

    private void loadDetailedReport() {
        ReportType selected = (ReportType) reportTypeCombo.getSelectedItem();
        if (selected == null) {
            return;
        }
        String status = String.valueOf(statusCombo.getSelectedItem());
        String search = searchField.getText().trim();
        SwingWorker<ReportService.ReportTable, Void> worker = new SwingWorker<>() {
            @Override
            protected ReportService.ReportTable doInBackground() throws Exception {
                return reportService.getDetailedReport(selected.key, status, search);
            }

            @Override
            protected void done() {
                try {
                    ReportService.ReportTable result = get();
                    reportTableModel.setDataVector(result.rows().toArray(Object[][]::new),
                            result.columns().toArray());
                    UITheme.styleTable(reportTable);
                } catch (Exception ex) {
                    showReportError(ex);
                }
            }
        };
        worker.execute();
    }

    private void updateStatusChoices() {
        ReportType selected = (ReportType) reportTypeCombo.getSelectedItem();
        String[] choices = selected == ReportType.FINE_COLLECTION
                ? new String[]{"ALL", "UNPAID", "PAID"}
                : selected == ReportType.CURRENTLY_ISSUED
                        ? new String[]{"ISSUED"}
                        : selected == ReportType.OVERDUE
                                ? new String[]{"OVERDUE"}
                                : new String[]{"ALL"};
        statusCombo.setModel(new javax.swing.DefaultComboBoxModel<>(choices));
    }

    private void printReport() {
        try {
            reportTable.print(JTable.PrintMode.FIT_WIDTH,
                    new java.text.MessageFormat("Library Report"),
                    new java.text.MessageFormat("Page {0}"));
        } catch (PrinterException ex) {
            MessageUtil.showError(this, "Could not print report: " + ex.getMessage());
            System.err.println("Report print error: " + ex.getMessage());
        }
    }

    private void showReportError(Exception exception) {
        Throwable cause = exception.getCause() == null ? exception : exception.getCause();
        String message = cause.getMessage() == null || cause.getMessage().isBlank()
                ? "Could not load report." : cause.getMessage();
        MessageUtil.showError(this, message);
        System.err.println("Report error: " + message);
    }

    private enum ReportType {
        CURRENTLY_ISSUED("Currently Issued", "CURRENTLY_ISSUED"),
        OVERDUE("Overdue Books", "OVERDUE"),
        FINE_COLLECTION("Fine Collection", "FINE_COLLECTION"),
        INVENTORY_BY_CATEGORY("Inventory by Category", "INVENTORY_BY_CATEGORY"),
        FINE_TOTALS("Fine Totals", "FINE_TOTALS");

        private final String label;
        private final String key;

        ReportType(String label, String key) {
            this.label = label;
            this.key = key;
        }

        @Override
        public String toString() {
            return label;
        }
    }
}
