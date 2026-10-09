package library.management.system.member;

import library.management.system.model.MemberStatus;
import library.management.system.model.MemberType;

/**
 * Faculty member.
 *
 * Faculty members have different borrowing rules from students.
 */
public class FacultyMember extends Member {

    private static final MemberType TYPE = MemberType.FACULTY;

    public FacultyMember(
            String memberId,
            MemberStatus status,
            String fullName,
            String email,
            String phone) {

        super(memberId, status, fullName, email, phone);
    }

    public MemberType getMemberType() {
        return TYPE;
    }

    @Override
    public int getMaxBooks() {
        return TYPE.getMaxBooks();
    }

    @Override
    public int getLoanDays() {
        return TYPE.getLoanDays();
    }

    @Override
    public double calculateFine(int daysLate) {
        if (daysLate <= 0) {
            return 0.0;
        }

        return daysLate * TYPE.getFinePerDay();
    }

    @Override
    public String getWelcomeMessage() {
        return "Welcome, Faculty " + getFullName() + "!";
    }
}