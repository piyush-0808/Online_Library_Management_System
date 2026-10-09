package library.management.system.db;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/** Applies small, additive database changes for an existing installation. */
public final class SchemaMigrator {

    private SchemaMigrator() { }

    public static void main(String[] args) throws SQLException {
        try (Connection connection = DBConnection.getInstance().getConnection()) {
            if (!hasColumn(connection.getMetaData(), connection.getCatalog(),
                    "books", "is_reference")) {
                try (Statement statement = connection.createStatement()) {
                    statement.executeUpdate(
                            "ALTER TABLE books ADD COLUMN is_reference "
                            + "BOOLEAN NOT NULL DEFAULT FALSE AFTER category");
                }
                System.out.println("Applied migration: books.is_reference");
            }
            try (Statement statement = connection.createStatement()) {
                statement.executeUpdate("ALTER TABLE reservations MODIFY COLUMN status "
                        + "ENUM('WAITING', 'READY', 'FULFILLED', 'CANCELLED') "
                        + "NOT NULL DEFAULT 'WAITING'");
            }
            System.out.println("Schema is up to date (READY reservation state available).");
        }
    }

    private static boolean hasColumn(
            DatabaseMetaData metadata,
            String catalog,
            String table,
            String column)
            throws SQLException {
        try (ResultSet columns = metadata.getColumns(
                catalog, null, table, column)) {
            return columns.next();
        }
    }
}
