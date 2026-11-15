package com.pgkit.timemarkcamera;

/** Output presets; 2048 matches the selected setting observed on the reference LG. */
final class PhotoOutputSize {
    static final int REFERENCE_LONG_EDGE = 2048;
    static final int CAMERA_ORIGINAL = 0;
    static final String PREFERENCES = "photo_output";
    static final String LONG_EDGE_KEY = "long_edge";

    private PhotoOutputSize() {}

    /** Preserves aspect ratio and never enlarges a smaller source image. */
    static int[] fit(int width, int height, int longEdge) {
        if (width <= 0 || height <= 0) throw new IllegalArgumentException("Invalid image size");
        if (longEdge != CAMERA_ORIGINAL && longEdge != REFERENCE_LONG_EDGE)
            throw new IllegalArgumentException("Unknown photo output preset");
        int sourceLongEdge = Math.max(width, height);
        if (longEdge == CAMERA_ORIGINAL || sourceLongEdge <= longEdge)
            return new int[]{width, height};
        double scale = longEdge / (double) sourceLongEdge;
        return new int[]{Math.max(1, (int) Math.round(width * scale)),
                Math.max(1, (int) Math.round(height * scale))};
    }
}
