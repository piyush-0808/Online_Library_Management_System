package library.management.system.exception;

public class InvalidInputException extends LibraryException {

    private static final long serialVersionUID = 1L;

    public InvalidInputException(String message) {
        super(message);
    }
}