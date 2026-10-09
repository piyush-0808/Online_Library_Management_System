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
import library.management.system.model.MemberStatus;
import library.management.system.model.Role;
import library.management.system.model.User;

/**
 * DAO for user account database operations.
 *
 * This class contains SQL for the users table.
 * Business rules remain in the service layer.
 */
public class UserDAO implements GenericDAO<User, Long> {

    private final DBConnection dbConnection;

    public UserDAO() {
        this.dbConnection = DBConnection.getInstance();
    }

    /**
     * Finds a user by primary key.
     */
    @Override
    public Optional<User> findById(Long id) throws DatabaseException {

        String sql = """
                SELECT user_id, username, password_hash, password_salt,
                       role, status, full_name, email, phone
                FROM users
                WHERE user_id = ?
                """;

        try (Connection connection = dbConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setLong(1, id);

            try (ResultSet resultSet = statement.executeQuery()) {

                if (resultSet.next()) {
                    return Optional.of(mapResultSet(resultSet));
                }

                return Optional.empty();
            }

        } catch (SQLException e) {
            throw new DatabaseException(
                    "Unable to find user with ID: " + id, e);
        }
    }

    /**
     * Returns all users.
     */
    @Override
    public List<User> findAll() throws DatabaseException {

        String sql = """
                SELECT user_id, username, password_hash, password_salt,
                       role, status, full_name, email, phone
                FROM users
                ORDER BY username
                """;

        List<User> users = new ArrayList<>();

        try (Connection connection = dbConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {

            while (resultSet.next()) {
                users.add(mapResultSet(resultSet));
            }

            return users;

        } catch (SQLException e) {
            throw new DatabaseException(
                    "Unable to load users.", e);
        }
    }

    /**
     * Saves a new user.
     *
     * RETURN_GENERATED_KEYS is used to obtain the generated user ID.
     */
    @Override
    public User save(User user) throws DatabaseException {

        String sql = """
                INSERT INTO users
                    (username, password_hash, password_salt, role, status,
                     full_name, email, phone)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                """;

        try (Connection connection = dbConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     sql,
                     Statement.RETURN_GENERATED_KEYS)) {

            statement.setString(1, user.getUsername());
            statement.setString(2, user.getPasswordHash());
            statement.setString(3, user.getSalt());
            statement.setString(4, user.getRole().name());
            statement.setString(5, user.getStatus().name());
            statement.setString(6, user.getFullName());
            statement.setString(7, user.getEmail());
            statement.setString(8, user.getPhone());

            statement.executeUpdate();

            try (ResultSet generatedKeys = statement.getGeneratedKeys()) {

                if (generatedKeys.next()) {
                    user.setUserId(generatedKeys.getLong(1));
                }
            }

            return user;

        } catch (SQLException e) {
            throw new DatabaseException(
                    "Unable to save user.", e);
        }
    }

    /**
     * Updates an existing user.
     */
    @Override
    public boolean update(User user) throws DatabaseException {

        String sql = """
                UPDATE users
                SET username = ?,
                    password_hash = ?,
                    password_salt = ?,
                    role = ?,
                    status = ?,
                    full_name = ?,
                    email = ?,
                    phone = ?
                WHERE user_id = ?
                """;

        try (Connection connection = dbConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, user.getUsername());
            statement.setString(2, user.getPasswordHash());
            statement.setString(3, user.getSalt());
            statement.setString(4, user.getRole().name());
            statement.setString(5, user.getStatus().name());
            statement.setString(6, user.getFullName());
            statement.setString(7, user.getEmail());
            statement.setString(8, user.getPhone());
            statement.setLong(9, user.getUserId());

            return statement.executeUpdate() > 0;

        } catch (SQLException e) {
            throw new DatabaseException(
                    "Unable to update user.", e);
        }
    }

    /**
     * Deletes a user by ID.
     */
    @Override
    public boolean delete(Long id) throws DatabaseException {

        String sql = """
                DELETE FROM users
                WHERE user_id = ?
                """;

        try (Connection connection = dbConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setLong(1, id);

            return statement.executeUpdate() > 0;

        } catch (SQLException e) {
            throw new DatabaseException(
                    "Unable to delete user with ID: " + id, e);
        }
    }

    /**
     * Finds a user using the unique username, including profile and
     * password-verification fields used by the authentication service.
     */
    public Optional<User> findByUsername(String username)
            throws DatabaseException {

        String sql = """
                SELECT user_id, username, password_hash, password_salt,
                       role, status, full_name, email, phone
                FROM users
                WHERE username = ?
                """;

        try (Connection connection = dbConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, username);

            try (ResultSet resultSet = statement.executeQuery()) {

                if (resultSet.next()) {
                    return Optional.of(mapResultSet(resultSet));
                }

                return Optional.empty();
            }

        } catch (SQLException e) {
            throw new DatabaseException(
                    "Unable to find user by username.", e);
        }
    }

    /**
     * Checks whether a username already exists.
     */
    public boolean existsByUsername(String username)
            throws DatabaseException {

        String sql = """
                SELECT 1
                FROM users
                WHERE username = ?
                LIMIT 1
                """;

        try (Connection connection = dbConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, username);

            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next();
            }

        } catch (SQLException e) {
            throw new DatabaseException(
                    "Unable to check username.", e);
        }
    }

    /**
     * Updates only the account status.
     *
     * Useful for approving or blocking member accounts.
     */
    public boolean updateStatus(Long userId, MemberStatus status)
            throws DatabaseException {

        String sql = """
                UPDATE users
                SET status = ?
                WHERE user_id = ?
                """;

        try (Connection connection = dbConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, status.name());
            statement.setLong(2, userId);

            return statement.executeUpdate() > 0;

        } catch (SQLException e) {
            throw new DatabaseException(
                    "Unable to update user status.", e);
        }
    }

    /**
     * Converts one database row into a User object.
     */
    private User mapResultSet(ResultSet resultSet)
            throws SQLException {

        long userId = resultSet.getLong("user_id");
        String username = resultSet.getString("username");
        String passwordHash = resultSet.getString("password_hash");
        String salt = resultSet.getString("password_salt");

        Role role = Role.valueOf(
                resultSet.getString("role").toUpperCase());

        MemberStatus status = MemberStatus.valueOf(
                resultSet.getString("status").toUpperCase());

        return new User(
                userId,
                username,
                passwordHash,
                salt,
                role,
                status,
                resultSet.getString("full_name"),
                resultSet.getString("email"),
                resultSet.getString("phone")
        );
    }
}
