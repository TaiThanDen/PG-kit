package com.pgkit.timemarkcamera;

/** Pixel-coordinate constraints for the exported 4:3 Timemark watermark. */
final class WatermarkLayoutSpec {
    static final float LANDSCAPE_WIDTH = 1536f;
    static final float LANDSCAPE_HEIGHT = 1152f;
    static final float PORTRAIT_WIDTH = 1152f;
    static final float PORTRAIT_HEIGHT = 1536f;

    static final float TIME_LEFT = 31f;
    static final float DIVIDER_GAP = 32f;
    static final float DIVIDER_WIDTH = 7f;
    static final float DIVIDER_HEIGHT = 117f;
    static final float DIVIDER_BOTTOM = 165f;
    static final float DATE_GAP = 21f;
    static final float WEEKDAY_GAP = 19f;
    static final float WEEKDAY_TOP_GAP = 86f;
    static final float ADDRESS_LEFT = 36f;
    static final float ADDRESS_BOTTOM = 37f;
    static final float ADDRESS_LINE_STEP = 51f;
    static final float ADDRESS_MAX_WIDTH = 800f;
    static final float WORDMARK_RIGHT = 18f;
    static final float WORDMARK_BOTTOM = 56f;
    static final float SUBTITLE_RIGHT = 18f;
    static final float SUBTITLE_BOTTOM = 20f;
    static final float VERIFICATION_RIGHT = 11f;

    private WatermarkLayoutSpec() {
    }

    static float scaleFor(int width, int height) {
        boolean landscape = width > height;
        float referenceWidth = landscape ? LANDSCAPE_WIDTH : PORTRAIT_WIDTH;
        float referenceHeight = landscape ? LANDSCAPE_HEIGHT : PORTRAIT_HEIGHT;
        return Math.min(width / referenceWidth, height / referenceHeight);
    }

    static float dividerTop(int imageHeight, float scale) {
        return imageHeight - (DIVIDER_BOTTOM + DIVIDER_HEIGHT) * scale;
    }

    static float dividerX(float measuredTimeWidth, float scale) {
        return TIME_LEFT * scale + measuredTimeWidth + DIVIDER_GAP * scale;
    }

    static float rightAnchoredLeft(int imageWidth, float right, float visibleWidth, float scale) {
        return imageWidth - (right + visibleWidth) * scale;
    }

    static float centerAnchoredTop(int imageHeight, float visibleHeight, float scale) {
        return (imageHeight - visibleHeight * scale) / 2f;
    }
}
