package library.management.system.service;

import java.util.List;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import library.management.system.dao.BookDAO;
import library.management.system.exception.DatabaseException;
import library.management.system.exception.DuplicateRecordException;
import library.management.system.exception.InvalidInputException;
import library.management.system.item.Book;
import library.management.system.item.LibraryItem;
import library.management.system.item.ReferenceBook;
import library.management.system.model.ItemCategory;
import library.management.system.util.Validator;

/**
 * Service layer for library book operations.
 *
 * Architecture:
 * UI -> BookService -> BookDAO -> Database
 *
 * Business rules are kept in this service instead of the UI.
 */
public class BookService {

    private final BookDAO bookDAO;

    public BookService() {
        this.bookDAO = new BookDAO();
    }

    /**
     * Adds a normal borrowable book.
     */
    public LibraryItem addBook(
            String isbn,
            String title,
            String author,
            String category,
            int publicationYear,
            int totalCopies,
            int availableCopies,
            String publisher,
            String description)
            throws InvalidInputException,
            DuplicateRecordException,
            DatabaseException {

        validateBook(
                isbn,
                title,
                author,
                category,
                publicationYear,
                totalCopies,
                availableCopies);

        if (isbnExists(isbn)) {
            throw new DuplicateRecordException(
                    "A book with ISBN " + isbn + " already exists.");
        }

        isbn = Validator.normalizeISBN(isbn);

        ItemCategory itemCategory = parseCategory(category);

        Book book = new Book(
                0,
                title,
                author,
                isbn,
                itemCategory,
                totalCopies,
                availableCopies);

        book.setPublisher(publisher);
        book.setPublicationYear(publicationYear);
        book.setDescription(description);

        return bookDAO.save(book);
    }

    /**
     * Adds a reference book.
     *
     * Reference books can never be issued.
     */
    public LibraryItem addReferenceBook(
            String isbn,
            String title,
            String author,
            String category,
            int publicationYear,
            int totalCopies,
            String publisher,
            String description)
            throws InvalidInputException,
            DuplicateRecordException,
            DatabaseException {

        validateBook(
                isbn,
                title,
                author,
                category,
                publicationYear,
                totalCopies,
                totalCopies);

        if (isbnExists(isbn)) {
            throw new DuplicateRecordException(
                    "A book with ISBN " + isbn + " already exists.");
        }

        isbn = Validator.normalizeISBN(isbn);

        ItemCategory itemCategory = parseCategory(category);

        ReferenceBook referenceBook = new ReferenceBook(
                0,
                title,
                author,
                isbn,
                itemCategory,
                totalCopies,
                totalCopies);

        referenceBook.setPublisher(publisher);
        referenceBook.setPublicationYear(publicationYear);
        referenceBook.setDescription(description);

        return bookDAO.save(referenceBook);
    }

    /**
     * Finds a book by ID.
     */
    public Optional<LibraryItem> findBookById(int bookId)
            throws DatabaseException {

        if (bookId <= 0) {
            return Optional.empty();
        }

        return bookDAO.findById(bookId);
    }

    /**
     * Alias used by other services.
     */
    public Optional<LibraryItem> findById(int bookId)
            throws DatabaseException {

        return findBookById(bookId);
    }

    /**
     * Returns all books.
     */
    public List<LibraryItem> getAllBooks()
            throws DatabaseException {

        return bookDAO.findAll();
    }

    /**
     * Alias used by UI.
     */
    public List<LibraryItem> findAll()
            throws DatabaseException {

        return getAllBooks();
    }

    /**
     * Searches books by title, author, ISBN or category.
     */
    public List<LibraryItem> searchBooks(String keyword)
            throws DatabaseException {

        if (keyword == null || keyword.isBlank()) {
            return bookDAO.findAll();
        }

        return bookDAO.search(keyword.trim());
    }

    /**
     * Alias used by UI.
     */
    public List<LibraryItem> search(String keyword)
            throws DatabaseException {

        return searchBooks(keyword);
    }

    /**
     * Updates book information.
     */
    public boolean updateBook(LibraryItem book)
            throws InvalidInputException,
            DuplicateRecordException,
            DatabaseException {

        if (book == null) {
            throw new InvalidInputException(
                    "Book cannot be null.");
        }

        if (book.getItemId() <= 0) {
            throw new InvalidInputException(
                    "Invalid book ID.");
        }

        if (!Validator.isNotBlank(book.getTitle())) {
            throw new InvalidInputException(
                    "Book title cannot be empty.");
        }

        if (!Validator.isNotBlank(book.getAuthor())) {
            throw new InvalidInputException(
                    "Author cannot be empty.");
        }

        if (!Validator.isValidISBN(book.getIsbn())) {
            throw new InvalidInputException(
                    "ISBN must be a valid ISBN-10 or ISBN-13.");
        }

        book.setIsbn(Validator.normalizeISBN(book.getIsbn()));

        Set<String> knownIsbns = new HashSet<>();
        for (LibraryItem existing : bookDAO.findAll()) {
            if (existing.getItemId() != book.getItemId()
                    && existing.getIsbn() != null) {
                knownIsbns.add(Validator.normalizeISBN(existing.getIsbn()));
            }
        }
        boolean duplicateIsbn = knownIsbns.contains(
                Validator.normalizeISBN(book.getIsbn()));
        if (duplicateIsbn) {
            throw new DuplicateRecordException(
                    "A book with ISBN " + book.getIsbn() + " already exists.");
        }

        if (book.getTotalCopies() < 0) {
            throw new InvalidInputException(
                    "Total copies cannot be negative.");
        }

        if (book.getAvailableCopies() < 0) {
            throw new InvalidInputException(
                    "Available copies cannot be negative.");
        }

        if (book.getAvailableCopies() > book.getTotalCopies()) {
            throw new InvalidInputException(
                    "Available copies cannot exceed total copies.");
        }

        return bookDAO.update(book);
    }

    /**
     * Deletes a book.
     */
    public boolean deleteBook(int bookId)
            throws InvalidInputException,
            DatabaseException {

        if (bookId <= 0) {
            throw new InvalidInputException(
                    "Invalid book ID.");
        }

        return bookDAO.delete(bookId);
    }

    /**
     * Alias used by UI.
     */
    public boolean delete(int bookId)
            throws InvalidInputException,
            DatabaseException {

        return deleteBook(bookId);
    }

    /**
     * Checks whether ISBN already exists.
     *
     * BookDAO does not expose findByISBN(), so the service
     * checks the existing collection returned by findAll().
     */
    public boolean isbnExists(String isbn)
            throws DatabaseException {

        if (!Validator.isValidISBN(isbn)) {
            return false;
        }

        String normalizedIsbn = Validator.normalizeISBN(isbn);

        Set<String> existingIsbns = new HashSet<>();
        for (LibraryItem book : bookDAO.findAll()) {
            if (book.getIsbn() != null) {
                existingIsbns.add(Validator.normalizeISBN(book.getIsbn()));
            }
        }
        return existingIsbns.contains(normalizedIsbn);
    }

    /**
     * Checks whether a book can currently be borrowed.
     */
    public boolean canBorrow(LibraryItem book)
            throws InvalidInputException {

        if (book == null) {
            throw new InvalidInputException(
                    "Book cannot be null.");
        }

        if (!(book instanceof Book)) {
            return false;
        }

        return book.isAvailable()
                && book.getAvailableCopies() > 0;
    }

    /**
     * Reference books are never borrowable.
     */
    public boolean isReferenceBook(LibraryItem book) {
        return book instanceof ReferenceBook;
    }

    /**
     * Converts UI category text into ItemCategory.
     */
    private ItemCategory parseCategory(String category)
            throws InvalidInputException {

        if (!Validator.isNotBlank(category)) {
            throw new InvalidInputException(
                    "Category cannot be empty.");
        }

        String normalized = category.trim()
                .toUpperCase()
                .replace(' ', '_')
                .replace('-', '_');

        try {
            return ItemCategory.valueOf(normalized);
        } catch (IllegalArgumentException ex) {
            throw new InvalidInputException(
                    "Invalid item category: " + category);
        }
    }

    /**
     * Validates book input before saving.
     */
    private void validateBook(
            String isbn,
            String title,
            String author,
            String category,
            int publicationYear,
            int totalCopies,
            int availableCopies)
            throws InvalidInputException {

        if (!Validator.isValidISBN(isbn)) {
            throw new InvalidInputException(
                    "ISBN must be a valid ISBN-10 or ISBN-13.");
        }

        if (!Validator.isNotBlank(title)) {
            throw new InvalidInputException(
                    "Book title cannot be empty.");
        }

        if (!Validator.isNotBlank(author)) {
            throw new InvalidInputException(
                    "Author cannot be empty.");
        }

        if (!Validator.isNotBlank(category)) {
            throw new InvalidInputException(
                    "Category cannot be empty.");
        }

        if (publicationYear < 0) {
            throw new InvalidInputException(
                    "Publication year cannot be negative.");
        }

        if (totalCopies <= 0) {
            throw new InvalidInputException(
                    "Total copies must be greater than zero.");
        }

        if (availableCopies < 0) {
            throw new InvalidInputException(
                    "Available copies cannot be negative.");
        }

        if (availableCopies > totalCopies) {
            throw new InvalidInputException(
                    "Available copies cannot exceed total copies.");
        }
    }
}
