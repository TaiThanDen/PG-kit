package com.pgkit.timemarkcamera;

import java.util.Locale;

/** Formats CameraX's recorded duration, independent of the editable watermark clock. */
final class RecordingTime {
    static String format(long nanos) {
        long seconds = Math.max(0, nanos / 1_000_000_000L);
        return seconds >= 3600
                ? String.format(Locale.US, "%02d:%02d:%02d", seconds/3600, (seconds/60)%60, seconds%60)
                : String.format(Locale.US, "%02d:%02d", seconds/60, seconds%60);
    }
}
