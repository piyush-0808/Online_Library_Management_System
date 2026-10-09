package library.management.system.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import library.management.system.db.DBConnection;
import library.management.system.exception.DatabaseException;

/** SQL access for dashboard and summary reports. */
public class ReportDAO {

    private final DBConnection dbConnection = DBConnection.getInstance();

    public DashboardMetrics getDashboardMetrics() throws DatabaseException {
        String booksSql = """
                SELECT COUNT(*) total_books,
                       COALESCE(SUM(total_copies), 0) total_copies,
                       COALESCE(SUM(available_copies), 0) available_copies
                FROM books
                """;
        String membersSql = """
                SELECT COUNT(*) total_members,
                       COALESCE(SUM(u.status = 'ACTIVE'), 0) active_members,
                       COALESCE(SUM(u.status = 'PENDING'), 0) pending_members,
                       COALESCE(SUM(u.status = 'BLOCKED'), 0) blocked_members
                FROM members m JOIN users u ON u.user_id = m.user_id
                """;
        String issuesSql = """
                SELECT COALESCE(SUM(status IN ('ISSUED', 'OVERDUE')), 0) active_issues,
                       COALESCE(SUM(status = 'OVERDUE'), 0) overdue_issues,
                       COALESCE(SUM(status = 'RETURNED'), 0) returned_issues
                FROM issues
                """;
        String finesSql = """
                SELECT COALESCE(SUM(IF(status = 'UNPAID', amount, 0)), 0) unpaid_fines,
                       COALESCE(SUM(IF(status = 'PAID', amount, 0)), 0) paid_fines,
                       COALESCE(SUM(status = 'UNPAID'), 0) unpaid_fine_count
                FROM fines
                """;
        String reservationsSql = """
                SELECT COUNT(*) total_reservations,
                       COALESCE(SUM(status = 'WAITING'), 0) waiting_reservations,
                       COALESCE(SUM(status = 'FULFILLED'), 0) fulfilled_reservations,
                       COALESCE(SUM(status = 'CANCELLED'), 0) cancelled_reservations
                FROM reservations
                """;

        try (Connection connection = dbConnection.getConnection()) {
            try (PreparedStatement statement = connection.prepareStatement(booksSql);
                 ResultSet result = statement.executeQuery()) {
                result.next();
                int totalBooks = result.getInt("total_books");
                int totalCopies = result.getInt("total_copies");
                int availableCopies = result.getInt("available_copies");

                try (PreparedStatement memberStatement = connection.prepareStatement(membersSql);
                     ResultSet members = memberStatement.executeQuery()) {
                    members.next();
                    int totalMembers = members.getInt("total_members");
                    int activeMembers = members.getInt("active_members");
                    int pendingMembers = members.getInt("pending_members");
                    int blockedMembers = members.getInt("blocked_members");

                    try (PreparedStatement issueStatement = connection.prepareStatement(issuesSql);
                         ResultSet issues = issueStatement.executeQuery()) {
                        issues.next();
                        int activeIssues = issues.getInt("active_issues");
                        int overdueIssues = issues.getInt("overdue_issues");
                        int returnedIssues = issues.getInt("returned_issues");

                        try (PreparedStatement fineStatement = connection.prepareStatement(finesSql);
                             ResultSet fines = fineStatement.executeQuery();
                             PreparedStatement reservationStatement = connection.prepareStatement(reservationsSql);
                             ResultSet reservations = reservationStatement.executeQuery()) {
                            fines.next();
                            reservations.next();
                            return new DashboardMetrics(
                                    totalBooks, totalCopies, availableCopies,
                                    totalMembers, activeMembers, pendingMembers,
                                    blockedMembers, activeIssues, overdueIssues,
                                    returnedIssues, fines.getDouble("unpaid_fines"),
                                    fines.getDouble("paid_fines"),
                                    fines.getInt("unpaid_fine_count"),
                                    reservations.getInt("total_reservations"),
                                    reservations.getInt("waiting_reservations"),
                                    reservations.getInt("fulfilled_reservations"),
                                    reservations.getInt("cancelled_reservations"));
                        }
                    }
                }
            }
        } catch (SQLException ex) {
            throw new DatabaseException("Could not generate dashboard report.", ex);
        }
    }

    public List<CirculationRow> findCirculationRows(String status, String search)
            throws DatabaseException {
        String sql = """
                SELECT i.issue_id, i.member_id, u.full_name, b.book_id,
                       b.title, i.issue_date, i.due_date, i.status
                FROM issues i
                JOIN members m ON m.member_id = i.member_id
                JOIN users u ON u.user_id = m.user_id
                JOIN books b ON b.book_id = i.book_id
                WHERE i.status IN ('ISSUED', 'OVERDUE')
                  AND i.status = ?
                  AND (? = '' OR CAST(i.issue_id AS CHAR) LIKE ?
                       OR i.member_id LIKE ? OR u.full_name LIKE ?
                       OR CAST(b.book_id AS CHAR) LIKE ? OR b.title LIKE ?)
                ORDER BY i.due_date, i.issue_id
                """;
        String term = search == null ? "" : search.trim();
        String pattern = "%" + term + "%";
        List<CirculationRow> rows = new ArrayList<>();
        try (Connection connection = dbConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, status);
            statement.setString(2, term);
            for (int parameter = 3; parameter <= 7; parameter++) {
                statement.setString(parameter, pattern);
            }
            try (ResultSet result = statement.executeQuery()) {
                while (result.next()) {
                    rows.add(new CirculationRow(
                            result.getLong("issue_id"),
                            result.getString("member_id"),
                            result.getString("full_name"),
                            result.getLong("book_id"),
                            result.getString("title"),
                            result.getDate("issue_date").toLocalDate(),
                            result.getDate("due_date").toLocalDate(),
                            result.getString("status")));
                }
            }
            return rows;
        } catch (SQLException ex) {
            throw new DatabaseException("Could not load circulation report.", ex);
        }
    }

    public List<FineCollectionRow> findFineCollectionRows(
            String status, String search)
            throws DatabaseException {
        String sql = """
                SELECT f.fine_id, f.issue_id, f.member_id, u.full_name,
                       b.title, f.days_late, f.amount, f.status, f.created_at, f.paid_at
                FROM fines f
                JOIN members m ON m.member_id = f.member_id
                JOIN users u ON u.user_id = m.user_id
                JOIN issues i ON i.issue_id = f.issue_id
                JOIN books b ON b.book_id = i.book_id
                WHERE (? = 'ALL' OR f.status = ?)
                  AND (? = '' OR CAST(f.fine_id AS CHAR) LIKE ?
                       OR CAST(f.issue_id AS CHAR) LIKE ?
                       OR f.member_id LIKE ? OR u.full_name LIKE ? OR b.title LIKE ?)
                ORDER BY f.created_at DESC, f.fine_id DESC
                """;
        String term = search == null ? "" : search.trim();
        String pattern = "%" + term + "%";
        List<FineCollectionRow> rows = new ArrayList<>();
        try (Connection connection = dbConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, status);
            statement.setString(2, status);
            statement.setString(3, term);
            for (int parameter = 4; parameter <= 8; parameter++) {
                statement.setString(parameter, pattern);
            }
            try (ResultSet result = statement.executeQuery()) {
                while (result.next()) {
                    java.sql.Timestamp created = result.getTimestamp("created_at");
                    java.sql.Timestamp paid = result.getTimestamp("paid_at");
                    rows.add(new FineCollectionRow(
                            result.getLong("fine_id"),
                            result.getLong("issue_id"),
                            result.getString("member_id"),
                            result.getString("full_name"),
                            result.getString("title"),
                            result.getInt("days_late"),
                            result.getBigDecimal("amount"),
                            result.getString("status"),
                            created == null ? null : created.toLocalDateTime(),
                            paid == null ? null : paid.toLocalDateTime()));
                }
            }
            return rows;
        } catch (SQLException ex) {
            throw new DatabaseException("Could not load fine collection report.", ex);
        }
    }

    public record CirculationRow(
            long issueId, String memberId, String memberName,
            long bookId, String bookTitle, LocalDate issueDate,
            LocalDate dueDate, String status) { }

    public record FineCollectionRow(
            long fineId, long issueId, String memberId, String memberName,
            String bookTitle, int daysLate, java.math.BigDecimal amount,
            String status, java.time.LocalDateTime createdAt,
            java.time.LocalDateTime paidAt) { }

    public int getTotalBooks() throws DatabaseException {
        return queryInt("SELECT COUNT(*) FROM books");
    }

    public int getTotalMembers() throws DatabaseException {
        return queryInt("SELECT COUNT(*) FROM members");
    }

    public int getActiveIssues() throws DatabaseException {
        return queryInt("SELECT COUNT(*) FROM issues WHERE status IN ('ISSUED', 'OVERDUE')");
    }

    public int getOverdueIssues() throws DatabaseException {
        return queryInt("SELECT COUNT(*) FROM issues WHERE status = 'OVERDUE'");
    }

    public double getTotalUnpaidFines() throws DatabaseException {
        return queryDouble("SELECT COALESCE(SUM(amount), 0) FROM fines WHERE status = 'UNPAID'");
    }

    private int queryInt(String sql) throws DatabaseException {
        return (int) queryNumber(sql, true);
    }

    private double queryDouble(String sql) throws DatabaseException {
        return queryNumber(sql, false);
    }

    private double queryNumber(String sql, boolean integer) throws DatabaseException {
        try (Connection connection = dbConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet result = statement.executeQuery()) {
            return result.next()
                    ? integer ? result.getInt(1) : result.getDouble(1)
                    : 0;
        } catch (SQLException ex) {
            throw new DatabaseException("Could not generate report.", ex);
        }
    }

    public record DashboardMetrics(
            int totalBooks,
            int totalCopies,
            int availableCopies,
            int totalMembers,
            int activeMembers,
            int pendingMembers,
            int blockedMembers,
            int activeIssues,
            int overdueIssues,
            int returnedIssues,
            double unpaidFines,
            double paidFines,
            int unpaidFineCount,
            int totalReservations,
            int waitingReservations,
            int fulfilledReservations,
            int cancelledReservations) { }
}
