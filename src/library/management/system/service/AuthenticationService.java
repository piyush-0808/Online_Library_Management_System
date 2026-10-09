package library.management.system.service;

import java.util.Optional;

import library.management.system.dao.MemberDAO;
import library.management.system.dao.UserDAO;
import library.management.system.exception.AuthenticationException;
import library.management.system.exception.DatabaseException;
import library.management.system.member.Librarian;
import library.management.system.member.Member;
import library.management.system.model.MemberStatus;
import library.management.system.model.Role;
import library.management.system.model.User;
import library.management.system.util.PasswordUtil;

public class AuthenticationService {

    private final UserDAO userDAO;
    private final MemberDAO memberDAO;

    public AuthenticationService() {
        this.userDAO = new UserDAO();
        this.memberDAO = new MemberDAO();
    }

    /**
     * Authenticates a user using username and password.
     *
     * @param username username
     * @param password plain-text password entered by user
     * @return authenticated User
     */
    public User authenticate(
            String username,
            String password)
            throws AuthenticationException, DatabaseException {

        if (username == null || username.isBlank()) {
            throw new AuthenticationException(
                    "Username cannot be empty."
            );
        }

        if (password == null || password.isEmpty()) {
            throw new AuthenticationException(
                    "Password cannot be empty."
            );
        }

        Optional<User> userOptional =
                userDAO.findByUsername(username.trim());
        if (userOptional.isEmpty()) {
            throw new AuthenticationException(
                    "Invalid username or password.");
        }

        User user = userOptional.get();
        if (!PasswordUtil.verifyPassword(
                password, user.getSalt(), user.getPasswordHash())) {
            throw new AuthenticationException(
                    "Invalid username or password.");
        }
        validateAccountStatus(user);
        return user;
    }

    /**
     * Loads the complete Member profile after authentication.
     */
    public Member loadMemberProfile(
            User user)
            throws AuthenticationException, DatabaseException {

        if (user == null) {
            throw new AuthenticationException(
                    "User information is required."
            );
        }

        if (user.getRole() != Role.MEMBER) {
            throw new AuthenticationException(
                    "The authenticated user is not a member."
            );
        }

        Optional<Member> member = memberDAO.findByUserId(user.getUserId());
        if (member.isEmpty()) {
            throw new AuthenticationException(
                    "Member profile not found.");
        }
        return member.get();
    }

    /**
     * Creates a Librarian object from an authenticated User.
     */
    public Librarian loadLibrarianProfile(User user)
            throws AuthenticationException {

        if (user == null) {
            throw new AuthenticationException(
                    "User information is required."
            );
        }

        if (user.getRole() != Role.LIBRARIAN) {
            throw new AuthenticationException(
                    "The authenticated user is not a librarian."
            );
        }

        Librarian librarian = new Librarian(
                user.getFullName(),
                user.getEmail(),
                user.getPhone()
        );

        librarian.setUserId(user.getUserId());
        librarian.setUsername(user.getUsername());

        return librarian;
    }

    /**
     * Checks whether the account is allowed to log in.
     */
    private void validateAccountStatus(User user)
            throws AuthenticationException {

        if (user.getStatus() == MemberStatus.PENDING) {

            throw new AuthenticationException(
                    "Your account is pending librarian approval."
            );
        }

        if (user.getStatus() == MemberStatus.BLOCKED) {

            throw new AuthenticationException(
                    "Your account has been blocked. "
                    + "Please contact the librarian."
            );
        }

        if (user.getStatus() != MemberStatus.ACTIVE) {

            throw new AuthenticationException(
                    "Your account is not active."
            );
        }
    }

}
