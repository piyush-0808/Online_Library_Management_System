package library.management.system.util;

import java.time.LocalDate;
import java.util.regex.Pattern;

/**
 * Utility class for validating user input.
 *
 * Validation is kept outside UI classes so that the same rules
 * can be reused by different screens.
 */
public final class Validator {

    private static final Pattern EMAIL_PATTERN =
            Pattern.compile(
                    "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$"
            );

    private static final Pattern PHONE_PATTERN =
            Pattern.compile(
                    "^[0-9]{10}$"
            );

    private static final Pattern ISBN_10_PATTERN =
            Pattern.compile(
                    "^(?:\\d{9}[\\dXx]|\\d{10})$"
            );

    private static final Pattern ISBN_13_PATTERN =
            Pattern.compile(
                    "^\\d{13}$"
            );

    private static final Pattern ISBN_INPUT_PATTERN =
            Pattern.compile("^[0-9Xx\\-\\s]+$");

    private Validator() {
        // Utility class - no objects required.
    }

    /**
     * Checks whether text is not null or blank.
     */
    public static boolean isNotBlank(String value) {

        return value != null
                && !value.trim().isEmpty();
    }

    /**
     * Validates an email address.
     */
    public static boolean isValidEmail(String email) {

        return isNotBlank(email)
                && EMAIL_PATTERN.matcher(email.trim()).matches();
    }

    /**
     * Validates exactly a 10-digit phone number.
     */
    public static boolean isValidPhone(String phone) {

        return isNotBlank(phone)
                && PHONE_PATTERN.matcher(phone.trim()).matches();
    }

    /**
     * Validates an ISBN-10 or ISBN-13.
     */
    public static boolean isValidISBN(String isbn) {

        if (!isNotBlank(isbn)) {
            return false;
        }

        if (!ISBN_INPUT_PATTERN.matcher(isbn.trim()).matches()) {
            return false;
        }
        String value = normalizeISBN(isbn);

        // This college project checks only the basic ISBN shape. It does not
        // enforce production-level checksum rules, so example ISBNs are easy
        // to enter while still preventing arbitrary text in the ISBN field.
        return ISBN_10_PATTERN.matcher(value).matches()
                || ISBN_13_PATTERN.matcher(value).matches();
    }

    /** Removes standard display separators from an ISBN. */
    public static String normalizeISBN(String isbn) {
        return isbn == null ? "" : isbn.replaceAll("[-\\s]", "").toUpperCase();
    }

    /**
     * Checks whether an integer is positive.
     */
    public static boolean isPositive(int value) {

        return value > 0;
    }

    /**
     * Checks whether a long value is positive.
     */
    public static boolean isPositive(long value) {

        return value > 0;
    }

    /**
     * Checks whether a number is zero or greater.
     */
    public static boolean isNonNegative(int value) {

        return value >= 0;
    }

    /**
     * Checks whether a number is zero or greater.
     */
    public static boolean isNonNegative(long value) {

        return value >= 0;
    }

    /**
     * Validates that a date is not null.
     */
    public static boolean isValidDate(LocalDate date) {

        return date != null;
    }

    /**
     * Validates that a date is today or in the future.
     */
    public static boolean isTodayOrFuture(LocalDate date) {

        return date != null
                && !date.isBefore(LocalDate.now());
    }

    /**
     * Validates that a date is today or in the past.
     */
    public static boolean isTodayOrPast(LocalDate date) {

        return date != null
                && !date.isAfter(LocalDate.now());
    }

    /**
     * Validates a username.
     *
     * Username must contain 3-30 characters and may contain
     * letters, digits, underscore and dot.
     */
    public static boolean isValidUsername(String username) {

        if (!isNotBlank(username)) {
            return false;
        }

        return username.trim().matches(
                "^[A-Za-z0-9_.]{3,30}$"
        );
    }

    /**
     * Validates a password length.
     *
     * Password complexity can be extended later if required.
     */
    public static boolean isValidPassword(String password) {

        return password != null
                && password.length() >= 6;
    }

    /**
     * Validates that a copy count is valid.
     */
    public static boolean isValidCopyCount(int copies) {

        return copies > 0;
    }

    /**
     * Validates that available copies are within total copies.
     */
    public static boolean isValidAvailableCopies(
            int totalCopies,
            int availableCopies) {

        return totalCopies >= 0
                && availableCopies >= 0
                && availableCopies <= totalCopies;
    }
}
