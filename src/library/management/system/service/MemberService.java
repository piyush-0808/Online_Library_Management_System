package library.management.system.service;

import library.management.system.dao.MemberDAO;
import library.management.system.dao.UserDAO;
import library.management.system.exception.DatabaseException;
import library.management.system.exception.DuplicateRecordException;
import library.management.system.exception.InvalidInputException;
import library.management.system.member.FacultyMember;
import library.management.system.member.Member;
import library.management.system.member.MemberFactory;
import library.management.system.member.StudentMember;
import library.management.system.model.MemberStatus;
import library.management.system.model.MemberType;
import library.management.system.model.User;
import library.management.system.util.PasswordUtil;
import library.management.system.util.Validator;

/**
 * Service layer for member registration and approval.
 *
 * UI must call this service instead of directly accessing the database.
 */
public class MemberService {

    private final MemberDAO memberDAO;
    private final UserDAO userDAO;

    public MemberService() {
        this.memberDAO = new MemberDAO();
        this.userDAO = new UserDAO();
    }

    /**
     * Registers a new member.
     *
     * Registration creates:
     * 1. A MEMBER record in users
     * 2. A corresponding record in members
     *
     * Both operations are performed inside one transaction.
     */
    public Member registerMember(
            String username,
            String password,
            String fullName,
            String email,
            String phone,
            MemberType memberType,
            String department)
            throws InvalidInputException, DuplicateRecordException, DatabaseException {

        validateRegistrationInput(
                username,
                password,
                fullName,
                email,
                phone,
                memberType,
                department
        );

        if (userDAO.existsByUsername(username)) {
            throw new DuplicateRecordException(
                    "Username already exists: " + username);
        }
        if (memberDAO.existsByEmail(email)) {
            throw new DuplicateRecordException(
                    "Email already exists: " + email);
        }
        if (memberDAO.existsByPhone(phone)) {
            throw new DuplicateRecordException(
                    "Phone number already exists: " + phone);
        }

        String salt = PasswordUtil.generateSalt();
        String passwordHash = PasswordUtil.hashPassword(password, salt);
        String memberId = memberDAO.registerPendingMember(
                username, passwordHash, salt, fullName, email, phone,
                memberType, department);
        return MemberFactory.createMember(
                memberType, memberId, MemberStatus.PENDING,
                fullName, email, phone);
    }

    /**
     * Approves a pending member.
     *
     * Updates both users.status and members approval information
     * inside one transaction.
     */
    public void approveMember(
            String memberId,
            long approvedBy)
            throws InvalidInputException, DatabaseException {

        if (!Validator.isNotBlank(memberId)) {
            throw new InvalidInputException(
                    "Member ID cannot be empty."
            );
        }

        if (approvedBy <= 0) {
            throw new InvalidInputException(
                    "Invalid approving librarian ID."
            );
        }

        memberDAO.approvePendingMember(memberId.trim(), approvedBy);
    }

    /**
     * Rejects/cancels a pending member registration by deleting
     * the member and its associated user.
     *
     * This method is intentionally transactional.
     */
    public void rejectMember(
            String memberId)
            throws InvalidInputException, DatabaseException {

        if (!Validator.isNotBlank(memberId)) {
            throw new InvalidInputException(
                    "Member ID cannot be empty."
            );
        }

        if (!memberDAO.rejectPendingMember(memberId.trim())) {
            throw new InvalidInputException(
                    "Member was not found or is no longer pending.");
        }
    }

    /**
     * Returns all members.
     *
     * This method is used by the member-management UI.
     */
    public java.util.List<Member> findAll()
            throws DatabaseException {

        return memberDAO.findAll();
    }

    /**
     * Finds a member by member ID.
     */
    public java.util.Optional<Member> findById(String memberId)
            throws DatabaseException {

        if (!Validator.isNotBlank(memberId)) {
            return java.util.Optional.empty();
        }

        return memberDAO.findById(memberId.trim());
    }

    /** Resolves an authenticated account to its member profile. */
    public java.util.Optional<Member> findByUserId(long userId)
            throws DatabaseException {
        if (userId <= 0) {
            return java.util.Optional.empty();
        }
        return memberDAO.findByUserId(userId);
    }

    public boolean updateStatus(String memberId, MemberStatus status)
            throws InvalidInputException, DatabaseException {
        if (!Validator.isNotBlank(memberId)) {
            throw new InvalidInputException("Member ID cannot be empty.");
        }
        if (status == null) {
            throw new InvalidInputException("Member status is required.");
        }
        return memberDAO.updateStatus(memberId.trim(), status);
    }

    /**
     * Finds members by their current status.
     */
    public java.util.List<Member> findByStatus(MemberStatus status)
            throws DatabaseException {

        if (status == null) {
            return new java.util.ArrayList<>();
        }

        return memberDAO.findByStatus(status);
    }

    /**
     * Convenience overload for UI code that supplies status text.
     */
    public java.util.List<Member> findByStatus(String status)
            throws DatabaseException {

        if (!Validator.isNotBlank(status)) {
            return new java.util.ArrayList<>();
        }

        try {
            return findByStatus(
                    MemberStatus.valueOf(
                            status.trim().toUpperCase()
                    )
            );
        } catch (IllegalArgumentException ex) {
            return new java.util.ArrayList<>();
        }
    }

    /**
     * Creates a Member object using the required class hierarchy.
     */
    public Member createMemberObject(
            MemberType memberType,
            String memberId,
            MemberStatus status,
            String fullName,
            String email,
            String phone) {

        return MemberFactory.createMember(
                memberType,
                memberId,
                status,
                fullName,
                email,
                phone
        );
    }

    /**
     * Validates registration input.
     */
    private void validateRegistrationInput(
            String username,
            String password,
            String fullName,
            String email,
            String phone,
            MemberType memberType,
            String department)
            throws InvalidInputException {

        if (!Validator.isValidUsername(username)) {
            throw new InvalidInputException(
                    "Username must contain 3-30 letters, digits, '_' or '.'."
            );
        }

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

        if (memberType == null) {
            throw new InvalidInputException(
                    "Member type is required."
            );
        }

        if (!Validator.isNotBlank(department)) {
            throw new InvalidInputException(
                    "Department cannot be empty."
            );
        }
    }

}
