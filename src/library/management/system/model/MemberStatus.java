package library.management.system.model;

public enum MemberStatus {

    PENDING("Pending"),
    ACTIVE("Active"),
    BLOCKED("Blocked");

    private final String displayName;

    MemberStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    @Override
    public String toString() {
        return displayName;
    }
}