package library.management.system.item;

import library.management.system.model.ItemCategory;

/**
 * Factory class for creating LibraryItem objects.
 *
 * The method returns the parent type LibraryItem,
 * demonstrating polymorphism.
 */
public final class ItemFactory {

    private ItemFactory() {
        // Factory class should not be instantiated.
    }

    public static LibraryItem createItem(
            String type,
            int itemId,
            String title,
            String author,
            String isbn,
            ItemCategory category,
            int totalCopies,
            int availableCopies) {

        return switch (type.toUpperCase()) {

            case "BOOK" -> new Book(
                    itemId,
                    title,
                    author,
                    isbn,
                    category,
                    totalCopies,
                    availableCopies);

            case "REFERENCE_BOOK" -> new ReferenceBook(
                    itemId,
                    title,
                    author,
                    isbn,
                    category,
                    totalCopies,
                    availableCopies);

            case "MAGAZINE" -> new Magazine(
                    itemId,
                    title,
                    author,
                    isbn,
                    category,
                    totalCopies,
                    availableCopies);

            default -> throw new IllegalArgumentException(
                    "Unknown library item type: " + type);
        };
    }
}