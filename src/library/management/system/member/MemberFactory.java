package library.management.system.member;

import library.management.system.model.MemberStatus;
import library.management.system.model.MemberType;

/**
 * Factory class for creating different Member objects.
 *
 * The factory returns the parent type Member.
 * This demonstrates polymorphism.
 */
public final class MemberFactory {

    private MemberFactory() {
        // Factory class should not be instantiated.
    }

    public static Member createMember(
            MemberType type,
            String memberId,
            MemberStatus status,
            String fullName,
            String email,
            String phone) {

        return switch (type) {
            case STUDENT -> new StudentMember(
                    memberId, status, fullName, email, phone);

            case FACULTY -> new FacultyMember(
                    memberId, status, fullName, email, phone);
        };
    }
}