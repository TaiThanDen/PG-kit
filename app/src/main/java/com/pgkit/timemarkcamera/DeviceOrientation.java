package com.pgkit.timemarkcamera;

import android.view.Surface;

/**
 * Converts the physical device orientation into CameraX/Surface rotations.
 *
 * <p>The activity stays portrait-only. These values are for the captured image and
 * watermark, not for rebuilding or rotating the application UI.</p>
 */
final class DeviceOrientation {
    private DeviceOrientation() {
    }

    static int fromSensorDegrees(int orientation) {
        // OrientationEventListener degrees run opposite to Surface rotation constants.
        if (orientation >= 45 && orientation < 135) return Surface.ROTATION_270;
        if (orientation >= 135 && orientation < 225) return Surface.ROTATION_180;
        if (orientation >= 225 && orientation < 315) return Surface.ROTATION_90;
        return Surface.ROTATION_0;
    }

    static float watermarkDegrees(int rotation) {
        switch (rotation) {
            case Surface.ROTATION_90: return -90f;
            case Surface.ROTATION_180: return 180f;
            case Surface.ROTATION_270: return 90f;
            default: return 0f;
        }
    }

    static boolean isLandscape(int rotation) {
        return rotation == Surface.ROTATION_90 || rotation == Surface.ROTATION_270;
    }
}
