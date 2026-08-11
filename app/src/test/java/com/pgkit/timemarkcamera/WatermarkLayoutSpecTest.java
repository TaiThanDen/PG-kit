package com.pgkit.timemarkcamera;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public final class WatermarkLayoutSpecTest {
    private static final float DELTA = 0.0001f;

    @Test public void referenceCanvasesUseOneToOneScale() {
        assertEquals(1f, WatermarkLayoutSpec.scaleFor(1536, 1152), DELTA);
        assertEquals(1f, WatermarkLayoutSpec.scaleFor(1152, 1536), DELTA);
    }

    @Test public void dividerTracksTheMeasuredTimeWidth() {
        assertEquals(252f, WatermarkLayoutSpec.dividerX(189f, 1f), DELTA);
        assertEquals(235f, WatermarkLayoutSpec.dividerX(172f, 1f), DELTA);
        assertEquals(286f, WatermarkLayoutSpec.dividerX(223f, 1f), DELTA);
    }

    @Test public void dateAndWeekdayShareTheSameLeftAnchor() {
        float dividerX = 252f;
        float dateX = dividerX + WatermarkLayoutSpec.DIVIDER_WIDTH
                + WatermarkLayoutSpec.DATE_GAP;
        float weekdayX = dividerX + WatermarkLayoutSpec.DIVIDER_WIDTH
                + WatermarkLayoutSpec.WEEKDAY_GAP;
        assertEquals(dateX, weekdayX, DELTA);
        assertEquals(90f, WatermarkLayoutSpec.WEEKDAY_TOP_GAP, DELTA);
    }

    @Test public void bottomAndRightAnchorsMoveOnlyWithCanvasEdges() {
        assertEquals(870f, WatermarkLayoutSpec.dividerTop(1152, 1f), DELTA);
        assertEquals(1254f, WatermarkLayoutSpec.dividerTop(1536, 1f), DELTA);
        assertEquals(1341f, WatermarkLayoutSpec.rightAnchoredLeft(1536, 18f, 177f, 1f), DELTA);
        assertEquals(957f, WatermarkLayoutSpec.rightAnchoredLeft(1152, 18f, 177f, 1f), DELTA);
        assertEquals(342f, WatermarkLayoutSpec.centerAnchoredTop(1152, 468f, 1f), DELTA);
        assertEquals(534f, WatermarkLayoutSpec.centerAnchoredTop(1536, 468f, 1f), DELTA);
    }
}
