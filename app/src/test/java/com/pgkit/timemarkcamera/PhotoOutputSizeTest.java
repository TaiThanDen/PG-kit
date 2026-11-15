package com.pgkit.timemarkcamera;

import static org.junit.Assert.assertArrayEquals;
import org.junit.Test;

public final class PhotoOutputSizeTest {
    @Test public void referencePresetPreservesBothFourByThreeOrientations() {
        assertArrayEquals(new int[]{1536, 2048}, PhotoOutputSize.fit(3024, 4032, 2048));
        assertArrayEquals(new int[]{2048, 1536}, PhotoOutputSize.fit(4032, 3024, 2048));
    }

    @Test public void preservesOtherAspectRatiosAndDoesNotUpscale() {
        assertArrayEquals(new int[]{2048, 1152}, PhotoOutputSize.fit(3840, 2160, 2048));
        assertArrayEquals(new int[]{2048, 2048}, PhotoOutputSize.fit(3000, 3000, 2048));
        assertArrayEquals(new int[]{640, 480}, PhotoOutputSize.fit(640, 480, 2048));
    }

    @Test public void originalPresetKeepsSensorResolution() {
        assertArrayEquals(new int[]{3024, 4032}, PhotoOutputSize.fit(3024, 4032, 0));
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsInvalidDimensions() { PhotoOutputSize.fit(0, 4032, 2048); }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsUnknownPreset() { PhotoOutputSize.fit(3024, 4032, -1); }
}
