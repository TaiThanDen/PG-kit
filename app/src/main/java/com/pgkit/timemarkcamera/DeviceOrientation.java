package com.pgkit.timemarkcamera;

import android.graphics.Matrix;
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

    /**
     * Converts sensor degrees while retaining the current rotation until the device has moved
     * roughly 60 degrees away from that orientation. This avoids rapid changes around the
     * 45-degree boundaries used by {@link #fromSensorDegrees(int)}.
     */
    static int fromSensorDegrees(int orientation, int currentRotation) {
        if (orientation < 0) {
            return isSurfaceRotation(currentRotation) ? currentRotation : Surface.ROTATION_0;
        }

        int stableRotation = isSurfaceRotation(currentRotation)
                ? currentRotation
                : fromSensorDegrees(orientation);
        int stableSensorDegrees = sensorDegreesForRotation(stableRotation);
        if (angularDistance(orientation, stableSensorDegrees) <= 60) {
            return stableRotation;
        }
        return fromSensorDegrees(orientation);
    }

    static float watermarkDegrees(int rotation) {
        switch (rotation) {
            case Surface.ROTATION_90: return 90f;
            case Surface.ROTATION_180: return 180f;
            case Surface.ROTATION_270: return -90f;
            default: return 0f;
        }
    }

    /**
     * Builds the complete transform for a portrait-sized watermark canvas.
     *
     * <p>The base watermark is always rendered at the bottom-left of {@code viewWidth x
     * viewHeight}. The transform maps it to the reference anchors without rotating the Activity:
     * 90 -> top-left, 180 -> top-right, 270 -> bottom-right.</p>
     */
    static void setWatermarkTransform(
            Matrix matrix, int rotation, float viewWidth, float viewHeight) {
        matrix.reset();
        matrix.setRotate(watermarkDegrees(rotation));
        switch (rotation) {
            case Surface.ROTATION_90:
                matrix.postTranslate(viewHeight, 0f);
                break;
            case Surface.ROTATION_180:
                matrix.postTranslate(viewWidth, viewHeight);
                break;
            case Surface.ROTATION_270:
                matrix.postTranslate(viewWidth - viewHeight, viewHeight);
                break;
            default:
                break;
        }
    }

    static boolean isLandscape(int rotation) {
        return rotation == Surface.ROTATION_90 || rotation == Surface.ROTATION_270;
    }

    private static boolean isSurfaceRotation(int rotation) {
        return rotation == Surface.ROTATION_0
                || rotation == Surface.ROTATION_90
                || rotation == Surface.ROTATION_180
                || rotation == Surface.ROTATION_270;
    }

    private static int sensorDegreesForRotation(int rotation) {
        switch (rotation) {
            case Surface.ROTATION_270: return 90;
            case Surface.ROTATION_180: return 180;
            case Surface.ROTATION_90: return 270;
            default: return 0;
        }
    }

    private static int angularDistance(int first, int second) {
        int difference = Math.abs(first - second) % 360;
        return Math.min(difference, 360 - difference);
    }
}
