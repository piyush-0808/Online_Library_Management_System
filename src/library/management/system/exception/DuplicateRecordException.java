package library.management.system.exception;

public class DuplicateRecordException extends LibraryException {

    private static final long serialVersionUID = 1L;

    public DuplicateRecordException(String message) {
        super(message);
    }
}