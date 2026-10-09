package library.management.system.service;

import java.util.List;
import java.util.Optional;

import library.management.system.dao.UserDAO;
import library.management.system.exception.DatabaseException;
import library.management.system.exception.DuplicateRecordException;
import library.management.system.exception.InvalidInputException;
import library.management.system.model.MemberStatus;
import library.management.system.model.Role;
import library.management.system.model.User;
import library.management.system.util.PasswordUtil;
import library.management.system.util.Validator;

public class UserService {

    private final UserDAO userDAO;

    public UserService() {
        this.userDAO = new UserDAO();
    }

    /**
     * Finds a user by ID.
     */
    public Optional<User> findById(long userId)
            throws DatabaseException,
                   InvalidInputException {

        validateUserId(userId);

        return userDAO.findById(userId);
    }

    /**
     * Finds a user by username.
     */
    public Optional<User> findByUsername(
            String username)
            throws DatabaseException,
                   InvalidInputException {

        validateUsername(username);

        return userDAO.findByUsername(username);
    }

    /**
     * Returns all users.
     */
    public List<User> findAll()
            throws DatabaseException {

        return userDAO.findAll();
    }

    /**
     * Returns users having a particular role.
     */
    public List<User> findByRole(
            Role role)
            throws DatabaseException,
                   InvalidInputException {

        if (role == null) {
            throw new InvalidInputException(
                    "Role cannot be null."
            );
        }

        List<User> users = userDAO.findAll();

        return users.stream()
                .filter(user -> user.getRole() == role)
                .toList();
    }

    /**
     * Returns users having a particular status.
     */
    public List<User> findByStatus(
            MemberStatus status)
            throws DatabaseException,
                   InvalidInputException {

        if (status == null) {
            throw new InvalidInputException(
                    "Status cannot be null."
            );
        }

        List<User> users = userDAO.findAll();

        return users.stream()
                .filter(user -> user.getStatus() == status)
                .toList();
    }

    /**
     * Creates a librarian user.
     *
     * This method is mainly intended for administrative use.
     */
    public User createLibrarian(
            String username,
            String password,
            String fullName,
            String email,
            String phone)
            throws DatabaseException,
                   InvalidInputException,
                   DuplicateRecordException {

        validateUserDetails(
                username,
                password,
                fullName,
                email,
                phone
        );

        checkDuplicateUsername(username);

        String salt =
                PasswordUtil.generateSalt();

        String passwordHash =
                PasswordUtil.hashPassword(
                        password,
                        salt
                );

        User user = new User(
                0,
                username,
                passwordHash,
                salt,
                Role.LIBRARIAN,
                MemberStatus.ACTIVE,
                fullName,
                email,
                phone
        );

        return userDAO.save(user);
    }

    /**
     * Changes user status.
     */
    public boolean updateStatus(
            long userId,
            MemberStatus status)
            throws DatabaseException,
                   InvalidInputException {

        validateUserId(userId);

        if (status == null) {
            throw new InvalidInputException(
                    "Status cannot be null."
            );
        }

        return userDAO.updateStatus(
                userId,
                status
        );
    }

    /**
     * Activates a user.
     */
    public boolean activateUser(
            long userId)
            throws DatabaseException,
                   InvalidInputException {

        return updateStatus(
                userId,
                MemberStatus.ACTIVE
        );
    }

    /**
     * Blocks a user.
     */
    public boolean blockUser(
            long userId)
            throws DatabaseException,
                   InvalidInputException {

        return updateStatus(
                userId,
                MemberStatus.BLOCKED
        );
    }

    /**
     * Changes a user's password.
     *
     * A new random salt is generated every time.
     */
    public boolean changePassword(
            long userId,
            String currentPassword,
            String newPassword)
            throws DatabaseException,
                   InvalidInputException,
                   DuplicateRecordException {

        validateUserId(userId);

        if (!Validator.isNotBlank(currentPassword)) {
            throw new InvalidInputException(
                    "Current password cannot be empty."
            );
        }

        if (!Validator.isValidPassword(newPassword)) {
            throw new InvalidInputException(
                    "New password must contain at least 6 characters."
            );
        }

        Optional<User> optionalUser =
                userDAO.findById(userId);

        if (optionalUser.isEmpty()) {
            throw new InvalidInputException(
                    "User not found."
            );
        }

        User user = optionalUser.get();

        if (!PasswordUtil.verifyPassword(
                currentPassword,
                user.getSalt(),
                user.getPasswordHash())) {

            throw new InvalidInputException(
                    "Current password is incorrect."
            );
        }

        String newSalt =
                PasswordUtil.generateSalt();

        String newHash =
                PasswordUtil.hashPassword(
                        newPassword,
                        newSalt
                );

        user.setSalt(newSalt);
        user.setPasswordHash(newHash);

        return userDAO.update(user);
    }

    /**
     * Updates basic profile information.
     */
    public boolean updateProfile(
            long userId,
            String fullName,
            String email,
            String phone)
            throws DatabaseException,
                   InvalidInputException {

        validateUserId(userId);

        if (!Validator.isNotBlank(fullName)) {
            throw new InvalidInputException(
                    "Full name cannot be empty."
            );
        }

        if (!Validator.isValidEmail(email)) {
            throw new InvalidInputException(
                    "Invalid email address."
            );
        }

        if (!Validator.isValidPhone(phone)) {
            throw new InvalidInputException(
                    "Phone number must contain exactly 10 digits."
            );
        }

        Optional<User> optionalUser =
                userDAO.findById(userId);

        if (optionalUser.isEmpty()) {
            throw new InvalidInputException(
                    "User not found."
            );
        }

        User user = optionalUser.get();

        user.setFullName(fullName.trim());
        user.setEmail(email.trim());
        user.setPhone(phone.trim());

        return userDAO.update(user);
    }

    /**
     * Deletes a user.
     */
    public boolean deleteUser(
            long userId)
            throws DatabaseException,
                   InvalidInputException {

        validateUserId(userId);

        Optional<User> optionalUser =
                userDAO.findById(userId);

        if (optionalUser.isEmpty()) {
            throw new InvalidInputException(
                    "User not found."
            );
        }

        return userDAO.delete(userId);
    }

    private void checkDuplicateUsername(
            String username)
            throws DatabaseException,
                   DuplicateRecordException {

        if (userDAO.existsByUsername(username)) {
            throw new DuplicateRecordException(
                    "Username already exists."
            );
        }
    }

    private void validateUserDetails(
            String username,
            String password,
            String fullName,
            String email,
            String phone)
            throws InvalidInputException {

        validateUsername(username);

        if (!Validator.isValidPassword(password)) {
            throw new InvalidInputException(
                    "Password must contain at least 6 characters."
            );
        }

        if (!Validator.isNotBlank(fullName)) {
            throw new InvalidInputException(
                    "Full name cannot be empty."
            );
        }

        if (!Validator.isValidEmail(email)) {
            throw new InvalidInputException(
                    "Invalid email address."
            );
        }

        if (!Validator.isValidPhone(phone)) {
            throw new InvalidInputException(
                    "Phone number must contain exactly 10 digits."
            );
        }
    }

    private void validateUsername(
            String username)
            throws InvalidInputException {

        if (!Validator.isValidUsername(username)) {
            throw new InvalidInputException(
                    "Username must be 3-30 characters and contain "
                    + "only letters, numbers, underscore or dot."
            );
        }
    }

    private void validateUserId(
            long userId)
            throws InvalidInputException {

        if (userId <= 0) {
            throw new InvalidInputException(
                    "Invalid user ID."
            );
        }
    }
}
