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
import javax.swing.SwingWorker;
import javax.swing.table.DefaultTableModel;

import library.management.system.item.LibraryItem;
import library.management.system.service.BookService;
import library.management.system.util.MessageUtil;

public final class BookSearchFrame extends BaseFrame {
    private static final long serialVersionUID = 1L;

    private final transient BookService bookService;

    private JTextField searchField;
    private JTable bookTable;
    private DefaultTableModel tableModel;

    public BookSearchFrame() {

        super(
                "Library Management System - Search Books",
                1150,
                700
        );

        bookService = new BookService();

        initializeUI();
        loadBooks();
    }

    private void initializeUI() {

        add(
                createHeader(
                        "Search Books",
                        "Find books by title, author, ISBN or category"
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
                new JLabel("Search:");

        label.setForeground(
                UITheme.TEXT
        );

        searchField =
                new JTextField(35);

        UITheme.styleTextField(
                searchField
        );

        searchField.setToolTipText(
                "Title, author, ISBN or category"
        );

        JButton searchButton =
                createButton(
                        "Search"
                );

        JButton clearButton =
                createButton(
                        "Clear"
                );

        JButton refreshButton =
                createButton(
                        "Refresh"
                );

        searchButton.addActionListener(
                e -> searchBooks()
        );

        searchField.addActionListener(
                e -> searchBooks()
        );

        clearButton.addActionListener(
                e -> {
                    searchField.setText("");
                    loadBooks();
                }
        );

        refreshButton.addActionListener(
                e -> loadBooks()
        );

        panel.add(label);
        panel.add(searchField);
        panel.add(searchButton);
        panel.add(clearButton);
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
                                "ID",
                                "ISBN",
                                "Title",
                                "Author",
                                "Category",
                                "Publisher",
                                "Year",
                                "Total Copies",
                                "Available Copies",
                                "Availability"
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

        bookTable =
                new JTable(tableModel);

        UITheme.styleTable(
                bookTable
        );

        JScrollPane scrollPane =
                new JScrollPane(
                        bookTable
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

    private void loadBooks() {

        runSearchTask(
                () -> bookService.findAll()
        );
    }

    private void searchBooks() {

        String query =
                searchField
                        .getText()
                        .trim();

        if (query.isEmpty()) {

            loadBooks();
            return;
        }

        runSearchTask(
                () -> bookService.search(query)
        );
    }

    private void runSearchTask(
            SearchOperation operation) {

        SwingWorker<List<LibraryItem>, Void> worker =
                new SwingWorker<>() {

                    @Override
                    protected List<LibraryItem> doInBackground()
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
                                        "An unexpected error occurred.";
                            }

                            MessageUtil.showError(
                                    BookSearchFrame.this,
                                    message
                            );

                            System.err.println(
                                    "Book search error: "
                                            + message
                            );
                        }
                    }
                };

        worker.execute();
    }

    private void refreshTable(
            List<LibraryItem> books) {

        tableModel.setRowCount(0);

        for (LibraryItem book : books) {

            String availability;

            if (book.getAvailableCopies() > 0
                    && book.isAvailable()) {

                availability = "AVAILABLE";

            } else {

                availability = "NOT AVAILABLE";
            }

            tableModel.addRow(
                    new Object[]{
                            book.getItemId(),
                            book.getIsbn(),
                            book.getTitle(),
                            book.getAuthor(),
                            book.getCategory(),
                            book.getPublisher(),
                            book.getPublicationYear(),
                            book.getTotalCopies(),
                            book.getAvailableCopies(),
                            availability
                    }
            );
        }
    }

    private interface SearchOperation {

        List<LibraryItem> execute()
                throws Exception;
    }
}