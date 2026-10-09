package library.management.system.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import library.management.system.db.DBConnection;
import library.management.system.exception.DatabaseException;
import library.management.system.exception.BookNotAvailableException;
import library.management.system.exception.InvalidInputException;
import library.management.system.exception.ItemNotBorrowableException;
import library.management.system.exception.LibraryException;
import library.management.system.exception.BorrowLimitExceededException;
import library.management.system.exception.FinePendingException;
import library.management.system.exception.MemberBlockedException;
import library.management.system.model.Issue;
import library.management.system.model.IssueStatus;

public class IssueDAO implements GenericDAO<Issue, Long> {

    private final DBConnection dbConnection;

    public IssueDAO() {
        this.dbConnection = DBConnection.getInstance();
    }

    /** Atomically creates an issue and decrements inventory. */
    public Issue issueAtomically(
            String memberId,
            long bookId,
            long librarianId,
            LocalDate issueDate,
            LocalDate dueDate,
            int maximumActiveIssues)
            throws LibraryException {
        String bookSql = """
                SELECT available_copies, is_reference
                FROM books WHERE book_id = ? FOR UPDATE
                """;
        String duplicateSql = """
                SELECT 1 FROM issues
                WHERE member_id = ? AND book_id = ?
                  AND status IN ('ISSUED', 'OVERDUE')
                LIMIT 1 FOR UPDATE
                """;
        String issueSql = """
                INSERT INTO issues
                    (member_id, book_id, issue_date, due_date, status, issued_by)
                VALUES (?, ?, ?, ?, 'ISSUED', ?)
                """;
        try (Connection connection = dbConnection.getConnection()) {
            connection.setAutoCommit(false);
            try {
                String memberLockSql = """
                        SELECT u.status
                        FROM members m JOIN users u ON u.user_id = m.user_id
                        WHERE m.member_id = ? FOR UPDATE
                        """;
                String countSql = """
                        SELECT COUNT(*) FROM issues
                        WHERE member_id = ? AND status IN ('ISSUED', 'OVERDUE')
                        """;
                String finesSql = """
                        SELECT COALESCE(SUM(amount), 0) FROM fines
                        WHERE member_id = ? AND status = 'UNPAID'
                        """;
                try (PreparedStatement statement = connection.prepareStatement(memberLockSql)) {
                    statement.setString(1, memberId);
                    try (ResultSet result = statement.executeQuery()) {
                        if (!result.next()) {
                            throw new InvalidInputException("Member not found: " + memberId);
                        }
                        if (!"ACTIVE".equalsIgnoreCase(result.getString("status"))) {
                            throw new MemberBlockedException(
                                    "Only active members can borrow books.");
                        }
                    }
                }
                try (PreparedStatement statement = connection.prepareStatement(countSql)) {
                    statement.setString(1, memberId);
                    try (ResultSet result = statement.executeQuery()) {
                        if (result.next() && result.getInt(1) >= maximumActiveIssues) {
                            throw new BorrowLimitExceededException(
                                    "Borrowing limit reached. Maximum allowed: "
                                            + maximumActiveIssues);
                        }
                    }
                }
                try (PreparedStatement statement = connection.prepareStatement(finesSql)) {
                    statement.setString(1, memberId);
                    try (ResultSet result = statement.executeQuery()) {
                        if (result.next() && result.getDouble(1) > 100.0) {
                            throw new FinePendingException(
                                    "Borrowing blocked because unpaid fines exceed Rs 100.");
                        }
                    }
                }

                int available;
                try (PreparedStatement statement = connection.prepareStatement(bookSql)) {
                    statement.setLong(1, bookId);
                    try (ResultSet result = statement.executeQuery()) {
                        if (!result.next()) {
                            throw new InvalidInputException("Book not found: " + bookId);
                        }
                        if (result.getBoolean("is_reference")) {
                            throw new ItemNotBorrowableException(
                                    "Reference books cannot be issued.");
                        }
                        available = result.getInt("available_copies");
                    }
                }
                Long readyReservationId = null;
                try (PreparedStatement statement = connection.prepareStatement("""
                        SELECT reservation_id
                        FROM reservations
                        WHERE member_id = ? AND book_id = ? AND status = 'READY'
                        ORDER BY reservation_date, reservation_id
                        LIMIT 1 FOR UPDATE
                        """)) {
                    statement.setString(1, memberId);
                    statement.setLong(2, bookId);
                    try (ResultSet result = statement.executeQuery()) {
                        if (result.next()) {
                            readyReservationId = result.getLong("reservation_id");
                        }
                    }
                }
                if (available <= 0 && readyReservationId == null) {
                    throw new BookNotAvailableException(
                            "No copy of this book is currently available.");
                }
                try (PreparedStatement statement = connection.prepareStatement(duplicateSql)) {
                    statement.setString(1, memberId);
                    statement.setLong(2, bookId);
                    try (ResultSet result = statement.executeQuery()) {
                        if (result.next()) {
                            throw new InvalidInputException(
                                    "This member already has this book issued.");
                        }
                    }
                }
                long issueId;
                try (PreparedStatement statement = connection.prepareStatement(
                        issueSql, Statement.RETURN_GENERATED_KEYS)) {
                    statement.setString(1, memberId);
                    statement.setLong(2, bookId);
                    statement.setDate(3, java.sql.Date.valueOf(issueDate));
                    statement.setDate(4, java.sql.Date.valueOf(dueDate));
                    statement.setLong(5, librarianId);
                    statement.executeUpdate();
                    try (ResultSet keys = statement.getGeneratedKeys()) {
                        if (!keys.next()) {
                            throw new SQLException("Could not retrieve issue ID.");
                        }
                        issueId = keys.getLong(1);
                    }
                }
                if (readyReservationId != null) {
                    try (PreparedStatement statement = connection.prepareStatement("""
                            UPDATE reservations SET status = 'FULFILLED'
                            WHERE reservation_id = ? AND status = 'READY'
                            """)) {
                        statement.setLong(1, readyReservationId);
                        if (statement.executeUpdate() != 1) {
                            throw new BookNotAvailableException(
                                    "The held reservation is no longer available.");
                        }
                    }
                } else {
                    try (PreparedStatement statement = connection.prepareStatement(
                            "UPDATE books SET available_copies = available_copies - 1 "
                            + "WHERE book_id = ? AND available_copies > 0")) {
                        statement.setLong(1, bookId);
                        if (statement.executeUpdate() != 1) {
                            throw new BookNotAvailableException(
                                    "Book is no longer available.");
                        }
                    }
                }
                connection.commit();
                return new Issue(issueId, memberId, bookId, issueDate, dueDate,
                        null, IssueStatus.ISSUED, librarianId, null);
            } catch (LibraryException | SQLException ex) {
                connection.rollback();
                throw ex;
            }
        } catch (LibraryException ex) {
            throw ex;
        } catch (SQLException ex) {
            throw new DatabaseException("Could not issue the book.", ex);
        }
    }

    /** Atomically closes an issue, restores inventory, and creates its fine. */
    public ReturnData returnAtomically(
            long issueId,
            long librarianId,
            LocalDate returnDate,
            double finePerDay)
            throws LibraryException {
        try (Connection connection = dbConnection.getConnection()) {
            connection.setAutoCommit(false);
            try {
                String issueSql = """
                        SELECT issue_id, member_id, book_id, issue_date, due_date,
                               return_date, status, issued_by, returned_by
                        FROM issues WHERE issue_id = ? FOR UPDATE
                        """;
                Issue issue;
                try (PreparedStatement statement = connection.prepareStatement(issueSql)) {
                    statement.setLong(1, issueId);
                    try (ResultSet result = statement.executeQuery()) {
                        if (!result.next()) {
                            throw new InvalidInputException("Issue record not found.");
                        }
                        issue = mapResultSet(result);
                    }
                }
                if (issue.getStatus() == IssueStatus.RETURNED) {
                    throw new InvalidInputException(
                            "This book has already been returned.");
                }
                try (PreparedStatement statement = connection.prepareStatement(
                        "SELECT book_id FROM books WHERE book_id = ? FOR UPDATE")) {
                    statement.setLong(1, issue.getBookId());
                    try (ResultSet result = statement.executeQuery()) {
                        if (!result.next()) {
                            throw new InvalidInputException(
                                    "Book inventory record was not found.");
                        }
                    }
                }
                int daysLate = (int) Math.max(0,
                        ChronoUnit.DAYS.between(issue.getDueDate(), returnDate));
                double fineAmount = daysLate * finePerDay;
                try (PreparedStatement statement = connection.prepareStatement("""
                        UPDATE issues SET return_date = ?, status = 'RETURNED', returned_by = ?
                        WHERE issue_id = ? AND status IN ('ISSUED', 'OVERDUE')
                        """)) {
                    statement.setDate(1, java.sql.Date.valueOf(returnDate));
                    statement.setLong(2, librarianId);
                    statement.setLong(3, issueId);
                    if (statement.executeUpdate() != 1) {
                        throw new InvalidInputException("Book could not be returned.");
                    }
                }
                try (PreparedStatement statement = connection.prepareStatement("""
                        UPDATE books SET available_copies = LEAST(total_copies, available_copies + 1)
                        WHERE book_id = ?
                        """)) {
                    statement.setLong(1, issue.getBookId());
                    if (statement.executeUpdate() != 1) {
                        throw new InvalidInputException(
                                "Book inventory could not be updated.");
                    }
                }
                String reservedForMember = holdNextWaitingReservation(
                        connection, issue.getBookId());
                if (daysLate > 0 && fineAmount > 0) {
                    try (PreparedStatement statement = connection.prepareStatement("""
                            INSERT INTO fines (issue_id, member_id, days_late, amount, status)
                            VALUES (?, ?, ?, ?, 'UNPAID')
                            ON DUPLICATE KEY UPDATE
                                days_late = IF(status = 'UNPAID', VALUES(days_late), days_late),
                                amount = IF(status = 'UNPAID', VALUES(amount), amount)
                            """)) {
                        statement.setLong(1, issueId);
                        statement.setString(2, issue.getMemberId());
                        statement.setInt(3, daysLate);
                        statement.setDouble(4, fineAmount);
                        statement.executeUpdate();
                    }
                }
                connection.commit();
                return new ReturnData(issue.getMemberId(), issue.getBookId(),
                        returnDate, daysLate, fineAmount, reservedForMember);
            } catch (LibraryException | SQLException ex) {
                connection.rollback();
                throw ex;
            }
        } catch (LibraryException ex) {
            throw ex;
        } catch (SQLException ex) {
            throw new DatabaseException("Could not return the book.", ex);
        }
    }

    /** Moves one just-returned general copy into the oldest waiter's hold. */
    private String holdNextWaitingReservation(
            Connection connection,
            long bookId)
            throws SQLException {
        String memberId = null;
        long reservationId = 0;
        try (PreparedStatement statement = connection.prepareStatement("""
                SELECT reservation_id, member_id
                FROM reservations
                WHERE book_id = ? AND status = 'WAITING'
                ORDER BY reservation_date, reservation_id
                LIMIT 1 FOR UPDATE
                """)) {
            statement.setLong(1, bookId);
            try (ResultSet result = statement.executeQuery()) {
                if (result.next()) {
                    reservationId = result.getLong("reservation_id");
                    memberId = result.getString("member_id");
                }
            }
        }
        if (memberId == null) {
            return null;
        }
        try (PreparedStatement statement = connection.prepareStatement("""
                UPDATE reservations SET status = 'READY'
                WHERE reservation_id = ? AND status = 'WAITING'
                """)) {
            statement.setLong(1, reservationId);
            if (statement.executeUpdate() != 1) {
                throw new SQLException("Could not hold the returned copy for the next member.");
            }
        }
        try (PreparedStatement statement = connection.prepareStatement("""
                UPDATE books SET available_copies = available_copies - 1
                WHERE book_id = ? AND available_copies > 0
                """)) {
            statement.setLong(1, bookId);
            if (statement.executeUpdate() != 1) {
                throw new SQLException("Could not remove the held copy from general availability.");
            }
        }
        return memberId;
    }

    public int refreshOverdueStatuses() throws DatabaseException {
        String sql = "UPDATE issues SET status = 'OVERDUE' "
                + "WHERE status = 'ISSUED' AND due_date < CURDATE()";
        try (Connection connection = dbConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            return statement.executeUpdate();
        } catch (SQLException ex) {
            throw new DatabaseException(
                    "Could not refresh overdue issue statuses.", ex);
        }
    }

    public record ReturnData(
            String memberId,
            long bookId,
            LocalDate returnDate,
            int daysLate,
            double fineAmount,
            String reservedForMemberId) { }

    @Override
    public Optional<Issue> findById(Long issueId)
            throws DatabaseException {

        if (issueId == null || issueId <= 0) {
            return Optional.empty();
        }

        String sql = """
                SELECT issue_id,
                       member_id,
                       book_id,
                       issue_date,
                       due_date,
                       return_date,
                       status,
                       issued_by,
                       returned_by
                FROM issues
                WHERE issue_id = ?
                """;

        try (Connection connection = dbConnection.getConnection();
             PreparedStatement statement =
                     connection.prepareStatement(sql)) {

            statement.setLong(1, issueId);

            try (ResultSet resultSet = statement.executeQuery()) {

                if (resultSet.next()) {
                    return Optional.of(mapResultSet(resultSet));
                }

                return Optional.empty();
            }

        } catch (SQLException e) {

            throw new DatabaseException(
                    "Could not find issue with ID: " + issueId,
                    e
            );
        }
    }

    @Override
    public List<Issue> findAll()
            throws DatabaseException {

        String sql = """
                SELECT issue_id,
                       member_id,
                       book_id,
                       issue_date,
                       due_date,
                       return_date,
                       status,
                       issued_by,
                       returned_by
                FROM issues
                ORDER BY issue_date DESC
                """;

        List<Issue> issues = new ArrayList<>();

        try (Connection connection = dbConnection.getConnection();
             PreparedStatement statement =
                     connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {

            while (resultSet.next()) {
                issues.add(mapResultSet(resultSet));
            }

            return issues;

        } catch (SQLException e) {

            throw new DatabaseException(
                    "Could not retrieve issues.",
                    e
            );
        }
    }

    @Override
    public Issue save(Issue issue)
            throws DatabaseException {

        if (issue == null) {
            throw new DatabaseException(
                    "Issue cannot be null."
            );
        }

        String sql = """
                INSERT INTO issues
                (member_id, book_id, issue_date, due_date,
                 return_date, status, issued_by, returned_by)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                """;

        try (Connection connection = dbConnection.getConnection();
             PreparedStatement statement =
                     connection.prepareStatement(
                             sql,
                             Statement.RETURN_GENERATED_KEYS)) {

            statement.setString(1, issue.getMemberId());
            statement.setLong(2, issue.getBookId());
            statement.setDate(
                    3,
                    java.sql.Date.valueOf(issue.getIssueDate())
            );
            statement.setDate(
                    4,
                    java.sql.Date.valueOf(issue.getDueDate())
            );

            if (issue.getReturnDate() != null) {
                statement.setDate(
                        5,
                        java.sql.Date.valueOf(issue.getReturnDate())
                );
            } else {
                statement.setNull(5, Types.DATE);
            }

            statement.setString(
                    6,
                    issue.getStatus().name()
            );

            statement.setLong(
                    7,
                    issue.getIssuedBy()
            );

            if (issue.getReturnedBy() != null) {
                statement.setLong(
                        8,
                        issue.getReturnedBy()
                );
            } else {
                statement.setNull(8, Types.BIGINT);
            }

            statement.executeUpdate();

            try (ResultSet keys =
                         statement.getGeneratedKeys()) {

                if (keys.next()) {
                    issue.setIssueId(
                            keys.getLong(1)
                    );
                }
            }

            return issue;

        } catch (SQLException e) {

            throw new DatabaseException(
                    "Could not save issue.",
                    e
            );
        }
    }

    @Override
    public boolean update(Issue issue)
            throws DatabaseException {

        if (issue == null || issue.getIssueId() <= 0) {
            return false;
        }

        String sql = """
                UPDATE issues
                SET member_id = ?,
                    book_id = ?,
                    issue_date = ?,
                    due_date = ?,
                    return_date = ?,
                    status = ?,
                    issued_by = ?,
                    returned_by = ?
                WHERE issue_id = ?
                """;

        try (Connection connection = dbConnection.getConnection();
             PreparedStatement statement =
                     connection.prepareStatement(sql)) {

            statement.setString(1, issue.getMemberId());
            statement.setLong(2, issue.getBookId());
            statement.setDate(
                    3,
                    java.sql.Date.valueOf(issue.getIssueDate())
            );
            statement.setDate(
                    4,
                    java.sql.Date.valueOf(issue.getDueDate())
            );

            if (issue.getReturnDate() != null) {
                statement.setDate(
                        5,
                        java.sql.Date.valueOf(issue.getReturnDate())
                );
            } else {
                statement.setNull(5, Types.DATE);
            }

            statement.setString(
                    6,
                    issue.getStatus().name()
            );

            statement.setLong(
                    7,
                    issue.getIssuedBy()
            );

            if (issue.getReturnedBy() != null) {
                statement.setLong(
                        8,
                        issue.getReturnedBy()
                );
            } else {
                statement.setNull(8, Types.BIGINT);
            }

            statement.setLong(
                    9,
                    issue.getIssueId()
            );

            return statement.executeUpdate() > 0;

        } catch (SQLException e) {

            throw new DatabaseException(
                    "Could not update issue.",
                    e
            );
        }
    }

    @Override
    public boolean delete(Long issueId)
            throws DatabaseException {

        if (issueId == null || issueId <= 0) {
            return false;
        }

        String sql = """
                DELETE FROM issues
                WHERE issue_id = ?
                """;

        try (Connection connection = dbConnection.getConnection();
             PreparedStatement statement =
                     connection.prepareStatement(sql)) {

            statement.setLong(1, issueId);

            return statement.executeUpdate() > 0;

        } catch (SQLException e) {

            throw new DatabaseException(
                    "Could not delete issue.",
                    e
            );
        }
    }

    /**
     * Finds all active issues for a member.
     */
    public List<Issue> findActiveByMemberId(
            String memberId)
            throws DatabaseException {

        String sql = """
                SELECT issue_id,
                       member_id,
                       book_id,
                       issue_date,
                       due_date,
                       return_date,
                       status,
                       issued_by,
                       returned_by
                FROM issues
                WHERE member_id = ?
                  AND status IN ('ISSUED', 'OVERDUE')
                ORDER BY due_date ASC
                """;

        return findByStringParameter(
                sql,
                memberId
        );
    }

    /**
     * Finds complete borrowing history for a member.
     */
    public List<Issue> findByMemberId(
            String memberId)
            throws DatabaseException {

        String sql = """
                SELECT issue_id,
                       member_id,
                       book_id,
                       issue_date,
                       due_date,
                       return_date,
                       status,
                       issued_by,
                       returned_by
                FROM issues
                WHERE member_id = ?
                ORDER BY issue_date DESC
                """;

        return findByStringParameter(
                sql,
                memberId
        );
    }

    /**
     * Finds active issues for a particular book.
     */
    public List<Issue> findActiveByBookId(
            long bookId)
            throws DatabaseException {

        String sql = """
                SELECT issue_id,
                       member_id,
                       book_id,
                       issue_date,
                       due_date,
                       return_date,
                       status,
                       issued_by,
                       returned_by
                FROM issues
                WHERE book_id = ?
                  AND status IN ('ISSUED', 'OVERDUE')
                ORDER BY issue_date ASC
                """;

        List<Issue> issues = new ArrayList<>();

        try (Connection connection = dbConnection.getConnection();
             PreparedStatement statement =
                     connection.prepareStatement(sql)) {

            statement.setLong(1, bookId);

            try (ResultSet resultSet =
                         statement.executeQuery()) {

                while (resultSet.next()) {
                    issues.add(
                            mapResultSet(resultSet)
                    );
                }
            }

            return issues;

        } catch (SQLException e) {

            throw new DatabaseException(
                    "Could not retrieve active issues for book.",
                    e
            );
        }
    }

    /**
     * Checks whether a member already has this book issued.
     */
    public boolean hasActiveIssue(
            String memberId,
            long bookId)
            throws DatabaseException {

        String sql = """
                SELECT 1
                FROM issues
                WHERE member_id = ?
                  AND book_id = ?
                  AND status IN ('ISSUED', 'OVERDUE')
                LIMIT 1
                """;

        try (Connection connection = dbConnection.getConnection();
             PreparedStatement statement =
                     connection.prepareStatement(sql)) {

            statement.setString(1, memberId);
            statement.setLong(2, bookId);

            try (ResultSet resultSet =
                         statement.executeQuery()) {

                return resultSet.next();
            }

        } catch (SQLException e) {

            throw new DatabaseException(
                    "Could not check existing issue.",
                    e
            );
        }
    }

    /**
     * Counts active books currently borrowed by a member.
     */
    public int countActiveByMemberId(
            String memberId)
            throws DatabaseException {

        String sql = """
                SELECT COUNT(*)
                FROM issues
                WHERE member_id = ?
                  AND status IN ('ISSUED', 'OVERDUE')
                """;

        try (Connection connection = dbConnection.getConnection();
             PreparedStatement statement =
                     connection.prepareStatement(sql)) {

            statement.setString(1, memberId);

            try (ResultSet resultSet =
                         statement.executeQuery()) {

                if (resultSet.next()) {
                    return resultSet.getInt(1);
                }

                return 0;
            }

        } catch (SQLException e) {

            throw new DatabaseException(
                    "Could not count active member issues.",
                    e
            );
        }
    }

    /**
     * Finds all overdue issues.
     */
    public List<Issue> findOverdueIssues()
            throws DatabaseException {

        String sql = """
                SELECT issue_id,
                       member_id,
                       book_id,
                       issue_date,
                       due_date,
                       return_date,
                       status,
                       issued_by,
                       returned_by
                FROM issues
                WHERE status IN ('ISSUED', 'OVERDUE')
                  AND due_date < CURDATE()
                ORDER BY due_date ASC
                """;

        List<Issue> issues = new ArrayList<>();

        try (Connection connection = dbConnection.getConnection();
             PreparedStatement statement =
                     connection.prepareStatement(sql);
             ResultSet resultSet =
                     statement.executeQuery()) {

            while (resultSet.next()) {
                issues.add(
                        mapResultSet(resultSet)
                );
            }

            return issues;

        } catch (SQLException e) {

            throw new DatabaseException(
                    "Could not retrieve overdue issues.",
                    e
            );
        }
    }

    /**
     * Updates an issue status.
     */
    public boolean updateStatus(
            long issueId,
            IssueStatus status)
            throws DatabaseException {

        if (issueId <= 0 || status == null) {
            return false;
        }

        String sql = """
                UPDATE issues
                SET status = ?
                WHERE issue_id = ?
                """;

        try (Connection connection = dbConnection.getConnection();
             PreparedStatement statement =
                     connection.prepareStatement(sql)) {

            statement.setString(1, status.name());
            statement.setLong(2, issueId);

            return statement.executeUpdate() > 0;

        } catch (SQLException e) {

            throw new DatabaseException(
                    "Could not update issue status.",
                    e
            );
        }
    }

    /**
     * Marks an issue as returned.
     *
     * This method is mainly useful for direct DAO operations.
     * Full return transaction will be handled by IssueService.
     */
    public boolean markReturned(
            long issueId,
            LocalDate returnDate,
            long returnedBy)
            throws DatabaseException {

        String sql = """
                UPDATE issues
                SET return_date = ?,
                    status = 'RETURNED',
                    returned_by = ?
                WHERE issue_id = ?
                  AND status IN ('ISSUED', 'OVERDUE')
                """;

        try (Connection connection = dbConnection.getConnection();
             PreparedStatement statement =
                     connection.prepareStatement(sql)) {

            statement.setDate(
                    1,
                    java.sql.Date.valueOf(returnDate)
            );
            statement.setLong(2, returnedBy);
            statement.setLong(3, issueId);

            return statement.executeUpdate() > 0;

        } catch (SQLException e) {

            throw new DatabaseException(
                    "Could not mark issue as returned.",
                    e
            );
        }
    }

    /**
     * Finds one active issue for a member and book.
     */
    public Optional<Issue> findActiveIssue(
            String memberId,
            long bookId)
            throws DatabaseException {

        String sql = """
                SELECT issue_id,
                       member_id,
                       book_id,
                       issue_date,
                       due_date,
                       return_date,
                       status,
                       issued_by,
                       returned_by
                FROM issues
                WHERE member_id = ?
                  AND book_id = ?
                  AND status IN ('ISSUED', 'OVERDUE')
                LIMIT 1
                """;

        try (Connection connection = dbConnection.getConnection();
             PreparedStatement statement =
                     connection.prepareStatement(sql)) {

            statement.setString(1, memberId);
            statement.setLong(2, bookId);

            try (ResultSet resultSet =
                         statement.executeQuery()) {

                if (resultSet.next()) {
                    return Optional.of(
                            mapResultSet(resultSet)
                    );
                }

                return Optional.empty();
            }

        } catch (SQLException e) {

            throw new DatabaseException(
                    "Could not find active issue.",
                    e
            );
        }
    }

    private List<Issue> findByStringParameter(
            String sql,
            String value)
            throws DatabaseException {

        List<Issue> issues = new ArrayList<>();

        try (Connection connection = dbConnection.getConnection();
             PreparedStatement statement =
                     connection.prepareStatement(sql)) {

            statement.setString(1, value);

            try (ResultSet resultSet =
                         statement.executeQuery()) {

                while (resultSet.next()) {
                    issues.add(
                            mapResultSet(resultSet)
                    );
                }
            }

            return issues;

        } catch (SQLException e) {

            throw new DatabaseException(
                    "Could not retrieve issue records.",
                    e
            );
        }
    }

    private Issue mapResultSet(
            ResultSet resultSet)
            throws SQLException {

        long issueId =
                resultSet.getLong("issue_id");

        String memberId =
                resultSet.getString("member_id");

        long bookId =
                resultSet.getLong("book_id");

        LocalDate issueDate =
                resultSet.getDate("issue_date")
                        .toLocalDate();

        LocalDate dueDate =
                resultSet.getDate("due_date")
                        .toLocalDate();

        LocalDate returnDate = null;

        if (resultSet.getDate("return_date") != null) {
            returnDate =
                    resultSet.getDate("return_date")
                            .toLocalDate();
        }

        IssueStatus status =
                IssueStatus.valueOf(
                        resultSet.getString("status")
                );

        long issuedBy =
                resultSet.getLong("issued_by");

        Long returnedBy = null;

        long returnedByValue =
                resultSet.getLong("returned_by");

        if (!resultSet.wasNull()) {
            returnedBy = returnedByValue;
        }

        return new Issue(
                issueId,
                memberId,
                bookId,
                issueDate,
                dueDate,
                returnDate,
                status,
                issuedBy,
                returnedBy
        );
    }
}
