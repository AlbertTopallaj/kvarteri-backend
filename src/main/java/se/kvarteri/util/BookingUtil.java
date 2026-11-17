package se.kvarteri.util;

import java.time.Duration;
import java.time.LocalDateTime;

public final class BookingUtil {

    private BookingUtil() {
    }

    /** Booking times must be in the future and at most {@code maxDaysAhead} days ahead. */
    public static boolean isValidBookingTime(LocalDateTime time, LocalDateTime now, int maxDaysAhead) {
        return time != null
                && time.isAfter(now)
                && Duration.between(now, time).toDays() <= maxDaysAhead;
    }
}
