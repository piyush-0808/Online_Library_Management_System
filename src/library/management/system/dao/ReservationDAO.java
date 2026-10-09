package library.management.system.dao;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import library.management.system.db.DBConnection;
import library.management.system.db.DatabaseTransaction;
import library.management.system.exception.DatabaseException;
import library.management.system.exception.InvalidInputException;
import library.management.system.exception.LibraryException;
import library.management.system.model.Reservation;

public class ReservationDAO
        implements GenericDAO<Reservation, Long> {

    private final DBConnection dbConnection;

    public ReservationDAO() {
        this.dbConnection = DBConnection.getInstance();
    }

    /** Serialize reservation requests for a member and book before insert. */
    public Reservation createWaitingAtomically(Reservation reservation)
            throws LibraryException {
        try (Connection connection = dbConnection.getConnection()) {
            connection.setAutoCommit(false);
            try {
                try (PreparedStatement statement = connection.prepareStatement(
                        "SELECT member_id FROM members WHERE member_id = ? FOR UPDATE")) {
                    statement.setString(1, reservation.getMemberId());
                    try (ResultSet result = statement.executeQuery()) {
                        if (!result.next()) {
                            throw new InvalidInputException("Member not found.");
                        }
                    }
                }

                int availableCopies;
                try (PreparedStatement statement = connection.prepareStatement(
                        "SELECT available_copies FROM books WHERE book_id = ? FOR UPDATE")) {
                    statement.setLong(1, reservation.getBookId());
                    try (ResultSet result = statement.executeQuery()) {
                        if (!result.next()) {
                            throw new InvalidInputException("Book not found.");
                        }
                        availableCopies = result.getInt("available_copies");
                    }
                }
                if (availableCopies > 0) {
                    throw new InvalidInputException(
                            "A copy is available; a reservation is not needed.");
                }

                try (PreparedStatement statement = connection.prepareStatement("""
                        SELECT 1 FROM reservations
                        WHERE member_id = ? AND book_id = ? AND status IN ('WAITING', 'READY')
                        LIMIT 1 FOR UPDATE
                        """)) {
                    statement.setString(1, reservation.getMemberId());
                    statement.setLong(2, reservation.getBookId());
                    try (ResultSet result = statement.executeQuery()) {
                        if (result.next()) {
                            throw new InvalidInputException(
                                    "You already have a waiting reservation for this book.");
                        }
                    }
                }

                try (PreparedStatement statement = connection.prepareStatement("""
                        INSERT INTO reservations (member_id, book_id, status)
                        VALUES (?, ?, 'WAITING')
                        """, Statement.RETURN_GENERATED_KEYS)) {
                    statement.setString(1, reservation.getMemberId());
                    statement.setLong(2, reservation.getBookId());
                    statement.executeUpdate();
                    try (ResultSet keys = statement.getGeneratedKeys()) {
                        if (!keys.next()) {
                            throw new SQLException(
                                    "Could not retrieve reservation ID.");
                        }
                        reservation.setReservationId(keys.getLong(1));
                    }
                }
                connection.commit();
                return reservation;
            } catch (LibraryException | SQLException ex) {
                connection.rollback();
                throw ex;
            }
        } catch (LibraryException ex) {
            throw ex;
        } catch (SQLException ex) {
            throw new DatabaseException("Could not create reservation.", ex);
        }
    }

    public Optional<Reservation> fulfillNextAtomically(long bookId)
            throws DatabaseException {
        try (Connection connection = dbConnection.getConnection()) {
            return DatabaseTransaction.execute(connection, transaction -> {
                int availableCopies;
                try (PreparedStatement statement = transaction.prepareStatement(
                        "SELECT available_copies FROM books WHERE book_id = ? FOR UPDATE")) {
                    statement.setLong(1, bookId);
                    try (ResultSet result = statement.executeQuery()) {
                        if (!result.next()) {
                            throw new SQLException("Book not found.");
                        }
                        availableCopies = result.getInt("available_copies");
                    }
                }
                if (availableCopies <= 0) {
                    return Optional.empty();
                }
                Reservation next = null;
                String selectSql = """
                        SELECT reservation_id, member_id, book_id,
                               reservation_date, status
                        FROM reservations
                        WHERE book_id = ? AND status = 'WAITING'
                        ORDER BY reservation_date, reservation_id
                        LIMIT 1 FOR UPDATE
                        """;
                try (PreparedStatement statement = transaction.prepareStatement(selectSql)) {
                    statement.setLong(1, bookId);
                    try (ResultSet result = statement.executeQuery()) {
                        if (result.next()) {
                            next = mapResultSet(result);
                        }
                    }
                }
                if (next == null) {
                    return Optional.empty();
                }
                try (PreparedStatement statement = transaction.prepareStatement("""
                        UPDATE reservations SET status = 'READY'
                        WHERE reservation_id = ? AND status = 'WAITING'
                        """)) {
                    statement.setLong(1, next.getReservationId());
                    if (statement.executeUpdate() != 1) {
                        return Optional.empty();
                    }
                }
                try (PreparedStatement statement = transaction.prepareStatement("""
                        UPDATE books SET available_copies = available_copies - 1
                        WHERE book_id = ? AND available_copies > 0
                        """)) {
                    statement.setLong(1, bookId);
                    if (statement.executeUpdate() != 1) {
                        throw new SQLException("Could not hold an available copy.");
                    }
                }
                next.setStatus("READY");
                return Optional.of(next);
            });
        } catch (SQLException ex) {
            throw new DatabaseException("Could not fulfill reservation.", ex);
        }
    }

    @Override
    public Optional<Reservation> findById(Long reservationId)
            throws DatabaseException {

        String sql = """
                SELECT reservation_id,
                       member_id,
                       book_id,
                       reservation_date,
                       status
                FROM reservations
                WHERE reservation_id = ?
                """;

        try (Connection connection = dbConnection.getConnection();
             PreparedStatement statement =
                     connection.prepareStatement(sql)) {

            statement.setLong(1, reservationId);

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
                    "Could not find reservation.",
                    e
            );
        }
    }

    @Override
    public List<Reservation> findAll()
            throws DatabaseException {

        String sql = """
                SELECT reservation_id,
                       member_id,
                       book_id,
                       reservation_date,
                       status
                FROM reservations
                ORDER BY reservation_date ASC,
                         reservation_id ASC
                """;

        List<Reservation> reservations =
                new ArrayList<>();

        try (Connection connection = dbConnection.getConnection();
             PreparedStatement statement =
                     connection.prepareStatement(sql);
             ResultSet resultSet =
                     statement.executeQuery()) {

            while (resultSet.next()) {
                reservations.add(
                        mapResultSet(resultSet)
                );
            }

            return reservations;

        } catch (SQLException e) {
            throw new DatabaseException(
                    "Could not retrieve reservations.",
                    e
            );
        }
    }

    @Override
    public Reservation save(
            Reservation reservation)
            throws DatabaseException {

        String sql = """
                INSERT INTO reservations
                (member_id, book_id, reservation_date, status)
                VALUES (?, ?, ?, ?)
                """;

        try (Connection connection = dbConnection.getConnection();
             PreparedStatement statement =
                     connection.prepareStatement(
                             sql,
                             Statement.RETURN_GENERATED_KEYS)) {

            statement.setString(
                    1,
                    reservation.getMemberId()
            );

            statement.setLong(
                    2,
                    reservation.getBookId()
            );

            LocalDateValue date =
                    new LocalDateValue(
                            reservation.getReservationDate()
                    );

            statement.setDate(
                    3,
                    date.toSqlDate()
            );

            statement.setString(
                    4,
                    reservation.getStatus()
            );

            statement.executeUpdate();

            try (ResultSet keys =
                         statement.getGeneratedKeys()) {

                if (keys.next()) {
                    reservation.setReservationId(
                            keys.getLong(1)
                    );
                }
            }

            return reservation;

        } catch (SQLException e) {
            throw new DatabaseException(
                    "Could not save reservation.",
                    e
            );
        }
    }

    @Override
    public boolean update(
            Reservation reservation)
            throws DatabaseException {

        String sql = """
                UPDATE reservations
                SET status = ?
                WHERE reservation_id = ?
                """;

        try (Connection connection = dbConnection.getConnection();
             PreparedStatement statement =
                     connection.prepareStatement(sql)) {

            statement.setString(
                    1,
                    reservation.getStatus()
            );

            statement.setLong(
                    2,
                    reservation.getReservationId()
            );

            return statement.executeUpdate() > 0;

        } catch (SQLException e) {
            throw new DatabaseException(
                    "Could not update reservation.",
                    e
            );
        }
    }

    @Override
    public boolean delete(Long reservationId)
            throws DatabaseException {

        String sql = """
                DELETE FROM reservations
                WHERE reservation_id = ?
                """;

        try (Connection connection = dbConnection.getConnection();
             PreparedStatement statement =
                     connection.prepareStatement(sql)) {

            statement.setLong(1, reservationId);

            return statement.executeUpdate() > 0;

        } catch (SQLException e) {
            throw new DatabaseException(
                    "Could not delete reservation.",
                    e
            );
        }
    }

    /**
     * Returns reservations of a particular member.
     */
    public List<Reservation> findByMemberId(
            String memberId)
            throws DatabaseException {

        String sql = """
                SELECT reservation_id,
                       member_id,
                       book_id,
                       reservation_date,
                       status
                FROM reservations
                WHERE member_id = ?
                ORDER BY reservation_date ASC,
                         reservation_id ASC
                """;

        return findByMember(
                sql,
                memberId
        );
    }

    /**
     * Returns all reservations for a particular book
     * in queue order.
     */
    public List<Reservation> findByBookId(
            long bookId)
            throws DatabaseException {

        String sql = """
                SELECT reservation_id,
                       member_id,
                       book_id,
                       reservation_date,
                       status
                FROM reservations
                WHERE book_id = ?
                  AND status = 'WAITING'
                ORDER BY reservation_date ASC,
                         reservation_id ASC
                """;

        List<Reservation> reservations =
                new ArrayList<>();

        try (Connection connection = dbConnection.getConnection();
             PreparedStatement statement =
                     connection.prepareStatement(sql)) {

            statement.setLong(1, bookId);

            try (ResultSet resultSet =
                         statement.executeQuery()) {

                while (resultSet.next()) {
                    reservations.add(
                            mapResultSet(resultSet)
                    );
                }
            }

            return reservations;

        } catch (SQLException e) {
            throw new DatabaseException(
                    "Could not retrieve book reservations.",
                    e
            );
        }
    }

    /**
     * Returns only waiting reservations of a book.
     */
    public List<Reservation> findWaitingByBookId(
            long bookId)
            throws DatabaseException {

        return findByBookId(bookId);
    }

    /**
     * Finds the first waiting reservation in queue.
     */
    public Optional<Reservation> findFirstWaiting(
            long bookId)
            throws DatabaseException {

        String sql = """
                SELECT reservation_id,
                       member_id,
                       book_id,
                       reservation_date,
                       status
                FROM reservations
                WHERE book_id = ?
                  AND status = 'WAITING'
                ORDER BY reservation_date ASC,
                         reservation_id ASC
                LIMIT 1
                """;

        try (Connection connection = dbConnection.getConnection();
             PreparedStatement statement =
                     connection.prepareStatement(sql)) {

            statement.setLong(1, bookId);

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
                    "Could not find reservation queue.",
                    e
            );
        }
    }

    /**
     * Checks whether a member already has a waiting
     * reservation for the same book.
     */
    public boolean hasWaitingReservation(
            String memberId,
            long bookId)
            throws DatabaseException {

        String sql = """
                SELECT 1
                FROM reservations
                WHERE member_id = ?
                  AND book_id = ?
                  AND status IN ('WAITING', 'READY')
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
                    "Could not check existing reservation.",
                    e
            );
        }
    }

    public boolean hasReadyReservation(String memberId, long bookId)
            throws DatabaseException {
        String sql = """
                SELECT 1 FROM reservations
                WHERE member_id = ? AND book_id = ? AND status = 'READY'
                LIMIT 1
                """;
        try (Connection connection = dbConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, memberId);
            statement.setLong(2, bookId);
            try (ResultSet result = statement.executeQuery()) {
                return result.next();
            }
        } catch (SQLException ex) {
            throw new DatabaseException("Could not check held reservation.", ex);
        }
    }

    /**
     * Updates reservation status.
     */
    public boolean updateStatus(
            long reservationId,
            String status)
            throws DatabaseException {

        String sql = """
                UPDATE reservations
                SET status = ?
                WHERE reservation_id = ?
                """;

        try (Connection connection = dbConnection.getConnection();
             PreparedStatement statement =
                     connection.prepareStatement(sql)) {

            statement.setString(1, status);
            statement.setLong(2, reservationId);

            return statement.executeUpdate() > 0;

        } catch (SQLException e) {
            throw new DatabaseException(
                    "Could not update reservation status.",
                    e
            );
        }
    }

    /**
     * Cancels a reservation.
     */
    public boolean cancel(
            long reservationId)
            throws DatabaseException {
        return cancelAtomically(reservationId, null);
    }

    public boolean cancel(long reservationId, String expectedMemberId)
            throws DatabaseException {
        return cancelAtomically(reservationId, expectedMemberId);
    }

    private boolean cancelAtomically(long reservationId, String expectedMemberId)
            throws DatabaseException {
        try (Connection connection = dbConnection.getConnection()) {
            connection.setAutoCommit(false);
            try {
                long bookId;
                try (PreparedStatement statement = connection.prepareStatement(
                        "SELECT book_id FROM reservations WHERE reservation_id = ?")) {
                    statement.setLong(1, reservationId);
                    try (ResultSet result = statement.executeQuery()) {
                        if (!result.next()) {
                            connection.commit();
                            return false;
                        }
                        bookId = result.getLong("book_id");
                    }
                }
                try (PreparedStatement statement = connection.prepareStatement(
                        "SELECT book_id FROM books WHERE book_id = ? FOR UPDATE")) {
                    statement.setLong(1, bookId);
                    try (ResultSet result = statement.executeQuery()) {
                        if (!result.next()) {
                            throw new SQLException("Book not found.");
                        }
                    }
                }
                String status;
                String memberId;
                try (PreparedStatement statement = connection.prepareStatement("""
                        SELECT status, member_id FROM reservations
                        WHERE reservation_id = ? FOR UPDATE
                        """)) {
                    statement.setLong(1, reservationId);
                    try (ResultSet result = statement.executeQuery()) {
                        if (!result.next()) {
                            connection.commit();
                            return false;
                        }
                        status = result.getString("status");
                        memberId = result.getString("member_id");
                    }
                }
                if (expectedMemberId != null && !expectedMemberId.equals(memberId)) {
                    connection.rollback();
                    return false;
                }
                if (!"WAITING".equals(status) && !"READY".equals(status)) {
                    connection.commit();
                    return false;
                }
                try (PreparedStatement statement = connection.prepareStatement("""
                        UPDATE reservations SET status = 'CANCELLED'
                        WHERE reservation_id = ? AND status IN ('WAITING', 'READY')
                        """)) {
                    statement.setLong(1, reservationId);
                    if (statement.executeUpdate() != 1) {
                        connection.rollback();
                        return false;
                    }
                }
                if ("READY".equals(status)) {
                    if (!holdNextWaitingReservation(connection, bookId)) {
                        try (PreparedStatement statement = connection.prepareStatement("""
                                UPDATE books SET available_copies = LEAST(total_copies, available_copies + 1)
                                WHERE book_id = ?
                                """)) {
                            statement.setLong(1, bookId);
                            statement.executeUpdate();
                        }
                    }
                }
                connection.commit();
                return true;
            } catch (SQLException ex) {
                connection.rollback();
                throw ex;
            }
        } catch (SQLException ex) {
            throw new DatabaseException("Could not cancel reservation.", ex);
        }
    }

    /** Reassigns a held copy to the next waiter without changing inventory. */
    private boolean holdNextWaitingReservation(Connection connection, long bookId)
            throws SQLException {
        long reservationId;
        try (PreparedStatement statement = connection.prepareStatement("""
                SELECT reservation_id FROM reservations
                WHERE book_id = ? AND status = 'WAITING'
                ORDER BY reservation_date, reservation_id
                LIMIT 1 FOR UPDATE
                """)) {
            statement.setLong(1, bookId);
            try (ResultSet result = statement.executeQuery()) {
                if (!result.next()) {
                    return false;
                }
                reservationId = result.getLong("reservation_id");
            }
        }
        try (PreparedStatement statement = connection.prepareStatement("""
                UPDATE reservations SET status = 'READY'
                WHERE reservation_id = ? AND status = 'WAITING'
                """)) {
            statement.setLong(1, reservationId);
            return statement.executeUpdate() == 1;
        }
    }

    /**
     * Marks a reservation as fulfilled.
     */
    public boolean fulfill(
            long reservationId)
            throws DatabaseException {

        return updateStatus(
                reservationId,
                "FULFILLED"
        );
    }

    private List<Reservation> findByMember(
            String sql,
            String memberId)
            throws DatabaseException {

        List<Reservation> reservations =
                new ArrayList<>();

        try (Connection connection = dbConnection.getConnection();
             PreparedStatement statement =
                     connection.prepareStatement(sql)) {

            statement.setString(1, memberId);

            try (ResultSet resultSet =
                         statement.executeQuery()) {

                while (resultSet.next()) {
                    reservations.add(
                            mapResultSet(resultSet)
                    );
                }
            }

            return reservations;

        } catch (SQLException e) {
            throw new DatabaseException(
                    "Could not retrieve member reservations.",
                    e
            );
        }
    }

    private Reservation mapResultSet(
            ResultSet resultSet)
            throws SQLException {

        Date reservationDate =
                resultSet.getDate(
                        "reservation_date"
                );

        return new Reservation(
                resultSet.getLong("reservation_id"),
                resultSet.getString("member_id"),
                resultSet.getLong("book_id"),
                reservationDate != null
                        ? reservationDate.toLocalDate()
                        : null,
                resultSet.getString("status")
        );
    }

    /**
     * Small helper to keep date handling explicit.
     */
    private static final class LocalDateValue {

        private final java.time.LocalDate value;

        private LocalDateValue(
                java.time.LocalDate value) {

            this.value = value;
        }

        private Date toSqlDate() {

            if (value == null) {
                return new Date(
                        System.currentTimeMillis()
                );
            }

            return Date.valueOf(value);
        }
    }
}
