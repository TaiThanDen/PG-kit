package com.pgkit.timemarkcamera;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.RectF;

import java.util.Date;

/**
 * A recording owns one immutable template/location snapshot. Its clock remains live.
 * Timemark's RecordHelper snapshots the live view at start, refreshes its bitmap once
 * per second, and composites that texture into each video frame. JPEG coordinates
 * are a separate path; see docs/timemark_reference/video/production-pipeline.md.
 */
final class VideoWatermarkRenderer {
    private final String address;
    private final WatermarkTemplate.Snapshot template;
    private final int layoutShortEdge;
    private final float density;
    private final Paint bitmapPaint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
    private final RectF destination = new RectF();
    private Bitmap overlay;
    private long renderedSecond = Long.MIN_VALUE;
    private int renderedWidth;
    private int renderedHeight;

    VideoWatermarkRenderer(Context context, String address, WatermarkTemplate.Snapshot template) {
        this.address = address;
        this.template = template;
        // This is the native layout's rasterization density, never JPEG/PreviewView geometry.
        layoutShortEdge = Math.max(1, context.getResources().getDisplayMetrics().widthPixels);
        density = context.getResources().getDisplayMetrics().density;
    }

    /** Called only on CameraEngine's effect thread, in upright output coordinates. */
    void draw(Canvas canvas, int width, int height, long deviceTimeMillis) {
        if (width <= 0 || height <= 0 || (template != null && "none".equals(template.id))) return;
        long second = deviceTimeMillis / 1000L;
        if (overlay == null || renderedWidth != width || renderedHeight != height) {
            release();
            float scale = layoutShortEdge / (float) Math.min(width, height);
            overlay = Bitmap.createBitmap(Math.round(width * scale), Math.round(height * scale),
                    Bitmap.Config.ARGB_8888);
            renderedWidth = width;
            renderedHeight = height;
        }
        if (renderedSecond != second) {
            Canvas bitmapCanvas = new Canvas(overlay);
            bitmapCanvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);
            WatermarkRenderer.drawLive(bitmapCanvas, layoutShortEdge, overlay.getHeight(),
                    new Date(deviceTimeMillis), address, density, template);
            renderedSecond = second;
        }
        destination.set(0, 0, width, height);
        canvas.drawBitmap(overlay, null, destination, bitmapPaint);
    }

    void release() {
        if (overlay != null) {
            overlay.recycle();
            overlay = null;
        }
        renderedSecond = Long.MIN_VALUE;
    }

}
