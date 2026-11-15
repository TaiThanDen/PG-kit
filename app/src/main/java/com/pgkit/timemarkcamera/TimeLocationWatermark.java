package com.pgkit.timemarkcamera;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.text.Layout;
import android.text.StaticLayout;
import android.text.TextDirectionHeuristics;
import android.text.TextPaint;

/** Native text layout for the camera's template 34, reconstructed from the 1034 binding.
 * Keep device dp rounding before the original 375 dp width normalization (x50.d.D).
 * JPEG export reuses the native layout with its own output scale and address-column width.
 */
final class TimeLocationWatermark {
    private static final ThreadLocal<Entry> CACHE = new ThreadLocal<>();

    private TimeLocationWatermark() {}

    static void draw(Canvas canvas, int width, int height, float density, String time,
                     String date, String weekday, String address, Typeface clockFont,
                     Typeface textFont) {
        drawStyled(canvas, width, height, density, time, date, weekday, address, clockFont,
                textFont, 1f, Color.WHITE, WatermarkRenderer.YELLOW, true);
    }

    static void drawStyled(Canvas canvas, int width, int height, float density, String time,
                           String date, String weekday, String address, Typeface clockFont,
                           Typeface textFont, float scale, int foreground, int accent,
                           boolean showLocation) {
        if (width <= 0 || height <= 0 || density <= 0) return;
        Entry entry = CACHE.get();
        if (entry == null || !entry.matches(width, density, time, date, weekday, address,
                clockFont, textFont)) {
            entry = new Entry(width, density, time, date, weekday, address, clockFont, textFont);
            CACHE.set(entry);
        }
        int save = canvas.save();
        canvas.translate(entry.margin * (1 - scale), (height - entry.margin) * (1 - scale));
        canvas.scale(scale, scale);
        entry.draw(canvas, height, foreground, accent, showLocation);
        canvas.restoreToCount(save);
    }

    /** Rasterizes the native layout at JPEG scale, independently of the preview crop. */
    static void drawExport(Canvas canvas, int width, int height, int layoutWidth, float density,
                           String time, String date, String weekday, String address,
                           Typeface clockFont, Typeface textFont) {
        drawExportStyled(canvas, width, height, layoutWidth, density, time, date, weekday,
                address, clockFont, textFont, 1f, Color.WHITE, WatermarkRenderer.YELLOW, true);
    }

    static void drawExportStyled(Canvas canvas, int width, int height, int layoutWidth, float density,
                                 String time, String date, String weekday, String address,
                                 Typeface clockFont, Typeface textFont, float sizeScale,
                                 int foreground, int accent, boolean showLocation) {
        if (width <= 0 || height <= 0 || layoutWidth <= 0 || density <= 0) return;
        float scale = Math.min(width, height) / (float) layoutWidth;
        int layoutHeight = Math.round(height / scale);
        Entry entry = new Entry(layoutWidth, density, time, date, weekday, address,
                clockFont, textFont, false);
        int save = canvas.save();
        // The reference's portrait export starts one pixel earlier at a 1536 px short side.
        // Keep this JPEG raster calibration separate from the native preview layout.
        canvas.translate(Math.min(width, height) / (width > height ? 768f : 1536f), 0f);
        canvas.scale(scale, scale);
        canvas.translate(entry.margin * (1 - sizeScale),
                (layoutHeight - entry.margin) * (1 - sizeScale));
        canvas.scale(sizeScale, sizeScale);
        entry.draw(canvas, layoutHeight, foreground, accent, showLocation);
        canvas.restoreToCount(save);
    }

    private static int dp(float value, float density) {
        return Math.round(value * density);
    }

    private static TextPaint textPaint(float size, Typeface font, boolean shadow) {
        // TextView's native flags, including subpixel text positioning.
        TextPaint paint = new TextPaint(0x503);
        paint.setTypeface(font);
        paint.setTextSize(size);
        paint.setColor(Color.WHITE);
        // These XML attributes are floats in pixels, not dimensions in dp.
        if (shadow) paint.setShadowLayer(3.5f, 0, 1.5f, 0x66000000);
        return paint;
    }

    private static StaticLayout layout(String value, TextPaint paint, int width,
                                       boolean includePadding, boolean multiline) {
        return StaticLayout.Builder.obtain(value, 0, value.length(), paint, Math.max(1, width))
                .setAlignment(Layout.Alignment.ALIGN_NORMAL)
                .setTextDirection(TextDirectionHeuristics.FIRSTSTRONG_LTR)
                .setIncludePad(includePadding)
                .setBreakStrategy(multiline ? Layout.BREAK_STRATEGY_HIGH_QUALITY
                        : Layout.BREAK_STRATEGY_SIMPLE)
                .setHyphenationFrequency(Layout.HYPHENATION_FREQUENCY_NORMAL)
                .build();
    }

    private static StaticLayout single(String value, TextPaint paint, boolean includePadding) {
        return layout(value, paint, (int) Math.ceil(Layout.getDesiredWidth(value, paint)),
                includePadding, false);
    }

    private static void drawLayout(Canvas canvas, StaticLayout layout, int left, int top) {
        int save = canvas.save();
        canvas.translate(left, top);
        layout.draw(canvas);
        canvas.restoreToCount(save);
    }

    private static final class Entry {
        final int width;
        final float density;
        final String time, date, weekday, address;
        final Typeface clockFont, textFont;
        final StaticLayout clock, dateLine, weekdayLine, location;
        final int margin, headerHeight, dividerX, dividerWidth, dividerHeight, detailX;
        final int detailInset, locationInset;

        Entry(int width, float density, String time, String date, String weekday, String address,
              Typeface clockFont, Typeface textFont) {
            this(width, density, time, date, weekday, address, clockFont, textFont, true);
        }

        Entry(int width, float density, String time, String date, String weekday, String address,
              Typeface clockFont, Typeface textFont, boolean scaleLocationColumn) {
            this.width = width;
            this.density = density;
            this.time = time;
            this.date = date;
            this.weekday = weekday;
            this.address = address == null ? "" : address;
            this.clockFont = clockFont;
            this.textFont = textFont;
            float totalScale = width / density / 375f;
            margin = dp(8, density);
            clock = single(time, textPaint(dp(45, density) * totalScale, clockFont, false), false);
            dateLine = single(date, textPaint(dp(14, density) * totalScale, textFont, true), true);
            weekdayLine = single(weekday, textPaint(dp(14, density) * totalScale, textFont, true), true);
            detailInset = dp(4, density);
            headerHeight = Math.max(clock.getHeight(), dateLine.getHeight()
                    + weekdayLine.getHeight() + 2 * detailInset);
            // The empty seconds container retains its 4 dp start margin. The divider has a
            // relative start margin of 6 dp; its binding's absolute left margin does not replace it.
            dividerX = margin + clock.getWidth() + dp(4, density) + dp(6, density);
            dividerWidth = dp(2, density);
            dividerHeight = (int) (dp(38, density) * totalScale);
            detailX = dividerX + dividerWidth + dp(6, density);
            locationInset = dp(3, density);
            int columnWidth = Math.min(width - 2 * margin,
                    scaleLocationColumn ? (int) (dp(245, density) * totalScale)
                            : dp(245, density) + dp(6, density));
            location = layout(this.address, textPaint(dp(15, density) * totalScale, textFont, true),
                    columnWidth - locationInset, true, scaleLocationColumn);
        }

        boolean matches(int width, float density, String time, String date, String weekday,
                        String address, Typeface clockFont, Typeface textFont) {
            return this.width == width && this.density == density && this.time.equals(time)
                    && this.date.equals(date) && this.weekday.equals(weekday)
                    && this.address.equals(address == null ? "" : address)
                    && this.clockFont == clockFont && this.textFont == textFont;
        }

        void draw(Canvas canvas, int height, int foreground, int accent, boolean showLocation) {
            boolean locationVisible = showLocation && !address.trim().isEmpty();
            int top = height - margin - headerHeight - (locationVisible ? location.getHeight() : 0);
            TextPaint clockPaint = clock.getPaint();
            clockPaint.setStyle(Paint.Style.STROKE);
            clockPaint.setStrokeWidth(dp(.5f, density));
            clockPaint.setColor(0x80000000);
            drawLayout(canvas, clock, margin, top);
            clockPaint.setStyle(Paint.Style.FILL);
            clockPaint.setColor(foreground);
            drawLayout(canvas, clock, margin, top);
            Paint divider = new Paint();
            divider.setColor(accent);
            int dividerTop = top + Math.round((headerHeight - dividerHeight) / 2f);
            canvas.drawRect(dividerX, dividerTop, dividerX + dividerWidth,
                    dividerTop + dividerHeight, divider);
            dateLine.getPaint().setColor(foreground);
            weekdayLine.getPaint().setColor(foreground);
            location.getPaint().setColor(foreground);
            drawLayout(canvas, dateLine, detailX, top + detailInset);
            drawLayout(canvas, weekdayLine, detailX,
                    top + headerHeight - detailInset - weekdayLine.getHeight());
            if (locationVisible) drawLayout(canvas, location, margin + locationInset, top + headerHeight);
        }
    }
}
