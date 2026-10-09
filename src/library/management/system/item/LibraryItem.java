package library.management.system.item;

import library.management.system.model.ItemCategory;

/**
 * Abstract base class for all library items.
 *
 * Demonstrates:
 * - Abstraction
 * - Encapsulation
 * - Inheritance
 */
public abstract class LibraryItem {

    private int itemId;
    private String title;
    private String author;
    private String isbn;
    private ItemCategory category;

    private int totalCopies;
    private int availableCopies;

    private String publisher;
    private int publicationYear;
    private String description;

    protected LibraryItem(
            int itemId,
            String title,
            String author,
            String isbn,
            ItemCategory category,
            int totalCopies,
            int availableCopies) {

        this.itemId = itemId;
        this.title = title;
        this.author = author;
        this.isbn = isbn;
        this.category = category;

        this.totalCopies = Math.max(0, totalCopies);
        this.availableCopies = Math.max(0,
                Math.min(availableCopies, this.totalCopies));

        this.publisher = "";
        this.publicationYear = 0;
        this.description = "";
    }

    // =========================
    // Abstract methods
    // =========================

    /**
     * Returns the display type of this library item.
     */
    public abstract String getItemType();

    /**
     * Returns whether this item can currently be borrowed.
     */
    public abstract boolean isAvailable();

    // =========================
    // Getters and Setters
    // =========================

    public int getItemId() {
        return itemId;
    }

    public void setItemId(int itemId) {
        this.itemId = itemId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public String getIsbn() {
        return isbn;
    }

    public void setIsbn(String isbn) {
        this.isbn = isbn;
    }

    public ItemCategory getCategory() {
        return category;
    }

    public void setCategory(ItemCategory category) {
        this.category = category;
    }

    public int getTotalCopies() {
        return totalCopies;
    }

    public void setTotalCopies(int totalCopies) {
        this.totalCopies = Math.max(0, totalCopies);

        if (availableCopies > this.totalCopies) {
            availableCopies = this.totalCopies;
        }
    }

    public int getAvailableCopies() {
        return availableCopies;
    }

    public void setAvailableCopies(int availableCopies) {
        this.availableCopies = Math.max(
                0,
                Math.min(availableCopies, totalCopies)
        );
    }

    public String getPublisher() {
        return publisher;
    }

    public void setPublisher(String publisher) {
        this.publisher = publisher == null ? "" : publisher;
    }

    public int getPublicationYear() {
        return publicationYear;
    }

    public void setPublicationYear(int publicationYear) {
        this.publicationYear = publicationYear;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description == null ? "" : description;
    }

    // =========================
    // Copy Management
    // =========================

    public void increaseAvailableCopies() {
        if (availableCopies < totalCopies) {
            availableCopies++;
        }
    }

    public void decreaseAvailableCopies() {
        if (availableCopies > 0) {
            availableCopies--;
        }
    }

    public boolean hasAvailableCopy() {
        return availableCopies > 0;
    }

    // =========================
    // Object methods
    // =========================

    @Override
    public String toString() {
        return title + " - " + author;
    }
}