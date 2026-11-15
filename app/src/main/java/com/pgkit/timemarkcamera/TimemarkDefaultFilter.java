package com.pgkit.timemarkcamera;

import android.hardware.camera2.CaptureRequest;
import android.annotation.SuppressLint;

import androidx.camera.camera2.interop.Camera2Interop;
import androidx.camera.core.ExtendableBuilder;

/**
 * Timemark's packaged rear-camera Original profile (v10.0.311 and v10.0.316).
 * The reference renderer forces LUT intensity to zero for ID -10000, and binds
 * tone intensity to zero. Original therefore preserves camera colors; it is not
 * an identity LUT that should be sampled again after the camera has rendered.
 * See docs/timemark_reference/default-filter for the REA and runtime evidence.
 */
final class TimemarkDefaultFilter {
    private TimemarkDefaultFilter() {}

    /** Applies the neutral profile to both repeating preview and still requests. */
    @SuppressLint("UnsafeOptInUsageError")
    static <T> void apply(ExtendableBuilder<T> builder) {
        // The original camera's public capture results also report EFFECT_MODE_OFF.
        // Keep hardware color effects off; do not tint the preview or resample JPEG
        // colors through res_original.png when the reference shader bypasses it.
        new Camera2Interop.Extender<>(builder).setCaptureRequestOption(
                CaptureRequest.CONTROL_EFFECT_MODE, CaptureRequest.CONTROL_EFFECT_MODE_OFF);
    }
}
