package library.management.system.db;

import java.sql.Connection;
import java.sql.SQLException;

/** Small generic helper for an atomic JDBC callback. */
public final class DatabaseTransaction {

    private DatabaseTransaction() { }

    @FunctionalInterface
    public interface Work<T> {
        T execute(Connection connection) throws SQLException;
    }

    /** Commits a successful callback and rolls back SQL/runtime failures. */
    public static <T> T execute(Connection connection, Work<T> work)
            throws SQLException {
        boolean originalAutoCommit = connection.getAutoCommit();
        connection.setAutoCommit(false);
        try {
            T result = work.execute(connection);
            connection.commit();
            return result;
        } catch (SQLException | RuntimeException ex) {
            try {
                connection.rollback();
            } catch (SQLException rollbackError) {
                ex.addSuppressed(rollbackError);
            }
            throw ex;
        } finally {
            connection.setAutoCommit(originalAutoCommit);
        }
    }
}
