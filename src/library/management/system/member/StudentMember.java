package library.management.system.member;

import library.management.system.model.MemberStatus;
import library.management.system.model.MemberType;

/**
 * Student member.
 *
 * Business rules come from the class hierarchy rather than
 * checking a type string with if-else.
 */
public class StudentMember extends Member {

    private static final MemberType TYPE = MemberType.STUDENT;

    public StudentMember(
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
        return "Welcome, Student " + getFullName() + "!";
    }
}