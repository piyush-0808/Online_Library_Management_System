package library.management.system.model;

public enum MemberType {

    STUDENT("Student", 3, 14, 5.0),
    FACULTY("Faculty", 10, 30, 2.0);

    private final String displayName;
    private final int maxBooks;
    private final int loanDays;
    private final double finePerDay;

    MemberType(
            String displayName,
            int maxBooks,
            int loanDays,
            double finePerDay) {

        this.displayName = displayName;
        this.maxBooks = maxBooks;
        this.loanDays = loanDays;
        this.finePerDay = finePerDay;
    }

    public String getDisplayName() {
        return displayName;
    }

    public int getMaxBooks() {
        return maxBooks;
    }

    public int getLoanDays() {
        return loanDays;
    }

    public double getFinePerDay() {
        return finePerDay;
    }

    @Override
    public String toString() {
        return displayName;
    }
}
