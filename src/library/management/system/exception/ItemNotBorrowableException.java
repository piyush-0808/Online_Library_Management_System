package library.management.system.exception;

public class ItemNotBorrowableException extends LibraryException {

    private static final long serialVersionUID = 1L;

    public ItemNotBorrowableException(String message) {
        super(message);
    }
}