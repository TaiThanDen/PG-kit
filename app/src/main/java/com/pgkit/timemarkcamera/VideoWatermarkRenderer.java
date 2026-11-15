package com.pgkit.timemarkcamera;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Typeface;

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
    private final Typeface brandFont;
    private final Typeface subtitleFont;
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
        brandFont = Typeface.createFromAsset(context.getAssets(), "fonts/RobotoCondensed-Medium.ttf");
        subtitleFont = Typeface.createFromAsset(context.getAssets(), "fonts/RobotoCondensed-Regular.ttf");
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
        // Measured against the original default-34 MP4. Other brand variants remain unverified.
        float scale = layoutShortEdge / 1152f;
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.SUBPIXEL_TEXT_FLAG);
        paint.setTypeface(brandFont);
        paint.setTextSize(40f * scale);
        paint.setShadowLayer(3.5f * scale, 0, 1.5f * scale, 0x59000000);
        Rect bounds = new Rect();
        paint.getTextBounds("Timemark", 0, 8, bounds);
        paint.setTextScaleX(177f * scale / Math.max(1, bounds.width()));
        paint.getTextBounds("Timemark", 0, 8, bounds);
        float left = width - 18f * scale - bounds.right;
        float baseline = height - 56f * scale - bounds.bottom;
        paint.setColor(0xfff8c23e);
        canvas.drawText("Time", left, baseline, paint);
        float timeWidth = paint.measureText("Time");
        paint.setColor(Color.WHITE);
        canvas.drawText("mark", left + timeWidth, baseline, paint);

        String subtitle = "100% Chân thực";
        paint.setTypeface(subtitleFont);
        paint.setTextScaleX(1f);
        paint.setTextSize(32f * scale);
        paint.setColor(0xffbebebe);
        paint.getTextBounds(subtitle, 0, subtitle.length(), bounds);
        paint.setTextScaleX(174f * scale / Math.max(1, bounds.width()));
        paint.getTextBounds(subtitle, 0, subtitle.length(), bounds);
        canvas.drawText(subtitle, width - 18f * scale - bounds.right,
                height - 20f * scale - bounds.bottom, paint);
    }
}
