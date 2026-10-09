package library.management.system.model;

import java.time.LocalDate;

public class Reservation {

    private long reservationId;
    private String memberId;
    private long bookId;
    private LocalDate reservationDate;
    private String status;

    public Reservation() {
    }

    public Reservation(
            long reservationId,
            String memberId,
            long bookId,
            LocalDate reservationDate,
            String status) {

        this.reservationId = reservationId;
        this.memberId = memberId;
        this.bookId = bookId;
        this.reservationDate = reservationDate;
        this.status = status;
    }

    public long getReservationId() {
        return reservationId;
    }

    public void setReservationId(long reservationId) {
        this.reservationId = reservationId;
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

    public LocalDate getReservationDate() {
        return reservationDate;
    }

    public void setReservationDate(LocalDate reservationDate) {
        this.reservationDate = reservationDate;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public boolean isWaiting() {
        return "WAITING".equalsIgnoreCase(status);
    }

    public boolean isFulfilled() {
        return "FULFILLED".equalsIgnoreCase(status);
    }

    /** True when a copy has been held for this member to check out. */
    public boolean isReady() {
        return "READY".equalsIgnoreCase(status);
    }

    public boolean isCancelled() {
        return "CANCELLED".equalsIgnoreCase(status);
    }

    @Override
    public String toString() {
        return "Reservation{"
                + "reservationId=" + reservationId
                + ", memberId='" + memberId + '\''
                + ", bookId=" + bookId
                + ", reservationDate=" + reservationDate
                + ", status='" + status + '\''
                + '}';
    }
}
