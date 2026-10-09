package library.management.system.item;

/**
 * Interface for items that can be issued to members.
 */
public interface Borrowable {

    boolean isBorrowable();

    int getLoanDays();
}