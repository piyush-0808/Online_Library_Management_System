package library.management.system.util;

import java.time.Year;

public final class MemberIdGenerator {

    private static int counter = 0;

    private MemberIdGenerator() {
        // Utility class
    }

    /**
     * Generates a member ID in the format:
     * LIB + YEAR + 4 digit number
     *
     * Example:
     * LIB20260001
     */
    public static synchronized String nextId() {

        counter++;

        return String.format(
                "LIB%d%04d",
                Year.now().getValue(),
                counter
        );
    }
}