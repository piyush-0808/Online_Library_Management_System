package library.management.system.model;

public enum IssueStatus {

    ISSUED("Issued"),
    RETURNED("Returned"),
    OVERDUE("Overdue");

    private final String displayName;

    IssueStatus(String displayName) {
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