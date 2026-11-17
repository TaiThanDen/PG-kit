package com.pgkit.timemarkcamera;

import org.junit.Test;
import static org.junit.Assert.*;

public class WatermarkClockTest {
    @Test public void defaultFollowsDeviceTime() {
        WatermarkClock clock = new WatermarkClock();
        assertEquals(123456789L, clock.millis(123456789L));
        assertEquals(123457789L, clock.millis(123457789L));
        assertFalse(clock.isCustom());
    }

    @Test public void selectedTimeAdvancesAcrossMidnight() {
        WatermarkClock clock = new WatermarkClock();
        long lastMinute = 86_340_000L;
        clock.set(lastMinute, 1_000L, true);
        assertEquals(86_400_000L, clock.millis(61_000L));
    }

    @Test public void frozenTimeSurvivesRestartAndDeviceTimeChanges() {
        WatermarkClock clock = new WatermarkClock();
        clock.set(5_000L, 1_000L, false);
        WatermarkClock restored = new WatermarkClock();
        restored.restore(clock.isCustom(), clock.isRunning(), clock.storedValue());
        assertEquals(5_000L, restored.millis(1_000_000L));
        assertFalse(restored.isRunning());
    }

    @Test public void runningOffsetSurvivesRestart() {
        WatermarkClock clock = new WatermarkClock();
        clock.set(5_000L, 1_000L, true);
        WatermarkClock restored = new WatermarkClock();
        restored.restore(clock.isCustom(), clock.isRunning(), clock.storedValue());
        assertEquals(65_000L, restored.millis(61_000L));
    }
}
