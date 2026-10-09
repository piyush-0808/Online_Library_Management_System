package library.management.system.item;

import library.management.system.model.ItemCategory;

/**
 * Reference books can only be read inside the library.
 *
 * They can NEVER be issued.
 */
public class ReferenceBook extends LibraryItem implements Reservable {

    public ReferenceBook(
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
        return "REFERENCE_BOOK";
    }

    @Override
    public boolean isAvailable() {
        return getAvailableCopies() > 0;
    }

    @Override
    public boolean isReservable() {
        return false;
    }
}