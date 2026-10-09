package library.management.system.model;

import java.time.LocalDate;

/**
 * Represents one book issue/borrowing transaction.
 */
public class Issue {

    private long issueId;
    private String memberId;
    private long bookId;

    private LocalDate issueDate;
    private LocalDate dueDate;
    private LocalDate returnDate;

    private IssueStatus status;

    private long issuedBy;
    private Long returnedBy;

    public Issue() {
    }

    public Issue(
            long issueId,
            String memberId,
            long bookId,
            LocalDate issueDate,
            LocalDate dueDate,
            LocalDate returnDate,
            IssueStatus status,
            long issuedBy,
            Long returnedBy) {

        this.issueId = issueId;
        this.memberId = memberId;
        this.bookId = bookId;
        this.issueDate = issueDate;
        this.dueDate = dueDate;
        this.returnDate = returnDate;
        this.status = status;
        this.issuedBy = issuedBy;
        this.returnedBy = returnedBy;
    }

    public long getIssueId() {
        return issueId;
    }

    public void setIssueId(long issueId) {
        this.issueId = issueId;
    }

    public String getMemberId() {
        return memberId;
    }

    public void setMemberId(String memberId) {
        this.memberId = memberId;
    }

    public long getBookId() {
        return bookId;
    }

    public void setBookId(long bookId) {
        this.bookId = bookId;
    }

    public LocalDate getIssueDate() {
        return issueDate;
    }

    public void setIssueDate(LocalDate issueDate) {
        this.issueDate = issueDate;
    }

    public LocalDate getDueDate() {
        return dueDate;
    }

    public void setDueDate(LocalDate dueDate) {
        this.dueDate = dueDate;
    }

    public LocalDate getReturnDate() {
        return returnDate;
    }

    public void setReturnDate(LocalDate returnDate) {
        this.returnDate = returnDate;
    }

    public IssueStatus getStatus() {
        return status;
    }

    public void setStatus(IssueStatus status) {
        this.status = status;
    }

    public long getIssuedBy() {
        return issuedBy;
    }

    public void setIssuedBy(long issuedBy) {
        this.issuedBy = issuedBy;
    }

    public Long getReturnedBy() {
        return returnedBy;
    }

    public void setReturnedBy(Long returnedBy) {
        this.returnedBy = returnedBy;
    }

    /**
     * Returns true when the issue is currently active.
     */
    public boolean isActive() {
        return status == IssueStatus.ISSUED
                || status == IssueStatus.OVERDUE;
    }

    /**
     * Returns true when the book has been returned.
     */
    public boolean isReturned() {
        return status == IssueStatus.RETURNED;
    }

    @Override
    public String toString() {
        return "Issue{"
                + "issueId=" + issueId
                + ", memberId='" + memberId + '\''
                + ", bookId=" + bookId
                + ", issueDate=" + issueDate
                + ", dueDate=" + dueDate
                + ", returnDate=" + returnDate
                + ", status=" + status
                + ", issuedBy=" + issuedBy
                + ", returnedBy=" + returnedBy
                + '}';
    }
}