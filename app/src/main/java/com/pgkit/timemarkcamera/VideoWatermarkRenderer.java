package com.pgkit.timemarkcamera;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.RectF;

import java.util.Date;
import java.io.IOException;
import java.io.InputStream;

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
    private final Bitmap brandWordmark;
    private final RectF brandWordmarkDestination = new RectF();
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
        // Vietnamese official-logo response contains both the wordmark and subtitle.
        // Keep its native transparent padding and shadows; do not reconstruct glyphs.
        try (InputStream source = context.getAssets().open("video/timecounter_brand.png")) {
            brandWordmark = BitmapFactory.decodeStream(source);
        } catch (IOException error) {
            throw new IllegalStateException("Video wordmark asset missing", error);
        }
        if (brandWordmark == null) throw new IllegalStateException("Video wordmark asset invalid");
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
            drawBrand(bitmapCanvas, overlay.getWidth(), overlay.getHeight());
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

    private void drawBrand(Canvas canvas, int width, int height) {
        // Full 300px source fits a 187px square at the measured 720 short edge.
        // Covered-camera .316 fit; .311/.316 scene references remain holdouts.
        float size = 187f * layoutShortEdge / 720f;
        brandWordmarkDestination.set(width - size, height - size, width, height);
        canvas.drawBitmap(brandWordmark, null, brandWordmarkDestination, bitmapPaint);
    }
}
