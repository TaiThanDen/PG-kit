package com.pgkit.timemarkcamera;

/** User-selected watermark time. Device wall time remains the default and elapsed source. */
final class WatermarkClock {
    private boolean custom;
    private boolean running = true;
    private long value;

    synchronized long millis(long deviceMillis) {
        return custom ? (running ? deviceMillis + value : value) : deviceMillis;
    }

    long now() { return millis(System.currentTimeMillis()); }

    synchronized void set(long selectedMillis, long deviceMillis, boolean keepRunning) {
        custom = true;
        running = keepRunning;
        value = keepRunning ? selectedMillis - deviceMillis : selectedMillis;
    }

    synchronized void reset() {
        custom = false;
        running = true;
        value = 0;
    }

    synchronized void restore(boolean custom, boolean running, long value) {
        this.custom = custom;
        this.running = running;
        this.value = value;
    }

    synchronized boolean isCustom() { return custom; }
    synchronized boolean isRunning() { return running; }
    synchronized long storedValue() { return value; }
}
