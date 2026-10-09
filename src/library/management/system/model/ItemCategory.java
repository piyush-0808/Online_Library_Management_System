package library.management.system.model;

public enum ItemCategory {

    BOOK("Book"),
    REFERENCE("Reference"),
    MAGAZINE("Magazine");

    private final String displayName;

    ItemCategory(String displayName) {
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