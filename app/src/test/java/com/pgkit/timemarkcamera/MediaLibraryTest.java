package com.pgkit.timemarkcamera;

import org.junit.Test;
import static org.junit.Assert.*;

public class MediaLibraryTest {
    @Test public void smallPhotoKeepsOriginalPixels() {
        assertEquals(1, MediaLibrary.sampleSize(1536, 2048, 2560));
    }
    @Test public void largeSensorPhotoIsDownsampledBeforeAllocation() {
        assertEquals(4, MediaLibrary.sampleSize(12000, 9000, 1600));
        assertEquals(4, MediaLibrary.sampleSize(9000, 12000, 1600));
    }
    @Test public void gridDoesNotDecodeFullResolutionPhoto() {
        assertEquals(8, MediaLibrary.sampleSize(4032, 3024, 256));
    }
    @Test(expected = IllegalArgumentException.class) public void rejectsInvalidBounds() {
        MediaLibrary.sampleSize(0, 100, 256);
    }
}
