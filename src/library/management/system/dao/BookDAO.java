package library.management.system.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import library.management.system.db.DBConnection;
import library.management.system.exception.DatabaseException;
import library.management.system.item.LibraryItem;
import library.management.system.item.ItemFactory;
import library.management.system.item.ReferenceBook;
import library.management.system.model.ItemCategory;

/**
 * DAO class for Book database operations.
 *
 * Uses JDBC and PreparedStatement so SQL stays inside the DAO layer.
 */
public class BookDAO implements GenericDAO<LibraryItem, Integer> {

    private final DBConnection dbConnection;

    public BookDAO() {
        this.dbConnection = DBConnection.getInstance();
    }

    /**
     * Finds one book using its database ID.
     */
    @Override
    public Optional<LibraryItem> findById(Integer id) throws DatabaseException {

        String sql = """
                SELECT book_id, title, author, isbn, category,
                       is_reference, publisher, publication_year,
                       total_copies, available_copies, description
                FROM books
                WHERE book_id = ?
                """;

        try (Connection connection = dbConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setInt(1, id);

            try (ResultSet resultSet = statement.executeQuery()) {

                if (resultSet.next()) {
                    return Optional.of(mapResultSet(resultSet));
                }

                return Optional.empty();
            }

        } catch (SQLException e) {
            throw new DatabaseException(
                    "Unable to find book with ID: " + id, e);
        }
    }

    /**
     * Returns all books.
     */
    @Override
    public List<LibraryItem> findAll() throws DatabaseException {

        String sql = """
                SELECT book_id, title, author, isbn, category,
                       is_reference, publisher, publication_year,
                       total_copies, available_copies, description
                FROM books
                ORDER BY title
                """;

        List<LibraryItem> books = new ArrayList<>();

        try (Connection connection = dbConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {

            while (resultSet.next()) {
                books.add(mapResultSet(resultSet));
            }

            return books;

        } catch (SQLException e) {
            throw new DatabaseException(
                    "Unable to load books.", e);
        }
    }

    /**
     * Inserts a new book and returns the saved object.
     */
    @Override
    public LibraryItem save(LibraryItem item) throws DatabaseException {

        String sql = """
                INSERT INTO books
                    (title, author, isbn, category, is_reference,
                     publisher, publication_year, total_copies,
                     available_copies, description)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;

        try (Connection connection = dbConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     sql, Statement.RETURN_GENERATED_KEYS)) {

            statement.setString(1, item.getTitle());
            statement.setString(2, item.getAuthor());
            statement.setString(3, item.getIsbn());
            statement.setString(4, item.getCategory().name());
            statement.setBoolean(5, item instanceof ReferenceBook);
            statement.setString(6, item.getPublisher());
            if (item.getPublicationYear() > 0) {
                statement.setInt(7, item.getPublicationYear());
            } else {
                statement.setNull(7, java.sql.Types.INTEGER);
            }
            statement.setInt(8, item.getTotalCopies());
            statement.setInt(9, item.getAvailableCopies());
            statement.setString(10, item.getDescription());

            statement.executeUpdate();

            try (ResultSet generatedKeys = statement.getGeneratedKeys()) {

                if (generatedKeys.next()) {
                    item.setItemId(generatedKeys.getInt(1));
                }
            }

            return item;

        } catch (SQLException e) {
            throw new DatabaseException(
                    "Unable to save book.", e);
        }
    }

    /**
     * Updates an existing book.
     */
    @Override
    public boolean update(LibraryItem item) throws DatabaseException {

        String sql = """
                UPDATE books
                SET title = ?,
                    author = ?,
                    isbn = ?,
                    category = ?,
                    is_reference = ?,
                    publisher = ?,
                    publication_year = ?,
                    total_copies = ?,
                    available_copies = ?,
                    description = ?
                WHERE book_id = ?
                """;

        try (Connection connection = dbConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, item.getTitle());
            statement.setString(2, item.getAuthor());
            statement.setString(3, item.getIsbn());
            statement.setString(4, item.getCategory().name());
            statement.setBoolean(5, item instanceof ReferenceBook);
            statement.setString(6, item.getPublisher());
            if (item.getPublicationYear() > 0) {
                statement.setInt(7, item.getPublicationYear());
            } else {
                statement.setNull(7, java.sql.Types.INTEGER);
            }
            statement.setInt(8, item.getTotalCopies());
            statement.setInt(9, item.getAvailableCopies());
            statement.setString(10, item.getDescription());
            statement.setInt(11, item.getItemId());

            return statement.executeUpdate() > 0;

        } catch (SQLException e) {
            throw new DatabaseException(
                    "Unable to update book.", e);
        }
    }

    /**
     * Deletes a book using its ID.
     */
    @Override
    public boolean delete(Integer id) throws DatabaseException {

        String sql = """
                DELETE FROM books
                WHERE book_id = ?
                """;

        try (Connection connection = dbConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setInt(1, id);

            return statement.executeUpdate() > 0;

        } catch (SQLException e) {
            throw new DatabaseException(
                    "Unable to delete book with ID: " + id, e);
        }
    }

    /**
     * Searches books by title, author, ISBN or category.
     *
     * PreparedStatement prevents SQL injection.
     */
    public List<LibraryItem> search(String keyword)
            throws DatabaseException {

        String sql = """
                SELECT book_id, title, author, isbn, category,
                       is_reference, publisher, publication_year,
                       total_copies, available_copies, description
                FROM books
                WHERE title LIKE ?
                   OR author LIKE ?
                   OR isbn LIKE ?
                   OR category LIKE ?
                ORDER BY title
                """;

        List<LibraryItem> books = new ArrayList<>();

        String searchValue = "%" + keyword + "%";

        try (Connection connection = dbConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, searchValue);
            statement.setString(2, searchValue);
            statement.setString(3, searchValue);
            statement.setString(4, searchValue);

            try (ResultSet resultSet = statement.executeQuery()) {

                while (resultSet.next()) {
                    books.add(mapResultSet(resultSet));
                }
            }

            return books;

        } catch (SQLException e) {
            throw new DatabaseException(
                    "Unable to search books.", e);
        }
    }

    /**
     * Converts one database row into a LibraryItem object.
     *
     * Polymorphism is used because the DAO returns the parent type
     * LibraryItem while the actual object is a Book.
     */
    private LibraryItem mapResultSet(ResultSet resultSet)
            throws SQLException {

        int itemId = resultSet.getInt("book_id");
        String title = resultSet.getString("title");
        String author = resultSet.getString("author");
        String isbn = resultSet.getString("isbn");

        ItemCategory category =
                ItemCategory.valueOf(
                        resultSet.getString("category").toUpperCase());

        boolean reference = resultSet.getBoolean("is_reference");

        int totalCopies = resultSet.getInt("total_copies");
        int availableCopies = resultSet.getInt("available_copies");

        LibraryItem item = ItemFactory.createItem(
                reference ? "REFERENCE_BOOK" : "BOOK",
                itemId,
                title,
                author,
                isbn,
                category,
                totalCopies,
                availableCopies);

        item.setPublisher(resultSet.getString("publisher"));
        item.setPublicationYear(resultSet.getInt("publication_year"));
        item.setDescription(resultSet.getString("description"));
        return item;
    }
}
