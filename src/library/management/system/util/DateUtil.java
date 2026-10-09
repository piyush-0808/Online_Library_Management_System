package library.management.system.util;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

public final class DateUtil {

    private DateUtil() {
        // Utility class
    }

    public static LocalDate today() {
        return LocalDate.now();
    }

    public static LocalDate addDays(LocalDate date, int days) {
        return date.plusDays(days);
    }

    public static long daysBetween(LocalDate startDate, LocalDate endDate) {
        if (startDate == null || endDate == null) {
            return 0;
        }

        return ChronoUnit.DAYS.between(startDate, endDate);
    }

    public static long overdueDays(LocalDate dueDate) {
        if (dueDate == null) {
            return 0;
        }

        long days = ChronoUnit.DAYS.between(dueDate, today());
        return Math.max(days, 0);
    }

    public static long overdueDays(LocalDate dueDate, LocalDate returnDate) {
        if (dueDate == null || returnDate == null) {
            return 0;
        }

        long days = ChronoUnit.DAYS.between(dueDate, returnDate);
        return Math.max(days, 0);
    }

    public static boolean isOverdue(LocalDate dueDate) {
        return dueDate != null && today().isAfter(dueDate);
    }

    public static boolean isBeforeToday(LocalDate date) {
        return date != null && date.isBefore(today());
    }

    public static boolean isToday(LocalDate date) {
        return date != null && date.equals(today());
    }

    public static boolean isTodayOrFuture(LocalDate date) {
        return date != null && !date.isBefore(today());
    }

    public static String format(LocalDate date) {
        return date == null ? "" : date.toString();
    }

    public static LocalDate parse(String date) {
        if (!Validator.isNotBlank(date)) {
            return null;
        }

        return LocalDate.parse(date.trim());
    }
}