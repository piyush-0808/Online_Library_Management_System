package library.management.system.ui;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.List;

import javax.swing.JComboBox;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SwingWorker;
import javax.swing.table.DefaultTableModel;

import library.management.system.exception.DatabaseException;
import library.management.system.exception.DuplicateRecordException;
import library.management.system.exception.InvalidInputException;
import library.management.system.item.Book;
import library.management.system.item.LibraryItem;
import library.management.system.item.ReferenceBook;
import library.management.system.model.ItemCategory;
import library.management.system.model.Role;
import library.management.system.model.User;
import library.management.system.service.BookService;
import library.management.system.util.MessageUtil;

public final class BookManagementFrame extends BaseFrame {
    private static final long serialVersionUID = 1L;

    private final transient BookService bookService;

    private JTextField isbnField;
    private JTextField titleField;
    private JTextField authorField;
    private JTextField categoryField;
    private JTextField publisherField;
    private JTextField yearField;
    private JTextField copiesField;
    private JTextField descriptionField;
    private JTextField searchField;

    private JComboBox<String> bookTypeCombo;

    private JTable bookTable;
    private DefaultTableModel tableModel;
    private JButton saveBookButton;
    private Integer editingBookId;

    public BookManagementFrame(User authenticatedUser) {

        super(
                "Library Management System - Book Management",
                1250,
                760
        );

        if (authenticatedUser == null
                || authenticatedUser.getUserId() <= 0
                || authenticatedUser.getRole() != Role.LIBRARIAN) {
            throw new IllegalArgumentException(
                    "A logged-in librarian is required to manage books.");
        }

        bookService = new BookService();

        initializeUI();
        loadBooks();
    }

    private void initializeUI() {

        add(
                createHeader(
                        "Book Management",
                        "Add, search and manage library books"
                ),
                BorderLayout.NORTH
        );

        JPanel mainPanel =
                new JPanel(
                        new BorderLayout(15, 15)
                );

        mainPanel.setOpaque(false);

        mainPanel.add(
                createFormPanel(),
                BorderLayout.NORTH
        );

        mainPanel.add(
                createTablePanel(),
                BorderLayout.CENTER
        );

        mainPanel.add(
                createBottomPanel(),
                BorderLayout.SOUTH
        );

        contentPanel.add(
                mainPanel,
                BorderLayout.CENTER
        );
    }

    private JPanel createFormPanel() {

        JPanel card = createCard();

        JPanel form =
                new JPanel(
                        new GridBagLayout()
                );

        form.setOpaque(false);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(4, 8, 4, 8);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        isbnField = new JTextField();
        titleField = new JTextField();
        authorField = new JTextField();
        categoryField = new JTextField();
        publisherField = new JTextField();
        yearField = new JTextField();
        copiesField = new JTextField();
        descriptionField = new JTextField();

        bookTypeCombo =
                new JComboBox<>(
                        new String[]{
                                "BOOK",
                                "REFERENCE_BOOK"
                        }
                );

        styleField(isbnField);
        styleField(titleField);
        styleField(authorField);
        styleField(categoryField);
        styleField(publisherField);
        styleField(yearField);
        styleField(copiesField);
        styleField(descriptionField);

        addCompactField(form, gbc, 0, 0, "ISBN", isbnField);
        addCompactField(form, gbc, 0, 2, "Title", titleField);
        addCompactField(form, gbc, 1, 0, "Author", authorField);
        addCompactField(form, gbc, 1, 2, "Category", categoryField);
        addCompactField(form, gbc, 2, 0, "Publisher", publisherField);
        addCompactField(form, gbc, 2, 2, "Publication Year", yearField);
        addCompactField(form, gbc, 3, 0, "Total Copies", copiesField);
        addCompactField(form, gbc, 3, 2, "Item Type", bookTypeCombo);

        JLabel descriptionLabel = new JLabel("Description");
        descriptionLabel.setForeground(UITheme.TEXT);
        gbc.gridx = 0;
        gbc.gridy = 4;
        gbc.weightx = 0.08;
        form.add(descriptionLabel, gbc);
        gbc.gridx = 1;
        gbc.gridwidth = 3;
        gbc.weightx = 0.92;
        form.add(descriptionField, gbc);

        saveBookButton =
                createButton(
                        "Add Book"
                );

        JButton clearButton =
                createButton(
                        "Clear"
                );

        saveBookButton.addActionListener(
                e -> addBook()
        );

        clearButton.addActionListener(
                e -> clearForm()
        );

        JPanel buttons =
                new JPanel(
                        new FlowLayout(
                                FlowLayout.RIGHT
                        )
                );

        buttons.setOpaque(false);

        buttons.add(saveBookButton);
        buttons.add(clearButton);

        gbc.gridx = 0;
        gbc.gridy = 5;
        gbc.gridwidth = 4;
        gbc.weightx = 1;

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

    private void addCompactField(
            JPanel panel,
            GridBagConstraints gbc,
            int row,
            int column,
            String labelText,
            java.awt.Component component) {

        gbc.gridx = column;
        gbc.gridy = row;
        gbc.gridwidth = 1;
        gbc.weightx = 0.08;

        JLabel label =
                new JLabel(labelText);

        label.setForeground(
                UITheme.TEXT
        );

        panel.add(
                label,
                gbc
        );

        gbc.gridx = column + 1;
        gbc.weightx = 0.42;

        panel.add(
                component,
                gbc
        );
    }

    private void styleField(
            JTextField field) {

        UITheme.styleTextField(field);
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
                                "Total",
                                "Available"
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

        UITheme.styleTable(bookTable);

        JScrollPane scrollPane =
                new JScrollPane(bookTable);

        UITheme.styleScrollPane(
                scrollPane
        );

        card.add(
                scrollPane,
                BorderLayout.CENTER
        );

        return card;
    }

    private JPanel createBottomPanel() {

        JPanel panel =
                new JPanel(
                        new BorderLayout(10, 0)
                );

        panel.setOpaque(false);

        searchField =
                new JTextField();

        UITheme.styleTextField(
                searchField
        );

        searchField.setToolTipText(
                "Search by title, author, ISBN or category"
        );

        JButton searchButton =
                createButton(
                        "Search"
                );

        JButton refreshButton =
                createButton(
                        "Refresh"
                );

        JButton deleteButton =
                createButton(
                        "Delete Selected",
                        UITheme.DANGER
                );

        JButton editButton = createButton("Edit Selected");

        searchButton.addActionListener(
                e -> searchBooks()
        );

        refreshButton.addActionListener(
                e -> loadBooks()
        );

        deleteButton.addActionListener(
                e -> deleteSelectedBook()
        );

        editButton.addActionListener(
                e -> editSelectedBook()
        );

        JPanel searchPanel =
                new JPanel(
                        new BorderLayout(8, 0)
                );

        searchPanel.setOpaque(false);

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

        JPanel actions =
                new JPanel(
                        new FlowLayout(
                                FlowLayout.RIGHT
                        )
                );

        actions.setOpaque(false);

        actions.add(refreshButton);
        actions.add(editButton);
        actions.add(deleteButton);

        panel.add(
                actions,
                BorderLayout.EAST
        );

        return panel;
    }

    private void addBook() {

        try {

            String isbn =
                    isbnField.getText().trim();

            String title =
                    titleField.getText().trim();

            String author =
                    authorField.getText().trim();

            String category =
                    categoryField.getText().trim();

            String publisher =
                    publisherField.getText().trim();

            String yearText =
                    yearField.getText().trim();

            String copiesText =
                    copiesField.getText().trim();

            String description =
                    descriptionField.getText().trim();

            if (isbn.isEmpty()
                    || title.isEmpty()
                    || author.isEmpty()
                    || category.isEmpty()
                    || publisher.isEmpty()
                    || yearText.isEmpty()
                    || copiesText.isEmpty()) {

                throw new InvalidInputException(
                        "Please fill all required fields."
                );
            }

            int year =
                    Integer.parseInt(yearText);

            int copies =
                    Integer.parseInt(copiesText);

            if (year <= 0) {

                throw new InvalidInputException(
                        "Publication year must be positive."
                );
            }

            if (copies <= 0) {

                throw new InvalidInputException(
                        "Total copies must be greater than zero."
                );
            }

            if (editingBookId == null && bookService.isbnExists(isbn)) {

                throw new DuplicateRecordException(
                        "A book with this ISBN already exists."
                );
            }

            String type =
                    (String) bookTypeCombo
                            .getSelectedItem();

            final int finalYear = year;
            final int finalCopies = copies;

            runDatabaseTask(
                    () -> {

                        if (editingBookId != null) {
                            LibraryItem existing = bookService
                                    .findBookById(editingBookId)
                                    .orElseThrow(() ->
                                            new InvalidInputException(
                                                    "Book no longer exists."));
                            int currentlyIssued = existing.getTotalCopies()
                                    - existing.getAvailableCopies();
                            if (currentlyIssued > finalCopies) {
                                throw new InvalidInputException(
                                        "Total copies cannot be less than copies currently issued.");
                            }
                            boolean reference = "REFERENCE_BOOK".equals(type);
                            if ((existing instanceof ReferenceBook) != reference
                                    && currentlyIssued > 0) {
                                throw new InvalidInputException(
                                        "Item type cannot change while copies are issued.");
                            }
                            LibraryItem updated = reference
                                    ? new ReferenceBook(editingBookId, title,
                                            author, isbn,
                                            existing.getCategory(), finalCopies,
                                            finalCopies - currentlyIssued)
                                    : new Book(editingBookId, title, author,
                                            isbn, existing.getCategory(),
                                            finalCopies,
                                            finalCopies - currentlyIssued);
                            updated.setCategory(parseCategory(category));
                            updated.setPublisher(publisher);
                            updated.setPublicationYear(finalYear);
                            updated.setDescription(description);
                            bookService.updateBook(updated);
                            return bookService.findAll();
                        }

                        if ("REFERENCE_BOOK".equals(type)) {

                            bookService.addReferenceBook(
                                    isbn,
                                    title,
                                    author,
                                    category,
                                    finalYear,
                                    finalCopies,
                                    publisher,
                                    description
                            );

                        } else {

                            bookService.addBook(
                                    isbn,
                                    title,
                                    author,
                                    category,
                                    finalYear,
                                    finalCopies,
                                    finalCopies,
                                    publisher,
                                    description
                            );
                        }

                        return bookService.findAll();
                    },
                    "Book added successfully."
            );

        } catch (NumberFormatException e) {

            MessageUtil.showError(
                    this,
                    "Year and copies must be valid numbers."
            );

        } catch (
                InvalidInputException
                        | DuplicateRecordException
                        | DatabaseException e) {

            MessageUtil.showError(
                    this,
                    e.getMessage()
            );
        }
    }

    private void loadBooks() {

        runDatabaseTask(
                () -> bookService.findAll(),
                null
        );
    }

    private void searchBooks() {

        String query =
                searchField.getText().trim();

        if (query.isEmpty()) {

            loadBooks();
            return;
        }

        runDatabaseTask(
                () -> bookService.search(query),
                null
        );
    }

    private void runDatabaseTask(
            DatabaseOperation operation,
            String successMessage) {

        SwingWorker<List<LibraryItem>, Void> worker =
                new SwingWorker<>() {

                    @Override
                    protected List<LibraryItem> doInBackground()
                            throws Exception {

                        Object result =
                                operation.execute();

                        if (result instanceof List<?>) {

                            @SuppressWarnings("unchecked")
                            List<LibraryItem> books =
                                    (List<LibraryItem>) result;

                            return books;
                        }

                        return List.of();
                    }

                    @Override
                    protected void done() {

                        try {

                            List<LibraryItem> books =
                                    get();

                            refreshTable(books);

                            if (successMessage != null) {

                                MessageUtil.showSuccess(
                                        BookManagementFrame.this,
                                        successMessage
                                );

                                clearForm();
                            }

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
                                    BookManagementFrame.this,
                                    message
                            );

                            System.err.println(
                                    "Book operation error: "
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
                            book.getAvailableCopies()
                    }
            );
        }
    }

    private void deleteSelectedBook() {

        int selectedRow =
                bookTable.getSelectedRow();

        if (selectedRow < 0) {

            MessageUtil.showWarning(
                    this,
                    "Please select a book first."
            );

            return;
        }

        int modelRow =
                bookTable.convertRowIndexToModel(
                        selectedRow
                );

        Object idValue =
                tableModel.getValueAt(
                        modelRow,
                        0
                );

        int bookId =
                ((Number) idValue).intValue();

        int choice =
                JOptionPane.showConfirmDialog(
                        this,
                        "Delete the selected book?",
                        "Confirm Delete",
                        JOptionPane.YES_NO_OPTION,
                        JOptionPane.WARNING_MESSAGE
                );

        if (choice != JOptionPane.YES_OPTION) {
            return;
        }

        SwingWorker<Boolean, Void> worker =
                new SwingWorker<>() {

                    @Override
                    protected Boolean doInBackground()
                            throws Exception {

                        return bookService.delete(
                                bookId
                        );
                    }

                    @Override
                    protected void done() {

                        try {

                            if (get()) {

                                MessageUtil.showSuccess(
                                        BookManagementFrame.this,
                                        "Book deleted successfully."
                                );

                                loadBooks();

                            } else {

                                MessageUtil.showWarning(
                                        BookManagementFrame.this,
                                        "Book could not be deleted."
                                );
                            }

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
                                    BookManagementFrame.this,
                                    message
                            );
                        }
                    }
                };

        worker.execute();
    }

    private void clearForm() {

        editingBookId = null;
        saveBookButton.setText("Add Book");

        isbnField.setText("");
        titleField.setText("");
        authorField.setText("");
        categoryField.setText("");
        publisherField.setText("");
        yearField.setText("");
        copiesField.setText("");
        descriptionField.setText("");

        bookTypeCombo.setSelectedIndex(0);

        isbnField.requestFocus();
    }

    private void editSelectedBook() {
        int selectedRow = bookTable.getSelectedRow();
        if (selectedRow < 0) {
            MessageUtil.showWarning(this, "Select a book to edit.");
            return;
        }
        int modelRow = bookTable.convertRowIndexToModel(selectedRow);
        int bookId = ((Number) tableModel.getValueAt(modelRow, 0)).intValue();

        new SwingWorker<LibraryItem, Void>() {
            @Override
            protected LibraryItem doInBackground() throws Exception {
                return bookService.findBookById(bookId)
                        .orElseThrow(() -> new InvalidInputException(
                                "Book no longer exists."));
            }

            @Override
            protected void done() {
                try {
                    LibraryItem book = get();
                    editingBookId = book.getItemId();
                    isbnField.setText(book.getIsbn());
                    titleField.setText(book.getTitle());
                    authorField.setText(book.getAuthor());
                    categoryField.setText(book.getCategory().name());
                    publisherField.setText(book.getPublisher());
                    yearField.setText(String.valueOf(book.getPublicationYear()));
                    copiesField.setText(String.valueOf(book.getTotalCopies()));
                    descriptionField.setText(book.getDescription());
                    bookTypeCombo.setSelectedItem(
                            book instanceof ReferenceBook
                                    ? "REFERENCE_BOOK" : "BOOK");
                    saveBookButton.setText("Save Changes");
                } catch (Exception ex) {
                    Throwable cause = ex.getCause() == null
                            ? ex : ex.getCause();
                    MessageUtil.showError(
                            BookManagementFrame.this,
                            cause.getMessage() == null
                                    ? "Could not load the selected book."
                                    : cause.getMessage());
                }
            }
        }.execute();
    }

    private ItemCategory parseCategory(String category)
            throws InvalidInputException {
        String normalized = category.trim().toUpperCase()
                .replace(' ', '_').replace('-', '_');
        try {
            return ItemCategory.valueOf(normalized);
        } catch (IllegalArgumentException ex) {
            throw new InvalidInputException("Invalid item category: " + category);
        }
    }

    private interface DatabaseOperation {

        Object execute()
                throws Exception;
    }
}
