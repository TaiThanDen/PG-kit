package com.pgkit.timemarkcamera;

/** Portrait ratios observed in Timemark's capture-ratio control on the reference LG. */
enum PhotoAspectRatio {
    STANDARD("3:4", 3, 4), SQUARE("1:1", 1, 1), TALL("9:16", 9, 16);
    final String label;
    final int width;
    final int height;
    PhotoAspectRatio(String label, int width, int height) {
        this.label = label;
        this.width = width;
        this.height = height;
    }
    static PhotoAspectRatio read(String name) {
        for (PhotoAspectRatio ratio : values()) if (ratio.name().equals(name)) return ratio;
        return STANDARD;
    }
}
