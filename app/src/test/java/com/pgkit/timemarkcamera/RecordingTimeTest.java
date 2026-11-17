package com.pgkit.timemarkcamera;

import org.junit.Test;
import static org.junit.Assert.assertEquals;

public class RecordingTimeTest {
    @Test public void zeroAndSubsecondAreZero() {
        assertEquals("00:00", RecordingTime.format(0));
        assertEquals("00:00", RecordingTime.format(999_999_999L));
        assertEquals("00:00", RecordingTime.format(-1));
    }
    @Test public void rollsIntoMinutesAndHours() {
        assertEquals("00:59", RecordingTime.format(59_000_000_000L));
        assertEquals("01:00", RecordingTime.format(60_000_000_000L));
        assertEquals("01:01:01", RecordingTime.format(3661_000_000_000L));
    }
}
