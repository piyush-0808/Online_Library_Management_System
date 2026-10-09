package library.management.system.dao;

import library.management.system.db.DBConnection;
import library.management.system.exception.DatabaseException;
import library.management.system.member.FacultyMember;
import library.management.system.member.Member;
import library.management.system.member.StudentMember;
import library.management.system.model.MemberStatus;
import library.management.system.model.MemberType;
import library.management.system.util.MemberIdGenerator;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class MemberDAO implements GenericDAO<Member, String> {

    public String registerPendingMember(
            String username,
            String passwordHash,
            String passwordSalt,
            String fullName,
            String email,
            String phone,
            MemberType memberType,
            String department)
            throws DatabaseException {
        String userSql = """
                INSERT INTO users
                    (username, password_hash, password_salt, role, status,
                     full_name, email, phone)
                VALUES (?, ?, ?, 'MEMBER', 'PENDING', ?, ?, ?)
                """;
        String memberSql = """
                INSERT INTO members
                    (member_id, user_id, member_type, department, joined_date)
                VALUES (?, ?, ?, ?, ?)
                """;

        try (Connection connection = DBConnection.getInstance().getConnection()) {
            connection.setAutoCommit(false);
            try {
                long userId;
                try (PreparedStatement statement = connection.prepareStatement(
                        userSql, Statement.RETURN_GENERATED_KEYS)) {
                    statement.setString(1, username);
                    statement.setString(2, passwordHash);
                    statement.setString(3, passwordSalt);
                    statement.setString(4, fullName);
                    statement.setString(5, email);
                    statement.setString(6, phone);
                    statement.executeUpdate();
                    try (ResultSet keys = statement.getGeneratedKeys()) {
                        if (!keys.next()) {
                            throw new SQLException("Could not retrieve user ID.");
                        }
                        userId = keys.getLong(1);
                    }
                }

                String memberId = generateUniqueMemberId(connection);
                try (PreparedStatement statement = connection.prepareStatement(memberSql)) {
                    statement.setString(1, memberId);
                    statement.setLong(2, userId);
                    statement.setString(3, memberType.name());
                    statement.setString(4, department);
                    statement.setDate(5, java.sql.Date.valueOf(LocalDate.now()));
                    statement.executeUpdate();
                }
                connection.commit();
                return memberId;
            } catch (SQLException ex) {
                connection.rollback();
                throw ex;
            }
        } catch (SQLException ex) {
            throw new DatabaseException("Member registration failed.", ex);
        }
    }

    public void approvePendingMember(String memberId, long approvedBy)
            throws DatabaseException {
        String sql = """
                UPDATE users u
                JOIN members m ON m.user_id = u.user_id
                SET u.status = 'ACTIVE',
                    m.approved_by = ?,
                    m.approved_at = CURRENT_TIMESTAMP
                WHERE m.member_id = ?
                  AND u.role = 'MEMBER'
                  AND u.status = 'PENDING'
                """;
        try (Connection connection = DBConnection.getInstance().getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, approvedBy);
            statement.setString(2, memberId);
            if (statement.executeUpdate() == 0) {
                throw new DatabaseException(
                        "Member was not found or is no longer pending.");
            }
        } catch (SQLException ex) {
            throw new DatabaseException("Member approval failed.", ex);
        }
    }

    public boolean rejectPendingMember(String memberId)
            throws DatabaseException {
        String sql = """
                DELETE u FROM users u
                JOIN members m ON m.user_id = u.user_id
                WHERE m.member_id = ?
                  AND u.role = 'MEMBER'
                  AND u.status = 'PENDING'
                """;
        try (Connection connection = DBConnection.getInstance().getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, memberId);
            return statement.executeUpdate() == 1;
        } catch (SQLException ex) {
            throw new DatabaseException("Member rejection failed.", ex);
        }
    }

    private String generateUniqueMemberId(Connection connection)
            throws SQLException {
        for (int attempt = 0; attempt < 10; attempt++) {
            String memberId = MemberIdGenerator.nextId();
            try (PreparedStatement statement = connection.prepareStatement(
                    "SELECT 1 FROM members WHERE member_id = ?")) {
                statement.setString(1, memberId);
                try (ResultSet result = statement.executeQuery()) {
                    if (!result.next()) {
                        return memberId;
                    }
                }
            }
        }
        throw new SQLException("Unable to generate a unique member ID.");
    }

    @Override
    public Optional<Member> findById(String memberId)
            throws DatabaseException {

        String sql = """
                SELECT
                    m.member_id,
                    m.user_id,
                    m.member_type,
                    m.department,
                    m.joined_date,
                    m.approved_by,
                    m.approved_at,
                    u.full_name,
                    u.email,
                    u.phone,
                    u.status
                FROM members m
                INNER JOIN users u
                    ON m.user_id = u.user_id
                WHERE m.member_id = ?
                """;

        try (Connection connection = DBConnection.getInstance().getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, memberId);

            try (ResultSet resultSet = statement.executeQuery()) {

                if (resultSet.next()) {
                    return Optional.of(mapResultSet(resultSet));
                }

                return Optional.empty();
            }

        } catch (SQLException ex) {
            throw new DatabaseException(
                    "Unable to find member by ID.",
                    ex
            );
        }
    }

    @Override
    public List<Member> findAll() throws DatabaseException {

        String sql = """
                SELECT
                    m.member_id,
                    m.user_id,
                    m.member_type,
                    m.department,
                    m.joined_date,
                    m.approved_by,
                    m.approved_at,
                    u.full_name,
                    u.email,
                    u.phone,
                    u.status
                FROM members m
                INNER JOIN users u
                    ON m.user_id = u.user_id
                ORDER BY m.member_id
                """;

        List<Member> members = new ArrayList<>();

        try (Connection connection = DBConnection.getInstance().getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {

            while (resultSet.next()) {
                members.add(mapResultSet(resultSet));
            }

            return members;

        } catch (SQLException ex) {
            throw new DatabaseException(
                    "Unable to fetch members.",
                    ex
            );
        }
    }

    @Override
    public Member save(Member member) throws DatabaseException {

        String userSql = """
                INSERT INTO users
                (
                    username,
                    password_hash,
                    password_salt,
                    role,
                    status,
                    full_name,
                    email,
                    phone
                )
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                """;

        String memberSql = """
                INSERT INTO members
                (
                    member_id,
                    user_id,
                    member_type,
                    department,
                    joined_date
                )
                VALUES (?, ?, ?, ?, ?)
                """;

        throw new DatabaseException(
                "Member registration must be handled by MemberService " +
                "because it requires a transaction across users and members."
        );
    }

    @Override
    public boolean update(Member member) throws DatabaseException {

        String userSql = """
                UPDATE users u
                INNER JOIN members m
                    ON u.user_id = m.user_id
                SET
                    u.full_name = ?,
                    u.email = ?,
                    u.phone = ?,
                    u.status = ?,
                    m.member_type = ?,
                    m.department = ?
                WHERE m.member_id = ?
                """;

        try (Connection connection = DBConnection.getInstance().getConnection();
             PreparedStatement statement =
                     connection.prepareStatement(userSql)) {

            statement.setString(1, member.getFullName());
            statement.setString(2, member.getEmail());
            statement.setString(3, member.getPhone());
            statement.setString(4, member.getStatus().name());

            MemberType type =
                    member instanceof FacultyMember
                            ? MemberType.FACULTY
                            : MemberType.STUDENT;

            statement.setString(5, type.name());
            statement.setString(6, member.getDepartment());
            statement.setString(7, member.getMemberId());

            return statement.executeUpdate() > 0;

        } catch (SQLException ex) {
            throw new DatabaseException(
                    "Unable to update member.",
                    ex
            );
        }
    }

    @Override
    public boolean delete(String memberId) throws DatabaseException {

        String sql = """
                DELETE FROM members
                WHERE member_id = ?
                """;

        try (Connection connection = DBConnection.getInstance().getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, memberId);

            return statement.executeUpdate() > 0;

        } catch (SQLException ex) {
            throw new DatabaseException(
                    "Unable to delete member.",
                    ex
            );
        }
    }

    public List<Member> findByStatus(MemberStatus status)
            throws DatabaseException {

        String sql = """
                SELECT
                    m.member_id,
                    m.user_id,
                    m.member_type,
                    m.department,
                    m.joined_date,
                    m.approved_by,
                    m.approved_at,
                    u.full_name,
                    u.email,
                    u.phone,
                    u.status
                FROM members m
                INNER JOIN users u
                    ON m.user_id = u.user_id
                WHERE u.status = ?
                ORDER BY m.joined_date
                """;

        List<Member> members = new ArrayList<>();

        try (Connection connection = DBConnection.getInstance().getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, status.name());

            try (ResultSet resultSet = statement.executeQuery()) {

                while (resultSet.next()) {
                    members.add(mapResultSet(resultSet));
                }
            }

            return members;

        } catch (SQLException ex) {
            throw new DatabaseException(
                    "Unable to fetch members by status.",
                    ex
            );
        }
    }

    public Optional<Member> findByUserId(long userId)
            throws DatabaseException {

        String sql = """
                SELECT
                    m.member_id,
                    m.user_id,
                    m.member_type,
                    m.department,
                    m.joined_date,
                    m.approved_by,
                    m.approved_at,
                    u.full_name,
                    u.email,
                    u.phone,
                    u.status
                FROM members m
                INNER JOIN users u
                    ON m.user_id = u.user_id
                WHERE m.user_id = ?
                """;

        try (Connection connection = DBConnection.getInstance().getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setLong(1, userId);

            try (ResultSet resultSet = statement.executeQuery()) {

                if (resultSet.next()) {
                    return Optional.of(mapResultSet(resultSet));
                }

                return Optional.empty();
            }

        } catch (SQLException ex) {
            throw new DatabaseException(
                    "Unable to find member by user ID.",
                    ex
            );
        }
    }

    public boolean updateApproval(
            String memberId,
            long approvedBy)
            throws DatabaseException {

        String sql = """
                UPDATE members
                SET
                    approved_by = ?,
                    approved_at = CURRENT_TIMESTAMP
                WHERE member_id = ?
                """;

        try (Connection connection = DBConnection.getInstance().getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setLong(1, approvedBy);
            statement.setString(2, memberId);

            return statement.executeUpdate() > 0;

        } catch (SQLException ex) {
            throw new DatabaseException(
                    "Unable to approve member.",
                    ex
            );
        }
    }

    public boolean updateStatus(String memberId, MemberStatus status)
            throws DatabaseException {
        String sql = """
                UPDATE users u
                INNER JOIN members m ON m.user_id = u.user_id
                SET u.status = ?
                WHERE m.member_id = ?
                  AND u.role = 'MEMBER'
                """;

        try (Connection connection = DBConnection.getInstance().getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, status.name());
            statement.setString(2, memberId);
            return statement.executeUpdate() == 1;
        } catch (SQLException ex) {
            throw new DatabaseException("Unable to update member status.", ex);
        }
    }

    public boolean existsByEmail(String email)
            throws DatabaseException {

        String sql = """
                SELECT 1
                FROM users
                WHERE email = ?
                LIMIT 1
                """;

        try (Connection connection = DBConnection.getInstance().getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, email);

            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next();
            }

        } catch (SQLException ex) {
            throw new DatabaseException(
                    "Unable to check member email.",
                    ex
            );
        }
    }

    public boolean existsByPhone(String phone)
            throws DatabaseException {

        String sql = """
                SELECT 1
                FROM users
                WHERE phone = ?
                LIMIT 1
                """;

        try (Connection connection = DBConnection.getInstance().getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, phone);

            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next();
            }

        } catch (SQLException ex) {
            throw new DatabaseException(
                    "Unable to check member phone.",
                    ex
            );
        }
    }

    private Member mapResultSet(ResultSet resultSet)
            throws SQLException {

        String memberId = resultSet.getString("member_id");
        String fullName = resultSet.getString("full_name");
        String email = resultSet.getString("email");
        String phone = resultSet.getString("phone");

        MemberStatus status =
                MemberStatus.valueOf(
                        resultSet.getString("status")
                );

        MemberType memberType =
                MemberType.valueOf(
                        resultSet.getString("member_type")
                );

        String department =
                resultSet.getString("department");

        LocalDate joinedDate =
                resultSet.getDate("joined_date")
                        .toLocalDate();

        Member member;

        if (memberType == MemberType.FACULTY) {
            member = new FacultyMember(
                    memberId,
                    status,
                    fullName,
                    email,
                    phone
            );
        } else {
            member = new StudentMember(
                    memberId,
                    status,
                    fullName,
                    email,
                    phone
            );
        }

        member.setDepartment(department);
        member.setJoinedDate(joinedDate);

        long approvedBy =
                resultSet.getLong("approved_by");

        if (!resultSet.wasNull()) {
            member.setApprovedBy(approvedBy);
        }

        java.sql.Timestamp approvedAt =
                resultSet.getTimestamp("approved_at");

        if (approvedAt != null) {
            member.setApprovedAt(
                    approvedAt.toLocalDateTime().toLocalDate()
            );
        }

        return member;
    }
}
