package library.management.system.dao;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import library.management.system.db.DBConnection;
import library.management.system.exception.DatabaseException;
import library.management.system.model.Fine;

public class FineDAO implements GenericDAO<Fine, Long> {

    private final DBConnection dbConnection;

    public FineDAO() {
        this.dbConnection = DBConnection.getInstance();
    }

    @Override
    public Optional<Fine> findById(Long fineId)
            throws DatabaseException {

        String sql = """
                SELECT fine_id,
                       issue_id,
                       member_id,
                       days_late,
                       amount,
                       status,
                       paid_at,
                       paid_by
                FROM fines
                WHERE fine_id = ?
                """;

        try (Connection connection = dbConnection.getConnection();
             PreparedStatement statement =
                     connection.prepareStatement(sql)) {

            statement.setLong(1, fineId);

            try (ResultSet resultSet =
                         statement.executeQuery()) {

                if (resultSet.next()) {
                    return Optional.of(mapResultSet(resultSet));
                }

                return Optional.empty();
            }

        } catch (SQLException e) {
            throw new DatabaseException(
                    "Could not find fine.",
                    e
            );
        }
    }

    @Override
    public List<Fine> findAll()
            throws DatabaseException {

        String sql = """
                SELECT fine_id,
                       issue_id,
                       member_id,
                       days_late,
                       amount,
                       status,
                       paid_at,
                       paid_by
                FROM fines
                ORDER BY fine_id DESC
                """;

        List<Fine> fines = new ArrayList<>();

        try (Connection connection = dbConnection.getConnection();
             PreparedStatement statement =
                     connection.prepareStatement(sql);
             ResultSet resultSet =
                     statement.executeQuery()) {

            while (resultSet.next()) {
                fines.add(mapResultSet(resultSet));
            }

            return fines;

        } catch (SQLException e) {
            throw new DatabaseException(
                    "Could not retrieve fines.",
                    e
            );
        }
    }

    @Override
    public Fine save(Fine fine)
            throws DatabaseException {

        String sql = """
                INSERT INTO fines
                (issue_id, member_id, days_late, amount, status)
                VALUES (?, ?, ?, ?, ?)
                """;

        try (Connection connection = dbConnection.getConnection();
             PreparedStatement statement =
                     connection.prepareStatement(
                             sql,
                             java.sql.Statement.RETURN_GENERATED_KEYS)) {

            statement.setLong(1, fine.getIssueId());
            statement.setString(2, fine.getMemberId());
            statement.setInt(3, fine.getDaysLate());
            statement.setDouble(4, fine.getAmount());
            statement.setString(5, fine.getStatus());

            statement.executeUpdate();

            try (ResultSet keys =
                         statement.getGeneratedKeys()) {

                if (keys.next()) {
                    fine.setFineId(keys.getLong(1));
                }
            }

            return fine;

        } catch (SQLException e) {
            throw new DatabaseException(
                    "Could not save fine.",
                    e
            );
        }
    }

    /** Creates or refreshes an unpaid fine without overwriting a payment. */
    public void upsertOverdueFine(long issueId, String memberId,
            int daysLate, double amount) throws DatabaseException {
        String sql = """
                INSERT INTO fines (issue_id, member_id, days_late, amount, status)
                VALUES (?, ?, ?, ?, 'UNPAID')
                ON DUPLICATE KEY UPDATE
                    days_late = IF(status = 'UNPAID', VALUES(days_late), days_late),
                    amount = IF(status = 'UNPAID', VALUES(amount), amount)
                """;
        try (Connection connection = dbConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, issueId);
            statement.setString(2, memberId);
            statement.setInt(3, daysLate);
            statement.setDouble(4, amount);
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new DatabaseException("Could not refresh overdue fine.", e);
        }
    }

    @Override
    public boolean update(Fine fine)
            throws DatabaseException {

        String sql = """
                UPDATE fines
                SET days_late = ?,
                    amount = ?,
                    status = ?,
                    paid_at = ?,
                    paid_by = ?
                WHERE fine_id = ?
                """;

        try (Connection connection = dbConnection.getConnection();
             PreparedStatement statement =
                     connection.prepareStatement(sql)) {

            statement.setInt(1, fine.getDaysLate());
            statement.setDouble(2, fine.getAmount());
            statement.setString(3, fine.getStatus());

            if (fine.getPaidAt() != null) {
                statement.setDate(
                        4,
                        Date.valueOf(fine.getPaidAt())
                );
            } else {
                statement.setNull(4, java.sql.Types.DATE);
            }

            if (fine.getPaidBy() != null) {
                statement.setLong(
                        5,
                        fine.getPaidBy()
                );
            } else {
                statement.setNull(
                        5,
                        java.sql.Types.BIGINT
                );
            }

            statement.setLong(
                    6,
                    fine.getFineId()
            );

            return statement.executeUpdate() > 0;

        } catch (SQLException e) {
            throw new DatabaseException(
                    "Could not update fine.",
                    e
            );
        }
    }

    @Override
    public boolean delete(Long fineId)
            throws DatabaseException {

        String sql = """
                DELETE FROM fines
                WHERE fine_id = ?
                """;

        try (Connection connection = dbConnection.getConnection();
             PreparedStatement statement =
                     connection.prepareStatement(sql)) {

            statement.setLong(1, fineId);

            return statement.executeUpdate() > 0;

        } catch (SQLException e) {
            throw new DatabaseException(
                    "Could not delete fine.",
                    e
            );
        }
    }

    public List<Fine> findByMemberId(
            String memberId)
            throws DatabaseException {

        String sql = """
                SELECT fine_id,
                       issue_id,
                       member_id,
                       days_late,
                       amount,
                       status,
                       paid_at,
                       paid_by
                FROM fines
                WHERE member_id = ?
                ORDER BY fine_id DESC
                """;

        return findByStringParameter(
                sql,
                memberId
        );
    }

    public List<Fine> findUnpaidByMemberId(
            String memberId)
            throws DatabaseException {

        String sql = """
                SELECT fine_id,
                       issue_id,
                       member_id,
                       days_late,
                       amount,
                       status,
                       paid_at,
                       paid_by
                FROM fines
                WHERE member_id = ?
                  AND status = 'UNPAID'
                ORDER BY fine_id DESC
                """;

        return findByStringParameter(
                sql,
                memberId
        );
    }

    public double getUnpaidAmount(
            String memberId)
            throws DatabaseException {

        String sql = """
                SELECT COALESCE(SUM(amount), 0)
                FROM fines
                WHERE member_id = ?
                  AND status = 'UNPAID'
                """;

        try (Connection connection = dbConnection.getConnection();
             PreparedStatement statement =
                     connection.prepareStatement(sql)) {

            statement.setString(1, memberId);

            try (ResultSet resultSet =
                         statement.executeQuery()) {

                if (resultSet.next()) {
                    return resultSet.getDouble(1);
                }

                return 0.0;
            }

        } catch (SQLException e) {
            throw new DatabaseException(
                    "Could not calculate unpaid fine amount.",
                    e
            );
        }
    }

    public boolean markAsPaid(
            long fineId,
            long librarianId)
            throws DatabaseException {

        String sql = """
                UPDATE fines
                SET status = 'PAID',
                    paid_at = CURRENT_TIMESTAMP,
                    paid_by = ?
                WHERE fine_id = ?
                  AND status = 'UNPAID'
                """;

        try (Connection connection = dbConnection.getConnection();
             PreparedStatement statement =
                     connection.prepareStatement(sql)) {

            statement.setLong(1, librarianId);
            statement.setLong(2, fineId);

            return statement.executeUpdate() > 0;

        } catch (SQLException e) {
            throw new DatabaseException(
                    "Could not mark fine as paid.",
                    e
            );
        }
    }

    public Optional<Fine> findByIssueId(
            long issueId)
            throws DatabaseException {

        String sql = """
                SELECT fine_id,
                       issue_id,
                       member_id,
                       days_late,
                       amount,
                       status,
                       paid_at,
                       paid_by
                FROM fines
                WHERE issue_id = ?
                """;

        try (Connection connection = dbConnection.getConnection();
             PreparedStatement statement =
                     connection.prepareStatement(sql)) {

            statement.setLong(1, issueId);

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
                    "Could not find fine for issue.",
                    e
            );
        }
    }

    private List<Fine> findByStringParameter(
            String sql,
            String memberId)
            throws DatabaseException {

        List<Fine> fines = new ArrayList<>();

        try (Connection connection = dbConnection.getConnection();
             PreparedStatement statement =
                     connection.prepareStatement(sql)) {

            statement.setString(1, memberId);

            try (ResultSet resultSet =
                         statement.executeQuery()) {

                while (resultSet.next()) {
                    fines.add(mapResultSet(resultSet));
                }
            }

            return fines;

        } catch (SQLException e) {
            throw new DatabaseException(
                    "Could not retrieve member fines.",
                    e
            );
        }
    }

    private Fine mapResultSet(
            ResultSet resultSet)
            throws SQLException {

        Date paidDate =
                resultSet.getDate("paid_at");

        Long paidBy = null;

        long paidByValue =
                resultSet.getLong("paid_by");

        if (!resultSet.wasNull()) {
            paidBy = paidByValue;
        }

        return new Fine(
                resultSet.getLong("fine_id"),
                resultSet.getLong("issue_id"),
                resultSet.getString("member_id"),
                resultSet.getInt("days_late"),
                resultSet.getDouble("amount"),
                resultSet.getString("status"),
                paidDate != null
                        ? paidDate.toLocalDate()
                        : null,
                paidBy
        );
    }
}
