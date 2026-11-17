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

    /** A period is valid if start is in the future (within the limit), end is after start and not too long. */
    public static boolean isValidPeriod(LocalDateTime start, LocalDateTime end, LocalDateTime now,
                                        int maxDaysAhead, Duration maxLength) {
        return isValidBookingTime(start, now, maxDaysAhead)
                && end != null
                && end.isAfter(start)
                && !Duration.between(start, end).minus(maxLength).isPositive();
    }
}
