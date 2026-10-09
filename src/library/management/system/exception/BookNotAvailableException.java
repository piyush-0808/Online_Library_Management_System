package library.management.system.exception;

public class BookNotAvailableException extends LibraryException {

    private static final long serialVersionUID = 1L;

    public BookNotAvailableException(String message) {
        super(message);
    }
}