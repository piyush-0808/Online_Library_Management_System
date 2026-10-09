package library.management.system.service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.PriorityQueue;
import java.util.Queue;

import library.management.system.dao.ReservationDAO;
import library.management.system.dao.IssueDAO;
import library.management.system.exception.DatabaseException;
import library.management.system.exception.InvalidInputException;
import library.management.system.exception.MemberBlockedException;
import library.management.system.exception.LibraryException;
import library.management.system.model.Reservation;
import library.management.system.item.Reservable;
import library.management.system.util.Validator;

public class ReservationService {

    private final ReservationDAO reservationDAO;
    private final IssueDAO issueDAO;
    private final MemberService memberService;
    private final BookService bookService;

    public ReservationService() {
        this.reservationDAO = new ReservationDAO();
        this.issueDAO = new IssueDAO();
        this.memberService = new MemberService();
        this.bookService = new BookService();
    }

    /**
     * Creates a reservation for a book.
     *
     * A member can reserve a book only when:
     * - member exists
     * - member is active
     * - book exists
     * - book is currently unavailable
     * - member does not already have the book
     * - member does not already have a waiting reservation
     */
    public Reservation reserveBook(
            String memberId,
            long bookId)
            throws LibraryException {

        validateMemberId(memberId);
        validateBookId(bookId);

        /*
         * Load member through MemberService.
         */
        var memberOptional =
                memberService.findById(memberId);

        if (memberOptional.isEmpty()) {
            throw new InvalidInputException(
                    "Member not found."
            );
        }

        var member = memberOptional.get();

        /*
         * Pending and blocked members cannot reserve.
         */
        try {
            member.ensureCanBorrow();
        } catch (LibraryException e) {
            throw new MemberBlockedException(
                    e.getMessage()
            );
        }

        /*
         * Book must exist.
         */
        var bookOptional =
                bookService.findById(
                        (int) bookId
                );

        if (bookOptional.isEmpty()) {
            throw new InvalidInputException(
                    "Book not found."
            );
        }

        var book = bookOptional.get();

        if (issueDAO.hasActiveIssue(memberId, bookId)) {
            throw new InvalidInputException(
                    "You already have this book issued.");
        }

        if (!(book instanceof Reservable)
                || !((Reservable) book).isReservable()) {
            throw new InvalidInputException(
                    "This item cannot be reserved."
            );
        }

        /*
         * If a copy is already available, reservation
         * should not be created.
         */
        if (book.getAvailableCopies() > 0) {
            throw new InvalidInputException(
                    "Reservation is not allowed because "
                    + "a copy of this book is currently available."
            );
        }

        Reservation reservation =
                new Reservation(
                        0,
                        memberId,
                        bookId,
                        LocalDate.now(),
                        "WAITING"
                );

        return reservationDAO.createWaitingAtomically(reservation);
    }

    /**
     * Cancels an existing reservation.
     */
    public boolean cancelReservation(
            long reservationId,
            String memberId)
            throws LibraryException {

        validateReservationId(
                reservationId
        );

        validateMemberId(
                memberId
        );

        Optional<Reservation> optional =
                reservationDAO.findById(
                        reservationId
                );

        if (optional.isEmpty()) {
            throw new InvalidInputException(
                    "Reservation not found."
            );
        }

        Reservation reservation =
                optional.get();

        /*
         * A member can cancel only their own reservation.
         */
        if (!memberId.equals(
                reservation.getMemberId())) {

            throw new InvalidInputException(
                    "You cannot cancel another member's reservation."
            );
        }

        if (reservation.isCancelled()) {
            throw new InvalidInputException(
                    "Reservation is already cancelled."
            );
        }

        if (reservation.isFulfilled()) {
            throw new InvalidInputException(
                    "A fulfilled reservation cannot be cancelled."
            );
        }

        return reservationDAO.cancel(reservationId, memberId);
    }

    /**
     * Cancels a reservation by librarian/admin.
     */
    public boolean cancelReservationByLibrarian(
            long reservationId)
            throws LibraryException {

        validateReservationId(
                reservationId
        );

        Optional<Reservation> optional =
                reservationDAO.findById(
                        reservationId
                );

        if (optional.isEmpty()) {
            throw new InvalidInputException(
                    "Reservation not found."
            );
        }

        Reservation reservation =
                optional.get();

        if (reservation.isCancelled()) {
            throw new InvalidInputException(
                    "Reservation is already cancelled."
            );
        }

        if (reservation.isFulfilled()) {
            throw new InvalidInputException(
                    "Fulfilled reservation cannot be cancelled."
            );
        }

        return reservationDAO.cancel(
                reservationId
        );
    }

    /**
     * Returns all reservations.
     */
    public List<Reservation> findAll()
            throws DatabaseException {

        return reservationDAO.findAll();
    }

    /**
     * Finds a reservation by ID.
     */
    public Optional<Reservation> findById(
            long reservationId)
            throws DatabaseException,
                   InvalidInputException {

        validateReservationId(
                reservationId
        );

        return reservationDAO.findById(
                reservationId
        );
    }

    /**
     * Returns reservations of a member.
     */
    public List<Reservation> findByMemberId(
            String memberId)
            throws DatabaseException,
                   InvalidInputException {

        validateMemberId(memberId);

        return reservationDAO.findByMemberId(
                memberId
        );
    }

    /**
     * Returns waiting reservations for a book
     * in FIFO order.
     */
    public List<Reservation> getWaitingQueue(
            long bookId)
            throws DatabaseException,
                   InvalidInputException {

        validateBookId(bookId);

        Queue<Reservation> queue = new PriorityQueue<>(
                Comparator.comparing(
                                Reservation::getReservationDate,
                                Comparator.nullsLast(Comparator.naturalOrder()))
                        .thenComparingLong(Reservation::getReservationId));
        queue.addAll(reservationDAO.findWaitingByBookId(bookId));
        List<Reservation> ordered = new ArrayList<>(queue.size());
        while (!queue.isEmpty()) {
            ordered.add(queue.remove());
        }
        return List.copyOf(ordered);
    }

    /**
     * Returns the first member waiting for a book.
     */
    public Optional<Reservation> getNextReservation(
            long bookId)
            throws DatabaseException,
                   InvalidInputException {

        validateBookId(bookId);

        return reservationDAO.findFirstWaiting(
                bookId
        );
    }

    /**
     * Fulfills the first waiting reservation
     * when a copy becomes available.
     */
    public Optional<Reservation> fulfillNextReservation(
            long bookId)
            throws DatabaseException,
                   InvalidInputException {

        validateBookId(bookId);

        return reservationDAO.fulfillNextAtomically(bookId);
    }

    public boolean hasReadyReservation(String memberId, long bookId)
            throws DatabaseException, InvalidInputException {
        validateMemberId(memberId);
        validateBookId(bookId);
        return reservationDAO.hasReadyReservation(memberId.trim(), bookId);
    }

    /**
     * Checks whether a member has a waiting
     * reservation for a particular book.
     */
    public boolean hasWaitingReservation(
            String memberId,
            long bookId)
            throws DatabaseException,
                   InvalidInputException {

        validateMemberId(memberId);
        validateBookId(bookId);

        return reservationDAO.hasWaitingReservation(
                memberId,
                bookId
        );
    }

    private void validateMemberId(
            String memberId)
            throws InvalidInputException {

        if (!Validator.isNotBlank(memberId)) {
            throw new InvalidInputException(
                    "Member ID cannot be empty."
            );
        }
    }

    private void validateBookId(
            long bookId)
            throws InvalidInputException {

        if (bookId <= 0) {
            throw new InvalidInputException(
                    "Invalid book ID."
            );
        }
    }

    private void validateReservationId(
            long reservationId)
            throws InvalidInputException {

        if (reservationId <= 0) {
            throw new InvalidInputException(
                    "Invalid reservation ID."
            );
        }
    }
}
