package library.management.system.model;

import java.time.LocalDate;

public class Fine {

    private long fineId;
    private long issueId;
    private String memberId;
    private int daysLate;
    private double amount;
    private String status;
    private LocalDate paidAt;
    private Long paidBy;

    public Fine() {
    }

    public Fine(
            long fineId,
            long issueId,
            String memberId,
            int daysLate,
            double amount,
            String status,
            LocalDate paidAt,
            Long paidBy) {

        this.fineId = fineId;
        this.issueId = issueId;
        this.memberId = memberId;
        this.daysLate = daysLate;
        this.amount = amount;
        this.status = status;
        this.paidAt = paidAt;
        this.paidBy = paidBy;
    }

    public long getFineId() {
        return fineId;
    }

    public void setFineId(long fineId) {
        this.fineId = fineId;
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

    public int getDaysLate() {
        return daysLate;
    }

    public void setDaysLate(int daysLate) {
        this.daysLate = daysLate;
    }

    public double getAmount() {
        return amount;
    }

    public void setAmount(double amount) {
        this.amount = amount;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDate getPaidAt() {
        return paidAt;
    }

    public void setPaidAt(LocalDate paidAt) {
        this.paidAt = paidAt;
    }

    public Long getPaidBy() {
        return paidBy;
    }

    public void setPaidBy(Long paidBy) {
        this.paidBy = paidBy;
    }

    public boolean isPaid() {
        return "PAID".equalsIgnoreCase(status);
    }

    public boolean isUnpaid() {
        return "UNPAID".equalsIgnoreCase(status);
    }

    @Override
    public String toString() {
        return "Fine{"
                + "fineId=" + fineId
                + ", issueId=" + issueId
                + ", memberId='" + memberId + '\''
                + ", daysLate=" + daysLate
                + ", amount=" + amount
                + ", status='" + status + '\''
                + ", paidAt=" + paidAt
                + ", paidBy=" + paidBy
                + '}';
    }
}