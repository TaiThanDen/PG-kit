package com.pgkit.timemarkcamera;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class PhotoSaveMetadataTest {
    @Test public void qualityUsesLongestDimensionAtTheObservedBoundaries() {
        assertEquals(90, PhotoSaveMetadata.jpegQuality(1200, 1600));
        assertEquals(99, PhotoSaveMetadata.jpegQuality(1601, 1200));
        assertEquals(99, PhotoSaveMetadata.jpegQuality(3000, 3999));
        assertEquals(95, PhotoSaveMetadata.jpegQuality(4000, 3000));
        assertEquals(95, PhotoSaveMetadata.jpegQuality(3000, 4000));
    }
}
