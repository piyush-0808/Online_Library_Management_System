package library.management.system.service;

import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

import library.management.system.dao.IssueDAO;
import library.management.system.exception.BorrowLimitExceededException;
import library.management.system.exception.BookNotAvailableException;
import library.management.system.exception.FinePendingException;
import library.management.system.exception.InvalidInputException;
import library.management.system.exception.ItemNotBorrowableException;
import library.management.system.exception.LibraryException;
import library.management.system.exception.MemberBlockedException;
import library.management.system.item.LibraryItem;
import library.management.system.item.ReferenceBook;
import library.management.system.member.Member;
import library.management.system.model.Issue;
import library.management.system.model.IssueStatus;

/** Business rules for issuing and returning items. Persistence is in IssueDAO. */
public class IssueService {

    /** Coordinates same-book issue/return operations within this JVM. */
    private static final ConcurrentHashMap<Long, ReentrantLock> BOOK_LOCKS =
            new ConcurrentHashMap<>();

    private final IssueDAO issueDAO = new IssueDAO();
    private final MemberService memberService = new MemberService();
    private final BookService bookService = new BookService();
    private final FineService fineService = new FineService();
    private final ReservationService reservationService = new ReservationService();

    public Issue issueBook(
            String memberId,
            long bookId,
            long librarianId)
            throws LibraryException {
        return issueBook(memberId, bookId, librarianId, null);
    }

    /** Issues a book for a duration chosen in the issue screen. */
    public Issue issueBook(
            String memberId,
            long bookId,
            long librarianId,
            int loanDays)
            throws LibraryException {
        return issueBook(memberId, bookId, librarianId, Integer.valueOf(loanDays));
    }

    private Issue issueBook(
            String memberId,
            long bookId,
            long librarianId,
            Integer requestedLoanDays)
            throws LibraryException {
        validateIssueInput(memberId, bookId, librarianId);
        if (bookId > Integer.MAX_VALUE) {
            throw new InvalidInputException("Invalid book ID.");
        }

        ReentrantLock bookLock = acquireBookLock(bookId);
        try {
            return issueBookWithBookLock(
                    memberId, bookId, librarianId, requestedLoanDays);
        } finally {
            bookLock.unlock();
        }
    }

    private Issue issueBookWithBookLock(
            String memberId,
            long bookId,
            long librarianId,
            Integer requestedLoanDays)
            throws LibraryException {

        Member member = memberService.findById(memberId.trim())
                .orElseThrow(() -> new InvalidInputException(
                        "Member not found: " + memberId));
        try {
            member.ensureCanBorrow();
        } catch (LibraryException ex) {
            throw new MemberBlockedException(ex.getMessage());
        }
        fineService.ensureBorrowingAllowed(memberId.trim());

        if (issueDAO.countActiveByMemberId(memberId.trim())
                >= member.getMaxBooks()) {
            throw new BorrowLimitExceededException(
                    "Borrowing limit reached. Maximum allowed: "
                            + member.getMaxBooks());
        }

        LibraryItem book = bookService.findById((int) bookId)
                .orElseThrow(() -> new InvalidInputException(
                        "Book not found: " + bookId));
        if (book instanceof ReferenceBook) {
            throw new ItemNotBorrowableException(
                    "Reference books cannot be issued.");
        }
        boolean hasHeldCopy = reservationService.hasReadyReservation(
                memberId.trim(), bookId);
        if (book.getAvailableCopies() <= 0 && !hasHeldCopy) {
            throw new BookNotAvailableException(
                    "No copy of this book is currently available.");
        }
        if (issueDAO.hasActiveIssue(memberId.trim(), bookId)) {
            throw new InvalidInputException(
                    "This member already has this book issued.");
        }

        LocalDate issueDate = LocalDate.now();
        int loanDays = requestedLoanDays == null
                ? member.getLoanDays() : requestedLoanDays;
        if (loanDays < 1) {
            throw new InvalidInputException(
                    "Loan duration must be at least one day.");
        }
        LocalDate dueDate = issueDate.plusDays(loanDays);
        return issueDAO.issueAtomically(
                memberId.trim(), bookId, librarianId, issueDate, dueDate,
                member.getMaxBooks());
    }

    public ReturnResult returnBook(long issueId, long librarianId)
            throws LibraryException {
        if (issueId <= 0) {
            throw new InvalidInputException("Invalid issue ID.");
        }
        if (librarianId <= 0) {
            throw new InvalidInputException("Invalid librarian ID.");
        }

        Issue issue = issueDAO.findById(issueId)
                .orElseThrow(() -> new InvalidInputException(
                        "Issue record not found."));
        ReentrantLock bookLock = acquireBookLock(issue.getBookId());
        try {
            // Re-read after acquiring the lock so concurrent returns cannot
            // rely on the stale status observed before waiting.
            issue = issueDAO.findById(issueId)
                    .orElseThrow(() -> new InvalidInputException(
                            "Issue record not found."));
            if (issue.getStatus() == IssueStatus.RETURNED) {
                throw new InvalidInputException(
                        "This book has already been returned.");
            }
            Member member = memberService.findById(issue.getMemberId())
                    .orElseThrow(() -> new InvalidInputException(
                            "Member associated with issue not found."));

            IssueDAO.ReturnData returned = issueDAO.returnAtomically(
                    issueId,
                    librarianId,
                    LocalDate.now(),
                    member.calculateFine(1));
            return new ReturnResult(
                    issueId,
                    returned.memberId(),
                    returned.bookId(),
                    returned.returnDate(),
                    returned.daysLate(),
                    returned.fineAmount(),
                    returned.reservedForMemberId());
        } finally {
            bookLock.unlock();
        }
    }

    public List<Issue> getIssuesByMember(String memberId)
            throws library.management.system.exception.DatabaseException {
        if (memberId == null || memberId.isBlank()) {
            return List.of();
        }
        return issueDAO.findByMemberId(memberId.trim());
    }

    public int refreshOverdueStatuses()
            throws library.management.system.exception.DatabaseException {
        return issueDAO.refreshOverdueStatuses();
    }

    public List<Issue> getActiveIssues()
            throws library.management.system.exception.DatabaseException {
        return issueDAO.findAll().stream()
                .filter(issue -> issue.getStatus() == IssueStatus.ISSUED
                        || issue.getStatus() == IssueStatus.OVERDUE)
                .sorted((first, second) ->
                        first.getDueDate().compareTo(second.getDueDate()))
                .toList();
    }

    private void validateIssueInput(
            String memberId,
            long bookId,
            long librarianId)
            throws InvalidInputException {
        if (memberId == null || memberId.isBlank()) {
            throw new InvalidInputException("Member ID is required.");
        }
        if (bookId <= 0) {
            throw new InvalidInputException("Invalid book ID.");
        }
        if (librarianId <= 0) {
            throw new InvalidInputException("Invalid librarian ID.");
        }
    }

    private static ReentrantLock acquireBookLock(long bookId) {
        ReentrantLock lock = BOOK_LOCKS.computeIfAbsent(
                bookId, ignored -> new ReentrantLock());
        lock.lock();
        return lock;
    }

    public static final class ReturnResult {
        private final long issueId;
        private final String memberId;
        private final long bookId;
        private final LocalDate returnDate;
        private final int daysLate;
        private final double fineAmount;
        private final String reservedForMemberId;

        public ReturnResult(long issueId, String memberId, long bookId,
                LocalDate returnDate, int daysLate, double fineAmount,
                String reservedForMemberId) {
            this.issueId = issueId;
            this.memberId = memberId;
            this.bookId = bookId;
            this.returnDate = returnDate;
            this.daysLate = daysLate;
            this.fineAmount = fineAmount;
            this.reservedForMemberId = reservedForMemberId;
        }

        public long getIssueId() { return issueId; }
        public String getMemberId() { return memberId; }
        public long getBookId() { return bookId; }
        public LocalDate getReturnDate() { return returnDate; }
        public int getDaysLate() { return daysLate; }
        public double getFineAmount() { return fineAmount; }
        public String getReservedForMemberId() { return reservedForMemberId; }
        public boolean hasFine() { return fineAmount > 0; }
    }
}
