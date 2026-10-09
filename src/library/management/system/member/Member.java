package library.management.system.member;

import java.time.LocalDate;

import library.management.system.exception.LibraryException;
import library.management.system.model.MemberStatus;
import library.management.system.model.Person;
import library.management.system.model.Role;

public abstract class Member extends Person {

    private String memberId;
    private MemberStatus status;

    // Additional member information from the database.
    private String department;
    private LocalDate joinedDate;
    private Long approvedBy;
    private LocalDate approvedAt;

    /*
     * Existing constructor kept for compatibility with
     * StudentMember and FacultyMember.
     */
    protected Member(
            String memberId,
            MemberStatus status,
            String fullName,
            String email,
            String phone) {

        super(fullName, email, phone);

        this.memberId = memberId;
        this.status = status;
    }

    /*
     * Extended constructor used when complete member information
     * is available from the database.
     */
    protected Member(
            String memberId,
            String fullName,
            String email,
            String phone,
            MemberStatus status,
            String department,
            LocalDate joinedDate) {

        super(fullName, email, phone);

        this.memberId = memberId;
        this.status = status;
        this.department = department;
        this.joinedDate = joinedDate;
    }

    @Override
    public Role getRole() {
        return Role.MEMBER;
    }

    public String getMemberId() {
        return memberId;
    }

    public void setMemberId(String memberId) {
        this.memberId = memberId;
    }

    public MemberStatus getStatus() {
        return status;
    }

    public void setStatus(MemberStatus status) {
        this.status = status;
    }

    public String getDepartment() {
        return department;
    }

    public void setDepartment(String department) {
        this.department = department;
    }

    public LocalDate getJoinedDate() {
        return joinedDate;
    }

    public void setJoinedDate(LocalDate joinedDate) {
        this.joinedDate = joinedDate;
    }

    public Long getApprovedBy() {
        return approvedBy;
    }

    public void setApprovedBy(Long approvedBy) {
        this.approvedBy = approvedBy;
    }

    public LocalDate getApprovedAt() {
        return approvedAt;
    }

    public void setApprovedAt(LocalDate approvedAt) {
        this.approvedAt = approvedAt;
    }

    /*
     * Business rule: pending and blocked members cannot borrow.
     */
    public void ensureCanBorrow() throws LibraryException {

        if (status == MemberStatus.PENDING) {
            throw new LibraryException(
                    "Your membership is still pending approval."
            );
        }

        if (status == MemberStatus.BLOCKED) {
            throw new LibraryException(
                    "Your membership is blocked."
            );
        }
    }

    public abstract int getMaxBooks();

    public abstract int getLoanDays();

    public abstract double calculateFine(int overdueDays);
}