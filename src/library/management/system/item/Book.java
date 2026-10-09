package library.management.system.item;

import library.management.system.model.ItemCategory;

/**
 * Normal library book.
 *
 * A Book can be borrowed and reserved.
 */
public class Book extends LibraryItem implements Borrowable, Reservable {

    public Book(
            int itemId,
            String title,
            String author,
            String isbn,
            ItemCategory category,
            int totalCopies,
            int availableCopies) {

        super(
                itemId,
                title,
                author,
                isbn,
                category,
                totalCopies,
                availableCopies);
    }

    @Override
    public String getItemType() {
        return "BOOK";
    }

    @Override
    public boolean isAvailable() {
        return getAvailableCopies() > 0;
    }

    @Override
    public boolean isBorrowable() {
        return true;
    }

    @Override
    public int getLoanDays() {
        return 14;
    }

    @Override
    public boolean isReservable() {
        return true;
    }
}