package qasystem.service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

/**
 * A test double (a "fake"): a Clock that only moves when the test says so.
 * Lets tests check deadlines and sort order without waiting in real time.
 */
class MutableClock extends Clock {

    private Instant instant;
    private final ZoneId zone;

    MutableClock(LocalDateTime start) {
        this.zone = ZoneId.of("UTC");
        this.instant = start.atZone(zone).toInstant();
    }

    private MutableClock(Instant instant, ZoneId zone) {
        this.instant = instant;
        this.zone = zone;
    }

    /** Move time forward. */
    void advance(Duration duration) {
        instant = instant.plus(duration);
    }

    @Override
    public ZoneId getZone() {
        return zone;
    }

    @Override
    public Clock withZone(ZoneId zone) {
        return new MutableClock(instant, zone);
    }

    @Override
    public Instant instant() {
        return instant;
    }
}
