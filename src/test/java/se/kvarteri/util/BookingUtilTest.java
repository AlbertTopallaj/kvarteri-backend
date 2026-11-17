package se.kvarteri.util;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class BookingUtilTest {

    private final LocalDateTime now = LocalDateTime.of(2026, 1, 1, 12, 0);

    @Test
    void acceptsFutureTimeWithinLimit() {
        assertTrue(BookingUtil.isValidBookingTime(now.plusDays(3), now, 30));
    }

    @Test
    void rejectsPastNullAndTooFar() {
        assertFalse(BookingUtil.isValidBookingTime(now.minusHours(1), now, 30));
        assertFalse(BookingUtil.isValidBookingTime(null, now, 30));
        assertFalse(BookingUtil.isValidBookingTime(now.plusDays(31), now, 30));
    }
}
