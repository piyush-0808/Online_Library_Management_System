package library.management.system.service;

import java.util.List;
import java.util.Optional;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import library.management.system.dao.FineDAO;
import library.management.system.exception.DatabaseException;
import library.management.system.exception.InvalidInputException;
import library.management.system.model.Fine;
import library.management.system.model.Issue;
import library.management.system.model.IssueStatus;
import library.management.system.member.Member;
import library.management.system.util.Validator;

public class FineService {

    private final FineDAO fineDAO;

    public FineService() {
        this.fineDAO = new FineDAO();
    }

    /** Calculates and persists all active overdue fines on a bounded worker pool. */
    public int refreshOverdueFinesInParallel()
            throws DatabaseException {
        List<Issue> overdue = new IssueService().getActiveIssues().stream()
                .filter(issue -> issue.getStatus() == IssueStatus.OVERDUE
                        || issue.getDueDate().isBefore(LocalDate.now()))
                .toList();
        if (overdue.isEmpty()) {
            return 0;
        }
        int workers = Math.min(overdue.size(),
                Math.max(2, Runtime.getRuntime().availableProcessors()));
        ExecutorService executor = Executors.newFixedThreadPool(workers);
        try {
            List<java.util.concurrent.Callable<Void>> jobs = overdue.stream()
                    .<java.util.concurrent.Callable<Void>>map(issue -> () -> {
                        int daysLate = (int) ChronoUnit.DAYS.between(
                                issue.getDueDate(), LocalDate.now());
                        if (daysLate > 0) {
                            Member member = new MemberService()
                                    .findById(issue.getMemberId())
                                    .orElseThrow(() -> new DatabaseException(
                                            "Member not found for overdue issue "
                                                    + issue.getIssueId()));
                            double amount = member.calculateFine(daysLate);
                            fineDAO.upsertOverdueFine(issue.getIssueId(),
                                    issue.getMemberId(), daysLate, amount);
                        }
                        return null;
                    }).toList();
            List<Future<Void>> results = executor.invokeAll(jobs);
            for (Future<Void> result : results) {
                try {
                    result.get();
                } catch (ExecutionException e) {
                    Throwable cause = e.getCause();
                    if (cause instanceof DatabaseException databaseException) {
                        throw databaseException;
                    }
                    throw new DatabaseException(
                            "Could not calculate overdue fines.", cause);
                }
            }
            return overdue.size();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new DatabaseException("Overdue fine calculation was interrupted.", e);
        } finally {
            executor.shutdownNow();
        }
    }

    /**
     * Returns a fine by its ID.
     */
    public Optional<Fine> findById(long fineId)
            throws DatabaseException,
                   InvalidInputException {

        validateFineId(fineId);

        return fineDAO.findById(fineId);
    }

    /**
     * Returns all fines.
     */
    public List<Fine> findAll()
            throws DatabaseException {

        return fineDAO.findAll();
    }

    /**
     * Returns all fines belonging to a member.
     */
    public List<Fine> findByMemberId(
            String memberId)
            throws DatabaseException,
                   InvalidInputException {

        validateMemberId(memberId);

        return fineDAO.findByMemberId(memberId);
    }

    /**
     * Returns all unpaid fines of a member.
     */
    public List<Fine> findUnpaidByMemberId(
            String memberId)
            throws DatabaseException,
                   InvalidInputException {

        validateMemberId(memberId);

        return fineDAO.findUnpaidByMemberId(memberId);
    }

    /**
     * Returns total unpaid fine amount of a member.
     */
    public double getUnpaidAmount(
            String memberId)
            throws DatabaseException,
                   InvalidInputException {

        validateMemberId(memberId);

        return fineDAO.getUnpaidAmount(memberId);
    }

    /**
     * Checks whether the member has more than
     * Rs. 100 unpaid fine.
     */
    public boolean hasBorrowingBlock(
            String memberId)
            throws DatabaseException,
                   InvalidInputException {

        return getUnpaidAmount(memberId) > 100.0;
    }

    /**
     * Marks an unpaid fine as paid.
     */
    public boolean payFine(
            long fineId,
            long librarianId)
            throws DatabaseException,
                   InvalidInputException {

        validateFineId(fineId);
        validateLibrarianId(librarianId);

        Optional<Fine> fineOptional =
                fineDAO.findById(fineId);

        if (fineOptional.isEmpty()) {
            throw new InvalidInputException(
                    "Fine record not found."
            );
        }

        Fine fine = fineOptional.get();

        if (fine.isPaid()) {
            throw new InvalidInputException(
                    "This fine has already been paid."
            );
        }

        return fineDAO.markAsPaid(
                fineId,
                librarianId
        );
    }

    /**
     * Finds the fine generated for a particular issue.
     */
    public Optional<Fine> findByIssueId(
            long issueId)
            throws DatabaseException,
                   InvalidInputException {

        if (issueId <= 0) {
            throw new InvalidInputException(
                    "Invalid issue ID."
            );
        }

        return fineDAO.findByIssueId(issueId);
    }

    /**
     * Creates a fine manually when required by
     * an administrative operation.
     */
    public Fine createFine(
            long issueId,
            String memberId,
            int daysLate,
            double amount)
            throws DatabaseException,
                   InvalidInputException {

        if (issueId <= 0) {
            throw new InvalidInputException(
                    "Invalid issue ID."
            );
        }

        validateMemberId(memberId);

        if (daysLate <= 0) {
            throw new InvalidInputException(
                    "Days late must be greater than zero."
            );
        }

        if (amount <= 0) {
            throw new InvalidInputException(
                    "Fine amount must be greater than zero."
            );
        }

        /*
         * A single issue should have at most one fine.
         */
        if (fineDAO.findByIssueId(issueId).isPresent()) {
            throw new InvalidInputException(
                    "A fine already exists for this issue."
            );
        }

        Fine fine = new Fine(
                0,
                issueId,
                memberId,
                daysLate,
                amount,
                "UNPAID",
                null,
                null
        );

        return fineDAO.save(fine);
    }

    /**
     * Calculates whether borrowing should be blocked.
     */
    public void ensureBorrowingAllowed(
            String memberId)
            throws DatabaseException,
                   InvalidInputException {

        double unpaidAmount =
                getUnpaidAmount(memberId);

        if (unpaidAmount > 100.0) {
            throw new InvalidInputException(
                    String.format(
                            "Borrowing blocked. "
                            + "Outstanding fine: Rs %.2f",
                            unpaidAmount
                    )
            );
        }
    }

    /**
     * Validates a member ID.
     */
    private void validateMemberId(
            String memberId)
            throws InvalidInputException {

        if (!Validator.isNotBlank(memberId)) {
            throw new InvalidInputException(
                    "Member ID cannot be empty."
            );
        }
    }

    /**
     * Validates a fine ID.
     */
    private void validateFineId(
            long fineId)
            throws InvalidInputException {

        if (fineId <= 0) {
            throw new InvalidInputException(
                    "Invalid fine ID."
            );
        }
    }

    /**
     * Validates librarian ID.
     */
    private void validateLibrarianId(
            long librarianId)
            throws InvalidInputException {

        if (librarianId <= 0) {
            throw new InvalidInputException(
                    "Invalid librarian ID."
            );
        }
    }
}
